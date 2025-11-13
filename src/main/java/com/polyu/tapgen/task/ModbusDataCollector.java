package com.polyu.tapgen.task;

import com.polyu.tapgen.config.Constants;
import com.polyu.tapgen.config.DeviceGroup;
import com.polyu.tapgen.db.DeviceService;
import com.polyu.tapgen.device.*;
import com.polyu.tapgen.modbus.DeviceValue;
import com.polyu.tapgen.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ModbusDataCollector {
    @Resource
    private Map<String, DeviceGroup> deviceGroups;
    @Resource
    private DeviceService deviceService;
    @Resource
    private ExcelExportService excelExportService;
    private static final String excelPath = "C:/Users/User/.tapgen/excel";


    @Scheduled(fixedRate = 1000) // 1分钟执行一次
    public void collectAllGroupsData() {
        log.info("=== 开始采集所有设备组数据 ===");

        // 并行采集所有组
        deviceGroups.forEach((groupName, group) -> {
            CompletableFuture.runAsync(() -> collectGroupData(groupName, group));
        });
    }

    private void collectGroupData(String groupName, DeviceGroup group) {
        try {
            log.debug("开始采集组数据: {}", groupName);

            // 并行读取组内所有设备
            CompletableFuture<List<DeviceValue>> k24Future = CompletableFuture.supplyAsync(() ->
                    deviceService.dataCollection(group.getName(), Constants.Device.FLOW_METER, group.getFlowmeter())
            );
            CompletableFuture<List<DeviceValue>> bs600Future = CompletableFuture.supplyAsync(() ->
                    deviceService.dataCollection(group.getName(), Constants.Device.PRESSURE, group.getPressure())
            );
            CompletableFuture<List<DeviceValue>> sui201Future = CompletableFuture.supplyAsync(() ->
                    deviceService.dataCollection(group.getName(), Constants.Device.DC, group.getDcpower())
            );
            CompletableFuture<List<DeviceValue>> singlePhaseMeterFuture = CompletableFuture.supplyAsync(() ->
                    deviceService.dataCollection(group.getName(), Constants.Device.AC, group.getAcpower())
            );
            // 等待所有设备读取完成
            CompletableFuture.allOf(k24Future, bs600Future, sui201Future, singlePhaseMeterFuture).join();

            // 获取数据
            List<DeviceValue> allData = new ArrayList<>();
            allData.addAll(k24Future.get());
            allData.addAll(bs600Future.get());
            allData.addAll(sui201Future.get());
            allData.addAll(singlePhaseMeterFuture.get());
            // 存储到InfluxDB
            /*
            deviceService.saveK24Data(groupName, k24Data);
            deviceService.saveBS600Data(groupName, bs600Data);
            deviceService.saveSUI201Data(groupName, sui201Data);
            deviceService.saveSinglePhaseMeterData(groupName, singlePhaseMeterData);
        */
            // 写入Excel

            excelExportService.exportToExcel(allData, excelPath);


        } catch (Exception e) {
            log.error("采集组数据失败: {}", groupName, e);
        }
    }

}
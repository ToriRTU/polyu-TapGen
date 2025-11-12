package com.polyu.solarnexus.task;

import com.polyu.tapgen.config.DeviceGroup;
import com.polyu.tapgen.db.DeviceService;
import com.polyu.tapgen.device.*;
import com.polyu.tapgen.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ModbusDataCollector {

    private final Map<String, DeviceGroup> deviceGroups;
    private final K24FlowMeterService k24Service;
    private final BS600DifferentialPressureService bs600Service;
    private final SUI201PowerService sui201Service;
    private final SinglePhaseMeterService singlePhaseMeterService;
    private final DeviceService deviceService;
    private final ExcelExportService excelExportService;

    public ModbusDataCollector(Map<String, DeviceGroup> deviceGroups,
                               K24FlowMeterService k24Service,
                               BS600DifferentialPressureService bs600Service,
                               SUI201PowerService sui201Service,
                               SinglePhaseMeterService singlePhaseMeterService,
                               DeviceService deviceService,
                               ExcelExportService excelExportService) {
        this.deviceGroups = deviceGroups;
        this.k24Service = k24Service;
        this.bs600Service = bs600Service;
        this.sui201Service = sui201Service;
        this.singlePhaseMeterService = singlePhaseMeterService;
        this.deviceService = deviceService;
        this.excelExportService = excelExportService;
    }

    @Scheduled(fixedRate = 10000) // 1分钟执行一次
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
            CompletableFuture<K24FlowMeterData> k24Future = CompletableFuture.supplyAsync(() ->
                    k24Service.readData(group.getK24().getName(), group.getK24().getSlaveId())
            );

            CompletableFuture<BS600DifferentialPressureData> bs600Future = CompletableFuture.supplyAsync(() ->
                    bs600Service.readData(group.getBs600().getName(), group.getBs600().getSlaveId())
            );

            CompletableFuture<SUI201PowerData> sui201Future = CompletableFuture.supplyAsync(() ->
                    sui201Service.readData(group.getSui201().getName(), group.getSui201().getSlaveId())
            );

            CompletableFuture<SinglePhaseMeterData> singlePhaseMeterFuture = CompletableFuture.supplyAsync(() ->
                    singlePhaseMeterService.readData(group.getSinglePhaseMeter().getName(), group.getSinglePhaseMeter().getSlaveId())
            );

            // 等待所有设备读取完成
            CompletableFuture.allOf(k24Future, bs600Future, sui201Future, singlePhaseMeterFuture).join();

            // 获取数据
            K24FlowMeterData k24Data = k24Future.get();
            BS600DifferentialPressureData bs600Data = bs600Future.get();
            SUI201PowerData sui201Data = sui201Future.get();
            SinglePhaseMeterData singlePhaseMeterData = singlePhaseMeterFuture.get();

            // 存储到InfluxDB
            /*
            deviceService.saveK24Data(groupName, k24Data);
            deviceService.saveBS600Data(groupName, bs600Data);
            deviceService.saveSUI201Data(groupName, sui201Data);
            deviceService.saveSinglePhaseMeterData(groupName, singlePhaseMeterData);
*/
            // 写入Excel
            excelExportService.writeDataToExcel(groupName, k24Data, bs600Data, sui201Data, singlePhaseMeterData);

            // 打印日志
            log.info("组 {} 数据采集完成 - K24: 流量={}L/s | BS600: 主变量={} | SUI201: 功率={}W | 单相电表: 电压={}V",
                    groupName,
                    k24Data.getFlowRate(),
                    bs600Data.getFloatMainValue(),
                    sui201Data.getPower(),
                    singlePhaseMeterData.getVoltage());

        } catch (Exception e) {
            log.error("采集组数据失败: {}", groupName, e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("=== 关闭数据采集服务 ===");
        excelExportService.closeAllWriters();
    }
}
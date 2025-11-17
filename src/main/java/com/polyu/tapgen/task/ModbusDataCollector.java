package com.polyu.tapgen.task;

import com.google.gson.Gson;
import com.polyu.tapgen.config.Constants;
import com.polyu.tapgen.config.DeviceConfig;
import com.polyu.tapgen.config.DeviceGroup;
import com.polyu.tapgen.db.DeviceService;
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
import java.util.stream.Collectors;

@Slf4j
@Component
public class ModbusDataCollector {
    @Resource
    private Map<String, DeviceGroup> deviceGroups;
    @Resource
    private DeviceService deviceService;
    @Resource
    private CsvExportService csvExportService;
    private static final String excelPath = "C:/Users/User/.tapgen";


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
            List<CompletableFuture<List<DeviceValue>>> futures = new ArrayList<>();
            for(DeviceConfig config: group.getDevices()){
                CompletableFuture<List<DeviceValue>> future = CompletableFuture.supplyAsync(() ->
                        deviceService.dataCollection(group.getName(),  config)
                );
                futures.add(future);
            }

            // 等待所有设备读取完成
            List<DeviceValue> allData = futures.stream()
                    .map(CompletableFuture::join) // 等待每个future完成并获取结果
                    .flatMap(List::stream)        // 将List<List>展平为List
                    .collect(Collectors.toList());
            log.info("{}", new Gson().toJson(allData));
            // 写入Excel
            csvExportService.appendDataToDailyCsv(allData, excelPath);
            // 存储到InfluxDB
            deviceService.saveToDB(allData);

        } catch (Exception e) {
            log.error("采集组数据失败: {}", groupName, e);
        }
    }

}
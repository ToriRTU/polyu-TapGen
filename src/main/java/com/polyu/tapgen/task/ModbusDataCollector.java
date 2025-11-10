package com.polyu.tapgen.task;


import com.polyu.tapgen.config.DeviceGroup;
import com.polyu.tapgen.device.*;
import com.polyu.tapgen.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ModbusDataCollector {
    
    private final Map<String, DeviceGroup> deviceGroups;
    private final K24FlowMeterService k24Service;
    private final BS600DifferentialPressureService bs600Service;
    private final SUI201PowerService sui201Service;
    
    public ModbusDataCollector(Map<String, DeviceGroup> deviceGroups,
                              K24FlowMeterService k24Service,
                              BS600DifferentialPressureService bs600Service,
                              SUI201PowerService sui201Service) {
        this.deviceGroups = deviceGroups;
        this.k24Service = k24Service;
        this.bs600Service = bs600Service;
        this.sui201Service = sui201Service;
    }
    
    @Scheduled(fixedRate = 1000) // 1秒执行一次
    public void collectAllGroupsData() {
        log.info("=== 开始采集所有设备组数据 ===");
        
        // 并行采集所有组
        deviceGroups.forEach((groupName, group) -> {
            CompletableFuture.runAsync(() -> collectGroupData(groupName, group));
        });
    }
    
    private void collectGroupData(String groupName, DeviceGroup group) {
        try {
            log.info("开始采集组数据: {}", groupName);
            
            // 并行读取组内三个设备
            CompletableFuture<K24FlowMeterData> k24Future = CompletableFuture.supplyAsync(() ->
                k24Service.readData(group.getK24().getName(), group.getK24().getSlaveId())
            );
            
            CompletableFuture<BS600DifferentialPressureData> bs600Future = CompletableFuture.supplyAsync(() ->
                bs600Service.readData(group.getBs600().getName(), group.getBs600().getSlaveId())
            );
            
            CompletableFuture<SUI201PowerData> sui201Future = CompletableFuture.supplyAsync(() -> 
                sui201Service.readData(group.getSui201().getName(), group.getSui201().getSlaveId())
            );
            
            // 等待所有设备读取完成
            CompletableFuture.allOf(k24Future, bs600Future, sui201Future).join();
            
            // 打印组数据
            K24FlowMeterData k24Data = k24Future.get();
            BS600DifferentialPressureData bs600Data = bs600Future.get();
            SUI201PowerData sui201Data = sui201Future.get();
            
            log.info("组 {} 数据 - K24: 流量={}L/s, 总累计={}L | BS600: 主变量={} | SUI201: 电压={}V, 电流={}A, 功率={}W", 
                    groupName,
                    k24Data.getFlowRate(), k24Data.getTotalAccumulated(),
                    bs600Data.getFloatMainValue(),
                    sui201Data.getVoltage(), sui201Data.getCurrent(), sui201Data.getPower());
                    
        } catch (Exception e) {
            log.error("采集组数据失败: {}", groupName, e);
        }
    }
}
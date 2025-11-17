package com.polyu.tapgen.config;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ip.IpParameters;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "modbus")
public class ModbusTcpConfig {
    
    private TcpConfig tcp;
    private List<DeviceGroup> groups = new ArrayList<>();
    
    @Data
    public static class TcpConfig {
        private int timeout = 3000;
        private int retries = 2;
        private int reconnectInterval = 5000;
    }
    
    @Bean
    public Map<String, DeviceGroup> deviceGroups() {
        Map<String, DeviceGroup> groupMap = new HashMap<>();
        for (DeviceGroup group : groups) {
            groupMap.put(group.getName(), group);
        }
        return groupMap;
    }
    
    @Bean
    public Map<String, ModbusMaster> modbusMasters() {
        Map<String, ModbusMaster> masters = new HashMap<>();
        ModbusFactory modbusFactory = new ModbusFactory();
        for (DeviceGroup group : groups) {
            for(DeviceConfig config: group.getDevices()){
                masters.put(config.getName(), createTcpMaster(config, modbusFactory));
            }
        }
        
        return masters;
    }
    
    private ModbusMaster createTcpMaster(DeviceConfig config, ModbusFactory modbusFactory) {
        IpParameters ipParams = new IpParameters();
        ipParams.setHost(config.getHost());
        ipParams.setPort(config.getPort());
        
        ModbusMaster master = modbusFactory.createTcpMaster(ipParams, false);
        master.setTimeout(tcp.getTimeout());
        master.setRetries(tcp.getRetries());
        
        return master;
    }
}
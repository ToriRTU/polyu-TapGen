package com.polyu.tapgen.manager;

import com.polyu.tapgen.config.ModbusTcpConfig;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ModbusConnectionManager {
    
    private final Map<String, ModbusMaster> modbusMasters;
    private final Map<String, Boolean> connectionStatus = new ConcurrentHashMap<>();
    private final Map<String, Long> lastReconnectTime = new ConcurrentHashMap<>();
    private final long reconnectInterval;
    
    public ModbusConnectionManager(Map<String, ModbusMaster> modbusMasters, 
                                 ModbusTcpConfig tcpConfig) {
        this.modbusMasters = modbusMasters;
        this.reconnectInterval = tcpConfig.getTcp().getReconnectInterval();
        
        // 初始化连接状态
        for (String deviceName : modbusMasters.keySet()) {
            connectionStatus.put(deviceName, false);
            lastReconnectTime.put(deviceName, 0L);
        }
    }
    
    public boolean ensureConnected(String deviceName) {
        ModbusMaster master = modbusMasters.get(deviceName);
        if (master == null) {
            log.error("设备未配置: {}", deviceName);
            return false;
        }
        
        // 检查当前连接状态
        if (connectionStatus.get(deviceName)) {
            return true;
        }
        
        // 检查重连间隔
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastReconnectTime.get(deviceName) < reconnectInterval) {
            return false;
        }
        
        // 尝试连接
        try {
            if (!master.isInitialized()) {
                master.init();

            }

            connectionStatus.put(deviceName, true);
            log.info("设备连接成功: {}", deviceName);
            return true;
        } catch (Exception e) {
            connectionStatus.put(deviceName, false);
            lastReconnectTime.put(deviceName, currentTime);
            log.warn("设备连接失败: {} - {}", deviceName, e.getMessage());
            return false;
        }
    }
    
    public void updateConnectionStatus(String deviceName, boolean connected) {
        connectionStatus.put(deviceName, connected);
        if (!connected) {
            lastReconnectTime.put(deviceName, System.currentTimeMillis());
        }
    }
    
    public boolean isConnected(String deviceName) {
        return connectionStatus.getOrDefault(deviceName, false);
    }
    
    public ModbusMaster getMaster(String deviceName) {
        return modbusMasters.get(deviceName);
    }
}
package com.polyu.tapgen.service;

import com.google.gson.Gson;
import com.polyu.tapgen.device.SUI201PowerData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SUI201PowerService {
    
    private final ModbusConnectionManager connectionManager;
    private final ModbusBatchReaderService batchReaderService;
    
    public SUI201PowerService(ModbusConnectionManager connectionManager,
                             ModbusBatchReaderService batchReaderService) {
        this.connectionManager = connectionManager;
        this.batchReaderService = batchReaderService;
    }
    
    public SUI201PowerData readData(String deviceName, int slaveId) {
        SUI201PowerData data = new SUI201PowerData();
        data.setDeviceName(deviceName);
        data.setTimestamp(DateTimeUtil.now());
        
        // 确保设备连接
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }
        
        ModbusMaster master = connectionManager.getMaster(deviceName);
        
        try {
            // 一次性读取SUI201所有需要的寄存器
            // 电压(3000-3001), 电流(3002-3003), 功率(3004-3005), 累计电量(3006-3007)
            // 总共需要8个寄存器
            short[] registers = batchReaderService.readHoldingRegisters(master, slaveId, 3000, 8);
            log.info("{}: {}", deviceName, registers);
            // 解析数据
            // 电压测量值 (地址 3000-3001) - 有符号32位整数，字节交换
            int voltage = batchReaderService.getInt32Swapped(registers, 0);
            data.setVoltage(voltage / 1000.0f);
            
            // 电流测量值 (地址 3002-3003) - 有符号32位整数，字节交换
            int current = batchReaderService.getInt32Swapped(registers, 2);
            data.setCurrent(current / 1000.0f);
            
            // 功率 (地址 3004-3005) - 有符号32位整数，字节交换
            int power = batchReaderService.getInt32Swapped(registers, 4);
            data.setPower(power / 1000.0f);
            
            // 累计电量 (地址 3006-3007) - 有符号32位整数，字节交换
            int accumulatedEnergy = batchReaderService.getInt32Swapped(registers, 6);
            log.info("{}: {}", deviceName, accumulatedEnergy);
            data.setAccumulatedEnergy(accumulatedEnergy / 10.0f);
            
            // 更新连接状态为正常
            connectionManager.updateConnectionStatus(deviceName, true);
            
            log.debug("SUI201批量读取成功: 测量值{}个寄存器", registers.length);
            log.debug("{}", new Gson().toJson(data));
        } catch (Exception e) {
            log.error("读取SUI-201功率计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
}
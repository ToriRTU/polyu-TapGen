package com.polyu.tapgen.service;

import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.device.K24FlowMeterData;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class K24FlowMeterService {
    
    private final ModbusConnectionManager connectionManager;
    private final ModbusReaderService readerService;
    
    public K24FlowMeterService(ModbusConnectionManager connectionManager, 
                              ModbusReaderService readerService) {
        this.connectionManager = connectionManager;
        this.readerService = readerService;
    }
    
    public K24FlowMeterData readData(String deviceName, int slaveId) {
        K24FlowMeterData data = new K24FlowMeterData();
        data.setDeviceName(deviceName);
        data.setTimestamp(LocalDateTime.now());
        
        // 确保设备连接
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }
        
        ModbusMaster master = connectionManager.getMaster(deviceName);
        
        try {
            // 读取计量值 (地址 0x0009, 2个字) - 使用有符号32位整数，字节交换
            Number measurement = readerService.readRegister(master, slaveId, 0x0009, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (measurement != null) {
                data.setFlowRate(measurement.doubleValue() / 1000.0);
            }
            
            // 读取总累 (地址 0x000D, 2个字) - 使用有符号32位整数，字节交换
            Number totalAccumulated = readerService.readRegister(master, slaveId, 0x000D, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (totalAccumulated != null) {
                data.setTotalAccumulated(totalAccumulated.doubleValue() / 1000.0);
            }
            
            // 读取班累 (地址 0x000B, 2个字) - 使用有符号32位整数，字节交换
            Number shiftAccumulated = readerService.readRegister(master, slaveId, 0x000B, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (shiftAccumulated != null) {
                data.setShiftAccumulated(shiftAccumulated.doubleValue() / 1000.0);
            }
            
            // 读取平均流速 (地址 0x000F, 2个字) - 使用有符号32位整数，字节交换
            Number avgVelocity = readerService.readRegister(master, slaveId, 0x000F, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (avgVelocity != null) {
                data.setAverageFlowVelocity(avgVelocity.doubleValue() / 100.0);
            }
            
            // 读取瞬时流速 (地址 0x0017, 2个字) - 使用有符号32位整数，字节交换
            Number instantVelocity = readerService.readRegister(master, slaveId, 0x0017, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (instantVelocity != null) {
                data.setInstantaneousVelocity(instantVelocity.doubleValue() / 100.0);
            }
            
            // 读取单价 (地址 0x0011, 1个字) - 使用无符号16位整数
            Number price = readerService.readRegister(master, slaveId, 0x0011, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (price != null) {
                data.setPrice(price.doubleValue() / 100.0);
            }
            
            // 读取单位 (地址 0x0012, 1个字) - 使用无符号16位整数
            Number unit = readerService.readRegister(master, slaveId, 0x0012, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (unit != null) {
                data.setUnit(unit.intValue());
            }
            
            // 读取系数 (地址 0x0013, 1个字) - 使用无符号16位整数
            Number coefficient = readerService.readRegister(master, slaveId, 0x0013, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (coefficient != null) {
                data.setCoefficient(coefficient.doubleValue() / 1000.0);
            }
            
            // 读取校正脉冲 (地址 0x0014, 1个字) - 使用无符号16位整数
            Number calibrationPulse = readerService.readRegister(master, slaveId, 0x0014, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (calibrationPulse != null) {
                data.setCalibrationPulse(calibrationPulse.intValue());
            }
            
            // 读取时间戳 (地址 0x0015, 2个字) - 使用无符号32位整数，字节交换
            Number timestampReg = readerService.readRegister(master, slaveId, 0x0015, DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED, true);
            if (timestampReg != null) {
                data.setTimestampRegister(timestampReg.longValue());
            }
            
            // 读取时间单位 (地址 0x0019, 1个字) - 使用无符号16位整数
            Number timeUnit = readerService.readRegister(master, slaveId, 0x0019, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (timeUnit != null) {
                data.setTimeUnit(timeUnit.intValue());
            }
            
            // 更新连接状态为正常
            connectionManager.updateConnectionStatus(deviceName, true);
            
        } catch (Exception e) {
            log.error("读取K24流量计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
}
package com.polyu.tapgen.service;

import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.device.SUI201PowerData;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SUI201PowerService {
    
    private final ModbusConnectionManager connectionManager;
    private final ModbusReaderService readerService;
    
    public SUI201PowerService(ModbusConnectionManager connectionManager,
                             ModbusReaderService readerService) {
        this.connectionManager = connectionManager;
        this.readerService = readerService;
    }
    
    public SUI201PowerData readData(String deviceName, int slaveId) {
        SUI201PowerData data = new SUI201PowerData();
        data.setDeviceName(deviceName);
        data.setTimestamp(LocalDateTime.now());
        
        // 确保设备连接
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }
        
        ModbusMaster master = connectionManager.getMaster(deviceName);
        
        try {
            // 读取电压测量值 (地址 3000/0x0BB8, 2个寄存器) - 使用有符号32位整数，字节交换
            Number voltage = readerService.readRegister(master, slaveId, 0x0BB8, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (voltage != null) {
                data.setVoltage(voltage.floatValue() / 1000.0f);
            }
            
            // 读取电流测量值 (地址 3002/0x0BBA, 2个寄存器) - 使用有符号32位整数，字节交换
            Number current = readerService.readRegister(master, slaveId, 0x0BBA, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (current != null) {
                data.setCurrent(current.floatValue() / 1000.0f);
            }
            
            // 读取功率 (地址 3004/0x0BBC, 2个寄存器) - 使用有符号32位整数，字节交换
            Number power = readerService.readRegister(master, slaveId, 0x0BBC, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (power != null) {
                data.setPower(power.floatValue() / 1000.0f);
            }
            
            // 读取累计电量 (地址 3006/0x0BBE, 2个寄存器) - 使用有符号32位整数，字节交换
            Number accumulatedEnergy = readerService.readRegister(master, slaveId, 0x0BBE, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, true);
            if (accumulatedEnergy != null) {
                data.setAccumulatedEnergy(accumulatedEnergy.floatValue() / 10.0f);
            }
            
            // 读取波特率 (地址 3100/0x0C1C) - 使用无符号16位整数
            Number baudRate = readerService.readRegister(master, slaveId, 0x0C1C, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (baudRate != null) {
                data.setBaudRate(baudRate.intValue());
            }
            
            // 读取Modbus地址 (地址 3105/0x0C21) - 使用无符号16位整数
            Number modbusAddress = readerService.readRegister(master, slaveId, 0x0C21, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (modbusAddress != null) {
                data.setModbusAddress(modbusAddress.intValue());
            }
            
            // 更新连接状态为正常
            connectionManager.updateConnectionStatus(deviceName, true);
            
        } catch (Exception e) {
            log.error("读取SUI-201功率计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
}
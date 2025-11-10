package com.polyu.tapgen.service;

import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.device.BS600DifferentialPressureData;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class BS600DifferentialPressureService {
    
    private final ModbusConnectionManager connectionManager;
    private final ModbusReaderService readerService;
    
    public BS600DifferentialPressureService(ModbusConnectionManager connectionManager,
                                           ModbusReaderService readerService) {
        this.connectionManager = connectionManager;
        this.readerService = readerService;
    }
    
    public BS600DifferentialPressureData readData(String deviceName, int slaveId) {
        BS600DifferentialPressureData data = new BS600DifferentialPressureData();
        data.setDeviceName(deviceName);
        data.setTimestamp(LocalDateTime.now());
        
        // 确保设备连接
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }
        
        ModbusMaster master = connectionManager.getMaster(deviceName);
        
        try {
            // 读取整型主变量值 (地址 0x0000) - 使用有符号16位整数
            Number intMainValue = readerService.readRegister(master, slaveId, 0x0000, DataType.TWO_BYTE_INT_SIGNED, true);
            if (intMainValue != null) {
                data.setIntMainValue(intMainValue.intValue());
            }
            
            // 读取整型板卡温度值 (地址 0x0001) - 使用有符号16位整数
            Number intBoardTemp = readerService.readRegister(master, slaveId, 0x0001, DataType.TWO_BYTE_INT_SIGNED, true);
            if (intBoardTemp != null) {
                data.setIntBoardTemp(intBoardTemp.intValue());
            }
            
            // 读取浮点主变量值 (地址 0x0002-0x0003) - 使用32位浮点数，字节交换
            Number floatMainValue = readerService.readRegister(master, slaveId, 0x0002, DataType.FOUR_BYTE_FLOAT_SWAPPED, true);
            if (floatMainValue != null) {
                data.setFloatMainValue(floatMainValue.floatValue());
            }
            
            // 读取浮点板卡温度值 (地址 0x0004-0x0005) - 使用32位浮点数，字节交换
            Number floatBoardTemp = readerService.readRegister(master, slaveId, 0x0004, DataType.FOUR_BYTE_FLOAT_SWAPPED, true);
            if (floatBoardTemp != null) {
                data.setFloatBoardTemp(floatBoardTemp.floatValue());
            }
            
            // 读取Modbus地址 (地址 0x0006) - 使用无符号16位整数
            Number modbusAddress = readerService.readRegister(master, slaveId, 0x0006, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (modbusAddress != null) {
                data.setModbusAddress(modbusAddress.intValue());
            }
            
            // 读取波特率 (地址 0x0007) - 使用无符号16位整数
            Number baudRate = readerService.readRegister(master, slaveId, 0x0007, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (baudRate != null) {
                data.setBaudRate(baudRate.intValue());
            }
            
            // 读取校验位 (地址 0x0008) - 使用无符号16位整数
            Number parity = readerService.readRegister(master, slaveId, 0x0008, DataType.TWO_BYTE_INT_UNSIGNED, true);
            if (parity != null) {
                data.setParity(parity.intValue());
            }
            
            // 更新连接状态为正常
            connectionManager.updateConnectionStatus(deviceName, true);
            
        } catch (Exception e) {
            log.error("读取BS-600差压计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
}
package com.polyu.tapgen.service;

import com.polyu.tapgen.device.BS600DifferentialPressureData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Calendar;

@Slf4j
@Service
public class BS600DifferentialPressureService {
    
    private final ModbusConnectionManager connectionManager;
    private final ModbusBatchReaderService batchReaderService;
    
    public BS600DifferentialPressureService(ModbusConnectionManager connectionManager,
                                           ModbusBatchReaderService batchReaderService) {
        this.connectionManager = connectionManager;
        this.batchReaderService = batchReaderService;
    }
    
    public BS600DifferentialPressureData readData(String deviceName, int slaveId) {
        BS600DifferentialPressureData data = new BS600DifferentialPressureData();
        data.setDeviceName(deviceName);
        data.setTimestamp(DateTimeUtil.now());
        
        // 确保设备连接
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }
        
        ModbusMaster master = connectionManager.getMaster(deviceName);
        
        try {
            // 一次性读取BS600所有需要的寄存器 (从0x0000到0x0010)
            // 地址范围: 0x0000-0x0010 (共17个寄存器)
            short[] registers = batchReaderService.readHoldingRegisters(master, slaveId, 0x0000, 17);
            log.info("{}:{}", deviceName, registers);
            // 解析数据
            // 整型主变量值 (地址 0x0000) - 有符号16位整数
            int intMainValue = batchReaderService.getInt16(registers, 0);
            data.setIntMainValue(intMainValue);
            
            // 整型板卡温度值 (地址 0x0001) - 有符号16位整数
            int intBoardTemp = batchReaderService.getInt16(registers, 1);
            data.setIntBoardTemp(intBoardTemp);
            
            // 浮点主变量值 (地址 0x0002-0x0003) - 32位浮点数，字节交换
            float floatMainValue = batchReaderService.getFloatSwapped(registers, 2);
            data.setFloatMainValue(floatMainValue);
            
            // 浮点板卡温度值 (地址 0x0004-0x0005) - 32位浮点数，字节交换
            float floatBoardTemp = batchReaderService.getFloatSwapped(registers, 4);
            data.setFloatBoardTemp(floatBoardTemp);
            
            // Modbus地址 (地址 0x0006) - 无符号16位整数
            int modbusAddress = batchReaderService.getUInt16(registers, 6);
            data.setModbusAddress(modbusAddress);
            
            // 波特率 (地址 0x0007) - 无符号16位整数
            int baudRate = batchReaderService.getUInt16(registers, 7);
            data.setBaudRate(baudRate);
            
            // 校验位 (地址 0x0008) - 无符号16位整数
            int parity = batchReaderService.getUInt16(registers, 8);
            data.setParity(parity);
            
            // 主变量单位 (地址 0x0009) - 无符号16位整数
            int mainUnit = batchReaderService.getUInt16(registers, 9);
            data.setMainUnit(mainUnit);
            
            // 副变量单位 (地址 0x000A) - 无符号16位整数
            int subUnit = batchReaderService.getUInt16(registers, 10);
            data.setSubUnit(subUnit);
            
            // 主变量小数位数 (地址 0x000B) - 无符号16位整数
            int mainDecimal = batchReaderService.getUInt16(registers, 11);
            data.setMainDecimal(mainDecimal);
            
            // 副变量小数位数 (地址 0x000C) - 无符号16位整数
            int subDecimal = batchReaderService.getUInt16(registers, 12);
            data.setSubDecimal(subDecimal);
            
            // 主变量偏移值 (地址 0x000D-0x000E) - 32位浮点数，字节交换
            float mainOffset = batchReaderService.getFloatSwapped(registers, 13);
            data.setMainOffset(mainOffset);
            
            // 主变量增益值 (地址 0x000F-0x0010) - 32位浮点数，字节交换
            float mainGain = batchReaderService.getFloatSwapped(registers, 15);
            data.setMainGain(mainGain);
            
            // 更新连接状态为正常
            connectionManager.updateConnectionStatus(deviceName, true);
            
            log.debug("BS600批量读取成功: {}个寄存器", registers.length);
            
        } catch (Exception e) {
            log.error("读取BS-600差压计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
}
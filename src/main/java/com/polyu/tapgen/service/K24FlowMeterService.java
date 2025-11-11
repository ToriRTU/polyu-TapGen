package com.polyu.tapgen.service;

import com.polyu.tapgen.device.K24FlowMeterData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class K24FlowMeterService {
    
    private final ModbusConnectionManager connectionManager;
    private final ModbusBatchReaderService batchReaderService;
    
    public K24FlowMeterService(ModbusConnectionManager connectionManager, 
                              ModbusBatchReaderService batchReaderService) {
        this.connectionManager = connectionManager;
        this.batchReaderService = batchReaderService;
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
            // 一次性读取K24所有需要的寄存器 (从0x0000到0x0019)
            // 地址范围: 0x0000-0x0019 (共26个寄存器)
            short[] registers = batchReaderService.readHoldingRegisters(master, slaveId, 0x0000, 26);
            
            // 解析数据
            // 地址 (地址 0x0000) - 无符号16位整数
            int address = batchReaderService.getUInt16(registers, 0);
            // 注意：K24协议中地址是只读的，这里只是读取显示
            
            // 波特率 (地址 0x0001-0x0002) - 32位无符号整数
            long baudRate = batchReaderService.getUInt32(registers, 1);
            // 注意：波特率是配置信息，采集时可能不需要
            
            // 产品信息 (地址 0x0003-0x0004) - 32位数据
            long productInfo = batchReaderService.getUInt32(registers, 3);
            
            // 硬件信息 (地址 0x0005-0x0006) - 32位数据
            long hardwareInfo = batchReaderService.getUInt32(registers, 5);
            
            // 软件信息 (地址 0x0007-0x0008) - 32位数据
            long softwareInfo = batchReaderService.getUInt32(registers, 7);
            
            // 计量值 (地址 0x0009-0x000A) - 32位有符号整数，低3位为小数
            int measurement = batchReaderService.getInt32Swapped(registers, 9);
            data.setFlowRate(measurement / 1000.0);
            
            // 班累 (地址 0x000B-0x000C) - 32位有符号整数，低3位为小数
            int shiftAccumulated = batchReaderService.getInt32Swapped(registers, 11);
            data.setShiftAccumulated(shiftAccumulated / 1000.0);
            
            // 总累 (地址 0x000D-0x000E) - 32位有符号整数，低3位为小数
            int totalAccumulated = batchReaderService.getInt32Swapped(registers, 13);
            data.setTotalAccumulated(totalAccumulated / 1000.0);
            
            // 平均流速 (地址 0x000F-0x0010) - 32位有符号整数，低2位为小数
            int avgVelocity = batchReaderService.getInt32Swapped(registers, 15);
            data.setAverageFlowVelocity(avgVelocity / 100.0);
            
            // 单价 (地址 0x0011) - 16位无符号整数，低2位为小数
            int price = batchReaderService.getUInt16(registers, 17);
            data.setPrice(price / 100.0);
            
            // 单位 (地址 0x0012) - 16位无符号整数
            int unit = batchReaderService.getUInt16(registers, 18);
            data.setUnit(unit);
            
            // 系数 (地址 0x0013) - 16位无符号整数，低3位为小数
            int coefficient = batchReaderService.getUInt16(registers, 19);
            data.setCoefficient(coefficient / 1000.0);
            
            // 校正脉冲 (地址 0x0014) - 16位无符号整数
            int calibrationPulse = batchReaderService.getUInt16(registers, 20);
            data.setCalibrationPulse(calibrationPulse);
            
            // 时间戳 (地址 0x0015-0x0016) - 32位无符号整数
            long timestampReg = batchReaderService.getUInt32Swapped(registers, 21);
            data.setTimestampRegister(timestampReg);
            
            // 瞬时流速 (地址 0x0017-0x0018) - 32位有符号整数，低2位为小数
            int instantVelocity = batchReaderService.getInt32Swapped(registers, 23);
            data.setInstantaneousVelocity(instantVelocity / 100.0);
            
            // 时间单位 (地址 0x0019) - 16位无符号整数
            int timeUnit = batchReaderService.getUInt16(registers, 25);
            data.setTimeUnit(timeUnit);
            
            // 更新连接状态为正常
            connectionManager.updateConnectionStatus(deviceName, true);
            
            log.debug("K24批量读取成功: {}个寄存器", registers.length);
            
        } catch (Exception e) {
            log.error("读取K24流量计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
}
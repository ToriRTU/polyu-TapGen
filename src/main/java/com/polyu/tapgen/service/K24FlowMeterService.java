package com.polyu.tapgen.service;

import com.polyu.tapgen.device.K24FlowMeterData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        data.setTimestamp(DateTimeUtil.now());
        
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }
        
        ModbusMaster master = connectionManager.getMaster(deviceName);
        
        try {
            // 读取所有寄存器
            short[] registers = batchReaderService.readHoldingRegisters(master, slaveId, 0x0000, 26);
            
            // 调试输出（可选）
            if (log.isDebugEnabled()) {
                batchReaderService.debugRegisters(registers, deviceName);
                testDifferentParsingMethods(registers, data);
            } else {
                // 生产环境使用修正后的解析
                parseDataWithCorrectOrder(registers, data);
            }
            
            connectionManager.updateConnectionStatus(deviceName, true);
            
        } catch (Exception e) {
            log.error("读取K24流量计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
    
    /**
     * 使用正确的字节序解析数据（K24使用ABCD顺序）
     */
    private void parseDataWithCorrectOrder(short[] registers, K24FlowMeterData data) {
        // 计量值 (地址 0x0009-0x000A) - 使用ABCD顺序
        int measurement = batchReaderService.getInt32(registers, 9);
        data.setFlowRate(measurement / 1000.0);
        
        // 班累 (地址 0x000B-0x000C)
        int shiftAccumulated = batchReaderService.getInt32(registers, 11);
        data.setShiftAccumulated(shiftAccumulated / 1000.0);
        
        // 总累 (地址 0x000D-0x000E)
        int totalAccumulated = batchReaderService.getInt32(registers, 13);
        data.setTotalAccumulated(totalAccumulated / 1000.0);
        
        // 平均流速 (地址 0x000F-0x0010)
        int avgVelocity = batchReaderService.getInt32(registers, 15);
        data.setAverageFlowVelocity(avgVelocity / 100.0);
        
        // 瞬时流速 (地址 0x0017-0x0018)
        int instantVelocity = batchReaderService.getInt32(registers, 23);
        data.setInstantaneousVelocity(instantVelocity / 100.0);
        
        // 其他单寄存器数据
        data.setPrice(batchReaderService.getUInt16(registers, 17) / 100.0);
        data.setUnit(batchReaderService.getUInt16(registers, 18));
        data.setCoefficient(batchReaderService.getUInt16(registers, 19) / 1000.0);
        data.setCalibrationPulse(batchReaderService.getUInt16(registers, 20));
        data.setTimestampRegister(batchReaderService.getUInt32(registers, 21)); // ABCD顺序
        data.setTimeUnit(batchReaderService.getUInt16(registers, 25));
    }
    
    /**
     * 测试不同的解析方式（仅用于调试）
     */
    private void testDifferentParsingMethods(short[] registers, K24FlowMeterData data) {
        // 计量值 (地址 0x0009-0x000A)
        int measurement1 = batchReaderService.getInt32(registers, 9);        // ABCD顺序
        int measurement2 = batchReaderService.getInt32Swapped(registers, 9); // CDAB顺序
        
        log.debug("K24计量值解析测试:");
        log.debug("  ABCD有符号: {} -> {}", measurement1, measurement1 / 1000.0);
        log.debug("  CDAB有符号: {} -> {}", measurement2, measurement2 / 1000.0);
        
        // 使用正确的ABCD顺序
        data.setFlowRate(measurement1 / 1000.0);
        
        // 其他字段也使用ABCD顺序
        data.setTotalAccumulated(batchReaderService.getInt32(registers, 13) / 1000.0);
        data.setAverageFlowVelocity(batchReaderService.getInt32(registers, 15) / 100.0);
        data.setInstantaneousVelocity(batchReaderService.getInt32(registers, 23) / 100.0);
        
        // 单寄存器数据
        data.setPrice(batchReaderService.getUInt16(registers, 17) / 100.0);
        data.setUnit(batchReaderService.getUInt16(registers, 18));
        data.setCoefficient(batchReaderService.getUInt16(registers, 19) / 1000.0);
        data.setCalibrationPulse(batchReaderService.getUInt16(registers, 20));
        data.setTimestampRegister(batchReaderService.getUInt32(registers, 21));
        data.setTimeUnit(batchReaderService.getUInt16(registers, 25));
        data.setShiftAccumulated(batchReaderService.getInt32(registers, 11) / 1000.0);
    }
}
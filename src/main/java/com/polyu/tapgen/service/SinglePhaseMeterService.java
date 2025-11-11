package com.polyu.tapgen.service;

import com.polyu.tapgen.device.SinglePhaseMeterData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class SinglePhaseMeterService {
    
    private final ModbusConnectionManager connectionManager;
    private final ModbusBatchReaderService batchReaderService;
    
    public SinglePhaseMeterService(ModbusConnectionManager connectionManager,
                                  ModbusBatchReaderService batchReaderService) {
        this.connectionManager = connectionManager;
        this.batchReaderService = batchReaderService;
    }
    
    public SinglePhaseMeterData readData(String deviceName, int slaveId) {
        SinglePhaseMeterData data = new SinglePhaseMeterData();
        data.setDeviceName(deviceName);
        data.setTimestamp(DateTimeUtil.now());
        
        // 确保设备连接
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }
        
        ModbusMaster master = connectionManager.getMaster(deviceName);
        
        try {
            // 一次性读取主要电参数寄存器 (0x00-0x06)
            short[] mainParams = batchReaderService.readHoldingRegisters(master, slaveId, 0x00, 7);
            log.info("{}: {}", deviceName, mainParams);
            // 解析基本电参数
            // 电压 (地址 0x00) - XXX.XXV，需要除以100
            int voltage = batchReaderService.getUInt16(mainParams, 0);
            data.setVoltage(voltage / 100.0);
            
            // 电流 (地址 0x01) - XXX.XA，需要除以10
            int current = batchReaderService.getUInt16(mainParams, 1);
            data.setCurrent(current / 10.0);
            
            // 有功功率 (地址 0x02) - XXXXW
            int activePower = batchReaderService.getInt16(mainParams, 2);
            data.setActivePower((double) activePower);
            
            // 无功功率 (地址 0x03) - XXXXVar
            int reactivePower = batchReaderService.getInt16(mainParams, 3);
            data.setReactivePower((double) reactivePower);
            
            // 视在功率 (地址 0x04) - XXXXVA
            int apparentPower = batchReaderService.getInt16(mainParams, 4);
            data.setApparentPower((double) apparentPower);
            
            // 功率因素 (地址 0x05) - XX.XX，需要除以100
            int powerFactor = batchReaderService.getInt16(mainParams, 5);
            data.setPowerFactor(powerFactor / 100.0);
            
            // 频率 (地址 0x06) - XX.XXHZ，需要除以100
            int frequency = batchReaderService.getUInt16(mainParams, 6);
            data.setFrequency(frequency / 100.0);
            
            // 读取电能数据 (0x1D-0x20)
            short[] energyRegisters = batchReaderService.readHoldingRegisters(master, slaveId, 0x1D, 4);
            
            // 正向有功电能 (地址 0x1D-0x1E) - 32位无符号整数，XX.XXkwh，需要除以100
            long forwardEnergy = batchReaderService.getUInt32(energyRegisters, 0);
            data.setForwardActiveEnergy(forwardEnergy / 100.0);
            
            // 反向有功电能 (地址 0x1F-0x20) - 32位无符号整数，XX.XXkwh，需要除以100
            long reverseEnergy = batchReaderService.getUInt32(energyRegisters, 2);
            data.setReverseActiveEnergy(reverseEnergy / 100.0);
            
            // 更新连接状态为正常
            connectionManager.updateConnectionStatus(deviceName, true);
            
            log.debug("单相电表批量读取成功: 主要参数{}个, 电能{}个寄存器", 
                     mainParams.length, energyRegisters.length);
            
        } catch (Exception e) {
            log.error("读取单相电表数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }
        
        return data;
    }
}
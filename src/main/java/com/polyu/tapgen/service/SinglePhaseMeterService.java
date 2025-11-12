package com.polyu.tapgen.service;

import com.google.gson.Gson;
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

        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }

        ModbusMaster master = connectionManager.getMaster(deviceName);

        try {
            // 读取主要电参数寄存器 (0x00-0x06)
            short[] mainParams = batchReaderService.readHoldingRegisters(master, slaveId, 0x00, 7);

            // 读取电能数据寄存器 (0x1D-0x20)
            short[] energyRegisters = batchReaderService.readHoldingRegisters(master, slaveId, 0x1D, 4);

            // 读取状态信息寄存器 (0x46-0x47)
            short[] statusRegisters = batchReaderService.readHoldingRegisters(master, slaveId, 0x46, 2);

            // 调试输出
            batchReaderService.debugRegisters(mainParams, deviceName + "-电参数");
            batchReaderService.debugRegisters(energyRegisters, deviceName + "-电能");
            batchReaderService.debugRegisters(statusRegisters, deviceName + "-状态");

            // 解析数据
            parseMainParameters(mainParams, data);
            parseEnergyData(energyRegisters, data);
            parseStatusData(statusRegisters, data);
            log.debug("{}", new Gson().toJson(data));
            connectionManager.updateConnectionStatus(deviceName, true);

        } catch (Exception e) {
            log.error("读取单相电表数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }

        return data;
    }

    /**
     * 解析主要电参数
     */
    private void parseMainParameters(short[] registers, SinglePhaseMeterData data) {
        // 电压 (地址 0x00) - XXX.XXV，需要除以100
        int voltage = batchReaderService.getUInt16(registers, 0);
        data.setVoltage(voltage / 100.0);

        // 电流 (地址 0x01) - XXX.XA，需要除以10
        int current = batchReaderService.getUInt16(registers, 1);
        data.setCurrent(current / 10.0);

        // 有功功率 (地址 0x02) - XXXXW
        int activePower = batchReaderService.getInt16(registers, 2);
        data.setActivePower((double) activePower);

        // 无功功率 (地址 0x03) - XXXXVar
        int reactivePower = batchReaderService.getInt16(registers, 3);
        data.setReactivePower((double) reactivePower);

        // 视在功率 (地址 0x04) - XXXXVA
        int apparentPower = batchReaderService.getInt16(registers, 4);
        data.setApparentPower((double) apparentPower);

        // 功率因素 (地址 0x05) - XX.XX，需要除以100
        int powerFactor = batchReaderService.getInt16(registers, 5);
        data.setPowerFactor(powerFactor / 100.0);

        // 频率 (地址 0x06) - XX.XXHZ，需要除以100
        int frequency = batchReaderService.getUInt16(registers, 6);
        data.setFrequency(frequency / 100.0);
    }

    /**
     * 解析电能数据
     */
    private void parseEnergyData(short[] registers, SinglePhaseMeterData data) {
        // 正向有功电能 (地址 0x1D-0x1E) - 32位无符号整数，XX.XXkwh，需要除以100
        long forwardEnergy1 = batchReaderService.getUInt32(registers, 0);        // ABCD顺序
        long forwardEnergy2 = batchReaderService.getUInt32Swapped(registers, 0); // CDAB顺序

        log.debug("单相电表正向电能解析测试:");
        log.debug("  ABCD无符号: {} -> {}", forwardEnergy1, forwardEnergy1 / 100.0);
        log.debug("  CDAB无符号: {} -> {}", forwardEnergy2, forwardEnergy2 / 100.0);

        // 根据协议，单相电表通常使用ABCD顺序
        data.setForwardActiveEnergy(forwardEnergy1 / 100.0);

        // 反向有功电能 (地址 0x1F-0x20) - 32位无符号整数，XX.XXkwh，需要除以100
        long reverseEnergy1 = batchReaderService.getUInt32(registers, 2);
        long reverseEnergy2 = batchReaderService.getUInt32Swapped(registers, 2);
        data.setReverseActiveEnergy(reverseEnergy1 / 100.0);
    }

    /**
     * 解析状态数据
     */
    private void parseStatusData(short[] registers, SinglePhaseMeterData data) {
        if (registers.length < 2) return;

        // 报警输出 (地址 0x46)
        data.setAlarmOutput(batchReaderService.getUInt16(registers, 0));

        // 开关量输入信号 (地址 0x47)
        data.setDigitalInput(batchReaderService.getUInt16(registers, 1));
    }
}
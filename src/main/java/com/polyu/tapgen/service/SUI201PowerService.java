package com.polyu.tapgen.service;

import com.google.gson.Gson;
import com.polyu.tapgen.device.SUI201PowerData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }

        ModbusMaster master = connectionManager.getMaster(deviceName);

        try {
            // 读取SUI201测量值寄存器 (3000-3007)
            short[] measurementRegisters = batchReaderService.readHoldingRegisters(master, slaveId, 3000, 8);

            // 读取配置寄存器
            short[] configRegisters = batchReaderService.readHoldingRegisters(master, slaveId, 3100, 6);

            // 调试输出
            batchReaderService.debugRegisters(measurementRegisters, deviceName + "-测量值");
            if (configRegisters.length > 0) {
                batchReaderService.debugRegisters(configRegisters, deviceName + "-配置");
            }

            // 解析数据
            parseMeasurementData(measurementRegisters, data);
            parseConfigData(configRegisters, data);
            log.debug("{}", new Gson().toJson(data));
            connectionManager.updateConnectionStatus(deviceName, true);

        } catch (Exception e) {
            log.error("读取SUI-201功率计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }

        return data;
    }

    /**
     * 解析测量数据
     */
    private void parseMeasurementData(short[] registers, SUI201PowerData data) {
        // 电压测量值 (地址 3000-3001)
        int voltage1 = batchReaderService.getInt32(registers, 0);        // ABCD顺序
        int voltage2 = batchReaderService.getInt32Swapped(registers, 0); // CDAB顺序

        log.debug("SUI201电压解析测试:");
        log.debug("  ABCD有符号: {} -> {}", voltage1, voltage1 / 1000.0f);
        log.debug("  CDAB有符号: {} -> {}", voltage2, voltage2 / 1000.0f);

        // 根据协议文档，SUI201使用有符号32位整数，CDAB顺序
        data.setVoltage(voltage2 / 1000.0f);

        // 电流测量值 (地址 3002-3003)
        int current1 = batchReaderService.getInt32(registers, 2);
        int current2 = batchReaderService.getInt32Swapped(registers, 2);
        data.setCurrent(current2 / 1000.0f);

        // 功率 (地址 3004-3005)
        int power1 = batchReaderService.getInt32(registers, 4);
        int power2 = batchReaderService.getInt32Swapped(registers, 4);
        data.setPower(power2 / 1000.0f);

        // 累计电量 (地址 3006-3007)
        int energy1 = batchReaderService.getInt32(registers, 6);
        int energy2 = batchReaderService.getInt32Swapped(registers, 6);
        data.setAccumulatedEnergy(energy2 / 10.0f);
    }

    /**
     * 解析配置数据
     */
    private void parseConfigData(short[] registers, SUI201PowerData data) {
        if (registers.length < 6) return;

        // 波特率 (地址 3100)
        // 其他配置参数（如果需要）
        if (registers.length >= 11) {
            // 电量单位 (地址 3200)
            data.setEnergyUnit(batchReaderService.getUInt16(registers, 6));
            // 系统采样频率 (地址 3201)
            data.setSamplingFrequency(batchReaderService.getUInt16(registers, 7));
            // 电量累积模式 (地址 3202)
            data.setAccumulationMode(batchReaderService.getUInt16(registers, 8));
            // 电压档位模式 (地址 3203)
            data.setVoltageRange(batchReaderService.getUInt16(registers, 9));
            // 库仑计修正电压 (地址 3204-3205)
            int correction = batchReaderService.getInt32Swapped(registers, 10);
            data.setCoulombCorrection(correction / 1000.0f);
        }
    }
}
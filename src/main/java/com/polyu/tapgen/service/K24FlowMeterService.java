package com.polyu.tapgen.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.google.gson.Gson;
import com.polyu.tapgen.device.K24FlowMeterData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.polyu.tapgen.utils.LocalDataUtils;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

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

            // 调试输出
            batchReaderService.debugRegisters(registers, deviceName);

            // 测试不同的解析方式
            testDifferentParsingMethods(registers, data);
            log.debug("{}", new Gson().toJson(data));
            connectionManager.updateConnectionStatus(deviceName, true);

        } catch (Exception e) {
            log.error("读取K24流量计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }

        return data;
    }

    /**
     * 测试不同的解析方式来确定正确的字节序和数据类型
     */
    private void testDifferentParsingMethods(short[] registers, K24FlowMeterData data) {
        // 计量值 (地址 0x0009-0x000A)
        int measurement1 = batchReaderService.getInt32(registers, 9);        // ABCD顺序
        int measurement2 = batchReaderService.getInt32Swapped(registers, 9); // CDAB顺序

        log.debug("计量值解析测试:");
        log.debug("  ABCD有符号: {} -> {}", measurement1, measurement1 / 1000.0);
        log.debug("  CDAB有符号: {} -> {}", measurement2, measurement2 / 1000.0);

        // K24使用ABCD顺序（标准Modbus字节序）
        data.setFlowRate(measurement1 / 1000.0);

        // 总累 (地址 0x000D-0x000E)
        int total1 = batchReaderService.getInt32(registers, 13);
        int total2 = batchReaderService.getInt32Swapped(registers, 13);
        data.setTotalAccumulated(total1 / 1000.0);

        // 平均流速 (地址 0x000F-0x0010)
        int avg1 = batchReaderService.getInt32(registers, 15);
        int avg2 = batchReaderService.getInt32Swapped(registers, 15);
        data.setAverageFlowVelocity(avg1 / 100.0);

        // 瞬时流速 (地址 0x0017-0x0018)
        int instant1 = batchReaderService.getInt32(registers, 23);
        int instant2 = batchReaderService.getInt32Swapped(registers, 23);
        data.setInstantaneousVelocity(instant1 / 100.0);

        // 其他单寄存器数据
        data.setPrice(batchReaderService.getUInt16(registers, 17) / 100.0);
        data.setUnit(batchReaderService.getUInt16(registers, 18));
        data.setCoefficient(batchReaderService.getUInt16(registers, 19) / 1000.0);
        data.setCalibrationPulse(batchReaderService.getUInt16(registers, 20));
        data.setTimestampRegister(batchReaderService.getUInt32(registers, 21)); // ABCD顺序
        data.setTimeUnit(batchReaderService.getUInt16(registers, 25));

        // 班累 (地址 0x000B-0x000C)
        int shift1 = batchReaderService.getInt32(registers, 11);
        int shift2 = batchReaderService.getInt32Swapped(registers, 11);
        data.setShiftAccumulated(shift1 / 1000.0);
    }
}
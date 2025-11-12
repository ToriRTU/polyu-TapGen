package com.polyu.tapgen.service;

import com.google.gson.Gson;
import com.polyu.tapgen.device.BS600DifferentialPressureData;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return data;
        }

        ModbusMaster master = connectionManager.getMaster(deviceName);

        try {
            // 读取BS600所有寄存器 (0x0000-0x0010)
            short[] registers = batchReaderService.readHoldingRegisters(master, slaveId, 0x0000, 17);

            // 调试输出
            batchReaderService.debugRegisters(registers, deviceName);

            // 测试不同的解析方式
            testDifferentParsingMethods(registers, data);
            log.debug("{}", new Gson().toJson(data));
            connectionManager.updateConnectionStatus(deviceName, true);

        } catch (Exception e) {
            log.error("读取BS-600差压计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }

        return data;
    }

    /**
     * 测试不同的解析方式
     */
    private void testDifferentParsingMethods(short[] registers, BS600DifferentialPressureData data) {
        // 整型主变量值 (地址 0x0000)
        data.setIntMainValue(batchReaderService.getInt16(registers, 0));

        // 整型板卡温度值 (地址 0x0001)
        data.setIntBoardTemp(batchReaderService.getInt16(registers, 1));

        // 浮点主变量值 (地址 0x0002-0x0003)
        float floatMain1 = batchReaderService.getFloat(registers, 2);        // ABCD顺序
        float floatMain2 = batchReaderService.getFloatSwapped(registers, 2); // CDAB顺序

        log.debug("BS600浮点主变量值解析测试:");
        log.debug("  ABCD顺序: {}", floatMain1);
        log.debug("  CDAB顺序: {}", floatMain2);

        // 根据协议文档，BS600使用CDAB顺序
        data.setFloatMainValue(floatMain2);

        // 浮点板卡温度值 (地址 0x0004-0x0005)
        float boardTemp1 = batchReaderService.getFloat(registers, 4);
        float boardTemp2 = batchReaderService.getFloatSwapped(registers, 4);
        data.setFloatBoardTemp(boardTemp2);

        // 配置参数
        data.setModbusAddress(batchReaderService.getUInt16(registers, 6));
        data.setBaudRate(batchReaderService.getUInt16(registers, 7));
        data.setParity(batchReaderService.getUInt16(registers, 8));
        data.setMainUnit(batchReaderService.getUInt16(registers, 9));
        data.setSubUnit(batchReaderService.getUInt16(registers, 10));
        data.setMainDecimal(batchReaderService.getUInt16(registers, 11));
        data.setSubDecimal(batchReaderService.getUInt16(registers, 12));

        // 主变量偏移值 (地址 0x000D-0x000E)
        float mainOffset1 = batchReaderService.getFloat(registers, 13);
        float mainOffset2 = batchReaderService.getFloatSwapped(registers, 13);
        data.setMainOffset(mainOffset2);

        // 主变量增益值 (地址 0x000F-0x0010)
        float mainGain1 = batchReaderService.getFloat(registers, 15);
        float mainGain2 = batchReaderService.getFloatSwapped(registers, 15);
        data.setMainGain(mainGain2);
    }
}
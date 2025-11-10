package com.polyu.tapgen.device;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SUI201PowerData {
    private String deviceName;
    private LocalDateTime timestamp;
    private Float voltage; // 电压 V
    private Float current; // 电流 A
    private Float power; // 功率 W
    private Float accumulatedEnergy; // 累计电量 Wh
    private Integer baudRate; // 波特率
    private Integer modbusAddress; // Modbus地址
    private Integer energyUnit; // 电量单位
    private Integer samplingFrequency; // 采样频率
    private Integer accumulationMode; // 电量累积模式
    private Integer voltageRange; // 电压档位模式
    private Float coulombCorrection; // 库仑计修正电压
}
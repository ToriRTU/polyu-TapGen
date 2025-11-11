package com.polyu.tapgen.device;

import lombok.Data;


@Data
public class BS600DifferentialPressureData {
    private String deviceName;
    private String timestamp;
    private Integer intMainValue; // 整型主变量值
    private Integer intBoardTemp; // 整型板卡温度值
    private Float floatMainValue; // 浮点主变量值
    private Float floatBoardTemp; // 浮点板卡温度值
    private Integer modbusAddress; // Modbus地址
    private Integer baudRate; // 波特率
    private Integer parity; // 校验位
    private Integer mainUnit; // 主变量单位
    private Integer subUnit; // 副变量单位
    private Integer mainDecimal; // 主变量小数位数
    private Integer subDecimal; // 副变量小数位数
    private Float mainOffset; // 主变量偏移值
    private Float mainGain; // 主变量增益值
}
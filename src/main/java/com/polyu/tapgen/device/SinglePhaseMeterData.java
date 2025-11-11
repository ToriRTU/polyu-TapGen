package com.polyu.tapgen.device;

import lombok.Data;

@Data
public class SinglePhaseMeterData {
    private String deviceName;
    private String timestamp;
    
    // 基本电参数
    private Double voltage;           // 电压 V
    private Double current;           // 电流 A
    private Double activePower;       // 有功功率 W
    private Double reactivePower;     // 无功功率 Var
    private Double apparentPower;     // 视在功率 VA
    private Double powerFactor;       // 功率因素
    private Double frequency;         // 频率 Hz
    
    // 电能计量
    private Double forwardActiveEnergy;  // 正向有功电能 kWh
    private Double reverseActiveEnergy;  // 反向有功电能 kWh
    
    // 状态信息
    private Integer alarmOutput;      // 报警输出
    private Integer digitalInput;     // 开关量输入信号
    
    // 设备配置
    private Integer password;         // 密码
    private Integer modbusAddress;    // 通讯地址
    private Integer baudRate;         // 波特率
    private Integer parity;           // 校验位
    private Integer ptRatio;          // PT变比
    private Integer ctRatio;          // CT变比
}
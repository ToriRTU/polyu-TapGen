package com.polyu.tapgen.device;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class K24FlowMeterData {
    private String deviceName;
    private LocalDateTime timestamp;
    private Double flowRate; // 瞬时流量 L/s
    private Double totalAccumulated; // 总累计 L
    private Double shiftAccumulated; // 班累 L
    private Double averageFlowVelocity; // 平均流速 L/s
    private Double instantaneousVelocity; // 瞬时流速 L/s
    private Double price; // 单价 元
    private Integer unit; // 单位
    private Double coefficient; // 系数
    private Integer calibrationPulse; // 校正脉冲
    private Long timestampRegister; // 时间戳
    private Integer timeUnit; // 时间单位
}
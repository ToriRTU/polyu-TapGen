package com.polyu.tapgen.modbus;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum DevicePoint {
    // ==================== K24 流量计 ====================
    K24_TOTAL_FLOW("流量计","k24", "总累计流量", "Total flow", 0x0009, 2,  DataType.INT32,0.001, "L"),
    K24_AVG_FLOW("流量计","k24", "平均流量", "Avg flow rate", 0x000F, 2,  DataType.INT32,0.01, "L/min"),
    K24_INSTANT_FLOW("流量计","k24", "瞬时流量", "Flow rate", 0x0017, 2, DataType.INT32, 0.01, "L"),

    // ==================== BS600 差压计 ====================
    BS600_PRESSURE_DIFF("差压计","bs600", "差压", "Pressure difference", 0x0002, 2, DataType.FLOAT_SWAP, 1.0, "mpa"),

    // ==================== 单相电表 (交流功率计) ====================
    AC_METER_VOLTAGE("交流功率计","acpower", "电压", "Voltage", 0x0004, 1, DataType.INT16, 0.1, "V"),
    AC_METER_CURRENT("交流功率计","acpower", "电流", "Current", 0x0005, 1, DataType.INT16, 0.0001, "A"),
    AC_METER_ACTIVE_POWER("交流功率计","acpower", "有功功率", "Input power", 0x0000, 1, DataType.INT16, 0.01, "W"),

    // ==================== SUI-201 直流功率计 ====================
    SUI201_VOLTAGE("直流功率计","sui-201", "电压", "Voltage", 0x0BB8, 2, DataType.INT32, 0.001, "V"),
    SUI201_CURRENT("直流功率计","sui-201", "电流", "Current", 0x0BBA, 2, DataType.INT32, 0.001, "A"),
    SUI201_POWER("直流功率计","sui-201", "功率", "Power", 0x0BBC, 2, DataType.INT32, 0.001, "W"),
    SUI201_ENERGY("直流功率计","sui-201", "累计电量", "Accumulated electricity", 0x0BBE, 2, DataType.INT32, 0.0001, "Wh");

    /**
     * 设备型号/名称
     */
    private final String device;
    private final String deivceCode;
    /**
     * 属性中文名称
     */
    private final String nameCN;
    /**
     * 属性英文编码
     */
    private final String code;
    /**
     * 寄存器地址 (从0开始)
     */
    private final int address;
    /**
     * 寄存器长度 (字数, 1个寄存器=2字节)
     */
    private final int length;
    private final DataType dataType;
    /**
     * 倍率 (原始值乘以倍率得到实际值)
     */
    private final double multiplier;
    /**
     * 单位
     */
    private final String unit;

    DevicePoint(String device,String deivceCode, String nameCN, String code, int address, int length, DataType dataType, double multiplier, String unit) {
        this.device = device;
        this.deivceCode = deivceCode;
        this.nameCN = nameCN;
        this.code = code;
        this.address = address;
        this.length = length;
        this.dataType = dataType;
        this.multiplier = multiplier;
        this.unit = unit;
    }

    /**
     * 根据设备型号和属性编码查找对应的枚举
     */
    public static DevicePoint findByDeviceAndCode(String deviceCode, String code) {
        List<DevicePoint> list = new ArrayList<>();
        for (DevicePoint point : values()) {
            if (point.getDeivceCode().equals(deviceCode) && point.getCode().equals(code)) {
                return point;
            }
        }
        return null;
    }
    public static List<DevicePoint> findByDevice(String deviceCode) {
        List<DevicePoint> list = new ArrayList<>();
        for (DevicePoint point : values()) {
            if (point.getDeivceCode().equals(deviceCode)) {
                list.add(point);
            }
        }
        return list;
    }
}
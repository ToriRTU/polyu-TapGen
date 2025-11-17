package com.polyu.tapgen.modbus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceValue {
    private String group;
    /**
     * 设备型号/名称
     */
    private String device;
    private String deivceCode;
    /**
     * 属性中文名称
     */
    private String nameCN;
    /**
     * 属性英文编码
     */
    private String code;
    /**
     * 寄存器地址 (从0开始)
     */
    private int address;
    /**
     * 寄存器长度 (字数, 1个寄存器=2字节)
     */
    private int length;
    /**
     * 倍率 (原始值乘以倍率得到实际值)
     */
    private double multiplier;
    /**
     * 单位
     */
    private String unit;

    public Double value;

    public String time;

    public DeviceValue(DevicePoint point){
        this.device = point.getDevice();
        this.deivceCode = point.getDeivceCode();
        this.nameCN = point.getNameCN();
        this.code = point.getCode();
        this.address = point.getAddress();
        this.length = point.getLength();
        this.multiplier = point.getMultiplier();
        this.unit = point.getUnit();
    }
    public DeviceValue(DevicePoint point,String group, Double value, String time){
        this.device = point.getDevice();
        this.group = group;
        this.deivceCode = point.getDeivceCode();
        this.nameCN = point.getNameCN();
        this.code = point.getCode();
        this.address = point.getAddress();
        this.length = point.getLength();
        this.multiplier = point.getMultiplier();
        this.unit = point.getUnit();
        this.value = value;
        this.time = time;
    }
    public DeviceValue(DevicePoint point, Double value, String time){
        this.device = point.getDevice();
        this.deivceCode = point.getDeivceCode();
        this.nameCN = point.getNameCN();
        this.code = point.getCode();
        this.address = point.getAddress();
        this.length = point.getLength();
        this.multiplier = point.getMultiplier();
        this.unit = point.getUnit();
        this.value = value;
        this.time = time;
    }
}

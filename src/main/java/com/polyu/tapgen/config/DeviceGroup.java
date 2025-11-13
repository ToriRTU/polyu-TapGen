package com.polyu.tapgen.config;

import lombok.Data;

@Data
public class DeviceGroup {
    private String name;
    private DeviceConfig flowmeter;
    private DeviceConfig pressure;
    private DeviceConfig acpower;
    private DeviceConfig dcpower;
}

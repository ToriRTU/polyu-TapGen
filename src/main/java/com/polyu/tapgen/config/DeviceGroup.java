package com.polyu.tapgen.config;

import lombok.Data;

@Data
public class DeviceGroup {
    private String name;
    private DeviceConfig k24;
    private DeviceConfig bs600;
    private DeviceConfig sui201;
}
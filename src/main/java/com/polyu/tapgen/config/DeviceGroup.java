package com.polyu.tapgen.config;

import lombok.Data;

import java.util.List;

@Data
public class DeviceGroup {
    private String name;
    private List<DeviceConfig> devices;
}

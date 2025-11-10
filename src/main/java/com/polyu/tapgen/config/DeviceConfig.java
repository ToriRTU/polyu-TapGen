package com.polyu.tapgen.config;

import lombok.Data;

@Data
public class DeviceConfig {
    private String name;
    private String host;
    private int port;
    private int slaveId;
}
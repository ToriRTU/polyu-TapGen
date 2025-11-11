package com.polyu.tapgen.utils;

import lombok.Data;

import java.util.Date;
@Data
public class InfluxMapValue {
    private String key;
    private String device;
    private Double value;
    private Date time;
}

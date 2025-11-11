package com.polyu.tapgen.utils;

import com.polyu.tapgen.config.Flux;

public enum InfluxSort {
    VALUE_DESC(Flux.FIELD_INFLUX_TIME, true),
    VALUE_ASC(Flux.FIELD_INFLUX_TIME, false);
    private String column;
    private Boolean desc;

    InfluxSort(String column, Boolean desc){
        this.column = column;
        this.desc = desc;
    }
    public String getColumn(){return column;}
    public Boolean getDesc(){return desc;}
}

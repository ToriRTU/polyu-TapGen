package com.polyu.tapgen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DeviceDataDTO {
    private String group;
    private String device;
    private String code;
    private Double value;
    private Long time;
}

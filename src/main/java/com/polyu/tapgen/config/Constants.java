package com.polyu.tapgen.config;

public class Constants {

    public class Influx{
        public static final String MEASUREMENT = "tapgen";
        public static final String CODE = "code";
        public static final String DEVICE = "device";
        public static final String GROUP = "group";
        public static final String VALUE = "value";
        public static final String BUCKET = "polyu";
        public static final String ONLINE_STATUS = "status";
    }

    public class Device{
        public static final String FLOW_METER = "flowmeter";
        public static final String PRESSURE = "pressure";
        public static final String AC = "acpower";
        public static final String DC = "dcpower";

        public static final String Flow_rate = "Flow rate";
        public static final String Current = "Current";

        public static final String DEVICE_TYPE_FLOW = "flow";
        public static final String DEVICE_TYPE_PRESSURE = "pressure";
        public static final String DEVICE_TYPE_AC = "ac";
        public static final String DEVICE_TYPE_DC = "dc";


    }

}

package com.polyu.tapgen.utils;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class InfluxValueUtils {
    /**
     * 将返回的Influx的值，进行计算，包括分组的数据，计算返回的数值的总和
     * 场景举例：光伏发电，获取每台逆变器的发电量。
     *
     * @param list 包含FluxTable对象的列表
     * @return 总和
     */
    public static Double getSum(List<FluxTable> list) {
        Double result = 0D;
        if(list != null){
            for (FluxTable table : list) {
                for (FluxRecord record : table.getRecords()) {
                    Double value = Double.parseDouble(record.getValue().toString());
                    if (value != null) {
                        result += value;
                    }
                }
            }
        }
        return formatDouble(result);
    }
    public static List<InfluxMapValue> getMapValue( List<FluxTable> list){
        List<InfluxMapValue> result = new ArrayList<>();
        if(list != null){
            for (FluxTable table : list) {
                for (FluxRecord record : table.getRecords()) {
                    InfluxMapValue bean = new InfluxMapValue();
                    bean.setKey(record.getValueByKey(Flux.MAP_RTN_CODE).toString());
                    bean.setDevice(record.getValueByKey(Flux.MAP_RTN_DEVICE).toString());
                    Double value = null;
                    if(record.getValue() != null){
                        value = (Double)record.getValue();
                        value = Math.round(value * 1000.0) / 1000.0;
                    }
                    bean.setValue(value);
                    Instant time = (Instant) record.getValueByKey(Flux.FIELD_INFLUX_TIME);
                    bean.setTime(Date.from(time));
                    result.add(bean);
                }
            }
        }
        return result;
    }

    /**
     * 比较直接，返回InfluxDb的一条数据的value值
     *
     * @param list 包含FluxTable对象的列表
     * @return 第一条记录的value值，如果没有找到则返回null
     */
    public static Double getValue(List<FluxTable> list) {
        if (Objects.nonNull(list) && !list.isEmpty() && !list.get(0).getRecords().isEmpty()) {
            FluxRecord record = list.get(0).getRecords().get(0);
            return Double.parseDouble(record.getValue().toString());
        }
        return null;
    }

    /**
     * 获取图表类型的数据，填充标题和数据列表
     *
     * @param list   包含FluxTable对象的列表
     * @param titles  标题列表
     * @param datas   数据列表
     */
    public static void getValue(List<FluxTable> list, List<String> titles, List<Double> datas) {
        getValue(list, titles, datas, DateUtils.DATE_TIME_FORMATTER);
    }
    public static void getValue(List<FluxTable> list, List<String> titles, List<Double> datas, DateTimeFormatter fmt) {
        if(titles == null){
            titles = new ArrayList<>();
        }else{
            titles.clear();
        }
        if(datas == null){
            datas = new ArrayList<>();
        }else{
            datas.clear();
        }
        if(list != null){
            for (FluxTable table : list) {
                for (FluxRecord record : table.getRecords()) {
                    // 假设_time字段是String类型，value字段是Double类型
                    Double value = null;
                    if(record.getValue() != null){
                        value = formatDouble(Double.parseDouble(record.getValue().toString()));
                    }
                    Instant time = record.getTime();
                    String title = DateUtils.formatTime(time, fmt);
                    if(!titles.contains(title)){
                        titles.add(title);
                        datas.add(value);
                    }else{
                        int index = titles.indexOf(title);
                        Double val = datas.get(index);
                        if(val != null && value != null){
                            val = val + value;
                            datas.set(index, val);
                        }else if(val == null && value != null){
                            datas.set(index, value);
                        }
                    }

                }
            }
        }
    }
    private static Double formatDouble(Double val){
        return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

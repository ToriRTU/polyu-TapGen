package com.polyu.tapgen.db;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;

import com.polyu.tapgen.config.Constants;
import com.polyu.tapgen.config.InfluxConfig;
import com.polyu.tapgen.dto.DeviceDataDTO;
import com.polyu.tapgen.utils.DateUtils;
import com.polyu.tapgen.utils.InfluxBuilder;
import com.polyu.tapgen.utils.InfluxMapValue;
import com.polyu.tapgen.utils.InfluxValueUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class InfluxService {
    @Resource
    private InfluxDBClient influxClient;
    @Resource
    private InfluxConfig config;
    @Resource
    private WriteApi writeApi;

    public void writeData(DeviceDataDTO data) {
        Point point = Point.measurement(Constants.Influx.MEASUREMENT)
            .addTag(Constants.Influx.GROUP, data.getGroup())
            .addTag(Constants.Influx.DEVICE, data.getDevice())
            .addTag(Constants.Influx.CODE, data.getCode())
            .addField(Constants.Influx.GROUP, data.getGroup())
            .addField(Constants.Influx.CODE, data.getCode())
            .addField(Constants.Influx.VALUE, data.getValue())
            .time(data.getTime(), WritePrecision.S);
        try (WriteApi writeApi = influxClient.getWriteApi()) {
            writeApi.writePoint(point);
        }
    }
    public void writeData(List<DeviceDataDTO> data) {
        List<Point> points = data.stream().map(e -> {
            return Point.measurement(Constants.Influx.MEASUREMENT)
                    .addTag(Constants.Influx.GROUP, e.getGroup())
                    .addTag(Constants.Influx.DEVICE, e.getDevice())
                    .addTag(Constants.Influx.CODE, e.getCode())
                    .addField(Constants.Influx.GROUP, e.getGroup())
                    .addField(Constants.Influx.CODE, e.getCode())
                    .addField(Constants.Influx.VALUE, e.getValue())
                    .time(e.getTime(), WritePrecision.S);
        }).collect(Collectors.toList());
        try (WriteApi writeApi = influxClient.getWriteApi()) {
            writeApi.writePoints(points);
        }
    }
    public List<FluxTable> getList(String influxQL) {
        try{
            List<FluxTable> result = influxClient.getQueryApi().query(influxQL, config.getOrg());
            return result;
        }catch (Exception e){
            return null;
        }
    }

    public List<InfluxMapValue> getLast(String device){
        // 获取当天的日期
        LocalDate today = LocalDate.now();
        // 将日期设置为当天的 0 点
        LocalDateTime startOfDay = today.atStartOfDay();
        // 转换为毫秒时间戳
        long start = startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = System.currentTimeMillis();
        InfluxBuilder builder = new InfluxBuilder()
                .time(DateUtils.timestampToRFC3339(start), DateUtils.timestampToRFC3339(end))
                .field(Constants.Influx.VALUE)
                .filter(Constants.Influx.DEVICE, device)
                .last(true);
        List<FluxTable> result = getList(builder.build());
        return InfluxValueUtils.getMapValue(result);
    }
    /**
     * 写入数据
     * @param bucket 桶
     * @param measurement 表
     * @param map 参数
     */
    public void writeData(String bucket, String measurement, Map<String, Object> map, Map<String, String> tags) {
        writeData(bucket, measurement, map, tags, null);
    }
    public void writeData(String bucket,String measurement, Map<String, Object> map, Map<String, String> tags, Long time) {
        if(time == null){
            time = System.currentTimeMillis();
        }
        Point point = Point
                .measurement(measurement)
                .addTags(tags)
                .addFields(map)
                .time(time, WritePrecision.MS);

        writeApi.writePoint(bucket, config.getOrg(), point);

    }

    public List<FluxTable> query(String fluxQuery) {
        return null;
    }
}
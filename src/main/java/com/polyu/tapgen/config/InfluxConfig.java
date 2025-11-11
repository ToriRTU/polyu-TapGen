package com.polyu.tapgen.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// config/InfluxConfig.java
@Configuration
@ConfigurationProperties(prefix = "influx")
@Data
public class InfluxConfig {
    private String url;
    private String token;
    private String org;
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
    @Bean
    public WriteApi writeApi() {
        InfluxDBClient influxDBClient = influxDBClient();
        return influxDBClient.makeWriteApi();
    }
}
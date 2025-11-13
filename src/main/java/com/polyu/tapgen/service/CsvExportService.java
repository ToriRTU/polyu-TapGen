package com.polyu.tapgen.service;

import com.polyu.tapgen.modbus.DevicePoint;
import com.polyu.tapgen.modbus.DeviceValue;
import com.polyu.tapgen.utils.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CsvExportService {

    // 预定义表头顺序
    private final List<String> headerColumns;

    public CsvExportService() {
        this.headerColumns = generateHeaderColumns();
    }

    /**
     * 生成固定的表头顺序
     */
    private List<String> generateHeaderColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("时间");
        
        Map<String, List<DevicePoint>> pointsByDevice = Arrays.stream(DevicePoint.values())
                .collect(Collectors.groupingBy(DevicePoint::getDeivceCode));
        
        for (String deviceCode : pointsByDevice.keySet()) {
            List<DevicePoint> devicePoints = pointsByDevice.get(deviceCode);
            for (DevicePoint point : devicePoints) {
                String columnName = point.getDevice() + "/" + point.getNameCN();
                columns.add(columnName);
            }
        }
        
        return columns;
    }

    /**
     * 追加一个时间点的所有设备数据到当天的CSV文件
     */
    public boolean appendDataToDailyCsv(List<DeviceValue> dataList, String basePath) {
        if (dataList.isEmpty()) {
            return true;
        }
        
        try {
            LocalDateTime timestamp = DateTimeUtil.parse(dataList.get(0).getTime());
            LocalDate date = timestamp.toLocalDate();
            String filePath = getDailyFilePath(date, basePath);
            File csvFile = new File(filePath);
            
            boolean isNewFile = !csvFile.exists();
            
            try (FileWriter writer = new FileWriter(csvFile, true)) {
                if (isNewFile) {
                    // 写入表头
                    String header = String.join(",", headerColumns);
                    writer.write(header + "\n");
                    log.info("创建新的CSV文件并写入表头，共{}列", headerColumns.size());
                }
                
                // 创建数据行并写入
                String row = createCsvRow(dataList, timestamp);
                writer.write(row + "\n");
                
                log.debug("CSV数据追加成功: {} -> {}个属性", 
                         timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                         dataList.size());
                return true;
                
            } catch (IOException e) {
                log.error("写入CSV文件失败: {}", filePath, e);
                return false;
            }
            
        } catch (Exception e) {
            log.error("处理数据失败", e);
            return false;
        }
    }

    /**
     * 获取当天CSV文件路径
     */
    private String getDailyFilePath(LocalDate date, String basePath) {
        String dirPath = basePath + File.separator + "data_export";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String fileName = "设备数据_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        return dirPath + File.separator + fileName;
    }

    /**
     * 创建CSV数据行
     */
    private String createCsvRow(List<DeviceValue> dataList, LocalDateTime timestamp) {
        List<String> row = new ArrayList<>();
        
        // 第一列：时间
        String timeStr = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        row.add(escapeCsv(timeStr));
        
        // 创建数据映射
        Map<String, Double> dataMap = new HashMap<>();
        for (DeviceValue data : dataList) {
            String key = data.getDevice() + "_" + data.getNameCN();
            dataMap.put(key, data.getValue());
        }
        
        // 按照表头顺序填充数据（跳过第一列"时间"）
        for (int i = 1; i < headerColumns.size(); i++) {
            String column = headerColumns.get(i);
            Double value = dataMap.get(column);
            if (value != null) {
                row.add(String.valueOf(value)); // 直接转为字符串
            } else {
                row.add(""); // 空值
            }
        }
        
        return String.join(",", row);
    }

    /**
     * CSV转义处理（如果值包含逗号或引号）
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 检查当天的CSV文件是否存在
     */
    public boolean isTodayFileExists(String basePath) {
        String filePath = getDailyFilePath(LocalDate.now(), basePath);
        return new File(filePath).exists();
    }

    /**
     * 获取当天文件的数据行数（不包括表头）
     */
    public int getTodayFileRowCount(String basePath) {
        try {
            String filePath = getDailyFilePath(LocalDate.now(), basePath);
            File file = new File(filePath);
            if (!file.exists()) {
                return 0;
            }
            
            // 简单统计行数
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                int lines = 0;
                while (reader.readLine() != null) {
                    lines++;
                }
                return Math.max(0, lines - 1); // 减去表头行
            }
            
        } catch (Exception e) {
            log.error("获取文件行数失败", e);
            return 0;
        }
    }

    /**
     * 获取表头信息
     */
    public List<String> getHeaderColumns() {
        return new ArrayList<>(headerColumns);
    }

    /**
     * 批量追加多个时间点的数据（如果需要）
     */
    public boolean appendBatchDataToDailyCsv(List<List<DeviceValue>> batchDataList, String basePath) {
        if (batchDataList.isEmpty()) {
            return true;
        }
        
        try {
            // 按日期分组
            Map<LocalDate, List<List<DeviceValue>>> dataByDate = new HashMap<>();
            for (List<DeviceValue> timeData : batchDataList) {
                if (!timeData.isEmpty()) {
                    LocalDateTime timestamp = DateTimeUtil.parse(timeData.get(0).getTime());
                    LocalDate date = timestamp.toLocalDate();
                    dataByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(timeData);
                }
            }
            
            // 为每个日期处理数据
            for (Map.Entry<LocalDate, List<List<DeviceValue>>> entry : dataByDate.entrySet()) {
                String filePath = getDailyFilePath(entry.getKey(), basePath);
                File csvFile = new File(filePath);
                
                boolean isNewFile = !csvFile.exists();
                
                try (FileWriter writer = new FileWriter(csvFile, true)) {
                    if (isNewFile) {
                        // 写入表头
                        String header = String.join(",", headerColumns);
                        writer.write(header + "\n");
                    }
                    
                    // 按时间排序后写入
                    List<List<DeviceValue>> sortedData = entry.getValue().stream()
                            .sorted(Comparator.comparing(list -> DateTimeUtil.parse(list.get(0).getTime())))
                            .collect(Collectors.toList());
                    
                    for (List<DeviceValue> timeData : sortedData) {
                        LocalDateTime timestamp = DateTimeUtil.parse(timeData.get(0).getTime());
                        String row = createCsvRow(timeData, timestamp);
                        writer.write(row + "\n");
                    }
                    
                    log.info("CSV批量追加成功: {}个时间点 -> {}", sortedData.size(), filePath);
                    
                } catch (IOException e) {
                    log.error("批量追加CSV数据失败: {}", filePath, e);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("处理批量数据失败", e);
            return false;
        }
    }
}
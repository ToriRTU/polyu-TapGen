package com.polyu.tapgen.service;

import com.polyu.tapgen.modbus.DevicePoint;
import com.polyu.tapgen.modbus.DeviceValue;
import com.polyu.tapgen.utils.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
                String columnName = point.getDevice() + "_" + point.getNameCN();
                columns.add(columnName);
            }
        }
        
        return columns;
    }

    /**
     * 追加一个时间点的所有设备数据到当天的CSV文件
     */
    public boolean appendDataToDailyCsv(List<DeviceValue> dataList, String group, String basePath) {
        if (dataList.isEmpty()) {
            log.warn("数据列表为空，无法导出");
            return true;
        }
        
        try {
            LocalDateTime timestamp = DateTimeUtil.parse(dataList.get(0).getTime());
            LocalDate date = timestamp.toLocalDate();
            String filePath = getDailyFilePath(date,group, basePath);
            File csvFile = new File(filePath);
            
            boolean isNewFile = !csvFile.exists();
            
            // 使用UTF-8编码，支持中文
            try (FileOutputStream fos = new FileOutputStream(csvFile, true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                
                // 如果是新文件，写入UTF-8 BOM头，确保Excel正确识别中文
                if (isNewFile) {
                    // 写入UTF-8 BOM
                    fos.write(0xEF);
                    fos.write(0xBB);
                    fos.write(0xBF);
                    
                    // 写入表头
                    String header = String.join(",", headerColumns);
                    writer.write(header);
                    writer.newLine();
                    log.info("创建新的CSV文件并写入表头，共{}列", headerColumns.size());
                }
                
                // 创建数据行并写入
                String row = createCsvRow(dataList, timestamp);
                writer.write(row);
                writer.newLine();
                
                log.info("CSV数据追加成功: {} -> {}个属性，文件: {}", 
                         timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                         dataList.size(),
                         filePath);
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
    private String getDailyFilePath(LocalDate date,String group, String basePath) {
        String dirPath = basePath + File.separator + "data_export";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("创建目录: {}", dirPath);
            }
        }
        
        String fileName = "设备数据_" + group + "_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
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
                row.add(BigDecimal.valueOf(value).toPlainString()); // 直接转为字符串
            } else {
                row.add(""); // 空值
            }
        }
        
        String result = String.join(",", row);
        log.debug("生成的CSV行: {}", result);
        return result;
    }

    /**
     * CSV转义处理
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }


    /**
     * 获取表头信息
     */
    public List<String> getHeaderColumns() {
        return new ArrayList<>(headerColumns);
    }
}
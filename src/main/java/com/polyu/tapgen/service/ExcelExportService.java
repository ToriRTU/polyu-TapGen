package com.polyu.tapgen.service;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.polyu.tapgen.modbus.DevicePoint;
import com.polyu.tapgen.modbus.DeviceValue;
import com.polyu.tapgen.utils.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExcelExportService {

    /**
     * 追加一个时间点的所有设备数据到当天的Excel文件
     */
    public boolean appendDataToDailyExcel(List<DeviceValue> dataList, String basePath) {
        if (dataList.isEmpty()) {
            return true;
        }
        
        try {
            // 获取时间（假设同一时间点的数据时间相同）
            LocalDateTime timestamp = DateTimeUtil.parse(dataList.get(0).getTime());
            LocalDate date = timestamp.toLocalDate();
            String filePath = getDailyFilePath(date, basePath);
            File excelFile = new File(filePath);
            
            try (ExcelWriter writer = ExcelUtil.getWriter(excelFile)) {
                if (!excelFile.exists()) {
                    // 文件不存在，创建并写入表头
                    writeHeader(writer);
                }
                
                // 创建一行数据
                Map<String, Object> row = createDataRow(dataList, timestamp);
                writer.writeRow(row.values());
                
                log.debug("数据追加成功: {} -> {}个属性", 
                         timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                         dataList.size());
                return true;
                
            } catch (Exception e) {
                log.error("追加数据到Excel失败: {}", filePath, e);
                return false;
            }
            
        } catch (Exception e) {
            log.error("处理数据失败", e);
            return false;
        }
    }

    /**
     * 获取当天Excel文件路径
     */
    private String getDailyFilePath(LocalDate date, String basePath) {
        String dirPath = basePath + File.separator + "data_export";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String fileName = "设备数据_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        return dirPath + File.separator + fileName;
    }

    /**
     * 写入表头（基于DevicePoint枚举生成）
     */
    private void writeHeader(ExcelWriter writer) {
        List<Object> header = new ArrayList<>();
        
        // 第一列固定为时间
        header.add("时间");
        
        // 每个属性一列，格式：设备_属性名
        Set<String> columns = getAllColumns();
        header.addAll(columns);
        
        writer.writeRow(header);
        writer.setColumnWidth(-1, 20);
        
        log.info("创建新的Excel文件并写入表头，共{}列", header.size());
    }

    /**
     * 获取所有列头（基于DevicePoint枚举）
     */
    private Set<String> getAllColumns() {
        Set<String> columns = new LinkedHashSet<>();
        
        // 从DevicePoint枚举中提取所有列
        for (DevicePoint point : DevicePoint.values()) {
            String columnName = point.getDevice() + "_" + point.getNameCN();
            columns.add(columnName);
        }
        
        return columns;
    }

    /**
     * 创建数据行
     */
    private Map<String, Object> createDataRow(List<DeviceValue> dataList, LocalDateTime timestamp) {
        Map<String, Object> row = new LinkedHashMap<>();
        
        // 第一列：时间
        String timeStr = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        row.put("时间", timeStr);
        
        // 为每个属性列填充数据
        Set<String> allColumns = getAllColumns();
        for (String column : allColumns) {
            if ("时间".equals(column)) continue;
            
            // 查找对应列的数据
            Optional<DeviceValue> matchedData = dataList.stream()
                .filter(data -> column.equals(data.getDevice() + "_" + data.getNameCN()))
                .findFirst();
            
            if (matchedData.isPresent()) {
                row.put(column, matchedData.get().getValue());
            } else {
                row.put(column, ""); // 没有数据的列留空
            }
        }
        
        return row;
    }



    /**
     * 检查当天的Excel文件是否存在
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
            
            // 使用Hutool读取行数（表头算1行，所以数据行数是总行数-1）
            return ExcelUtil.getReader(file).getRowCount() - 1;
            
        } catch (Exception e) {
            log.error("获取文件行数失败", e);
            return 0;
        }
    }
}
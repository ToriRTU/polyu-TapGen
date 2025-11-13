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

@Slf4j
@Service
public class ExcelExportService {

    /**
     * 导出设备数据到Excel（每天一个文件，支持追加）
     */
    public String exportToExcel(List<DeviceValue> allData, String basePath) {
        try {
            if (allData.isEmpty()) {
                log.warn("没有数据可导出");
                return null;
            }

            // 按日期分组
            Map<LocalDate, List<DeviceValue>> dataByDate = groupDataByDate(allData);

            // 为每天的数据生成或追加Excel
            List<String> exportedFiles = new ArrayList<>();
            for (Map.Entry<LocalDate, List<DeviceValue>> entry : dataByDate.entrySet()) {
                String filePath = exportOrAppendDailyData(entry.getKey(), entry.getValue(), basePath);
                if (filePath != null) {
                    exportedFiles.add(filePath);
                }
            }

            return exportedFiles.isEmpty() ? null : String.join(",", exportedFiles);

        } catch (Exception e) {
            log.error("导出Excel失败", e);
            return null;
        }
    }

    /**
     * 按日期分组数据
     */
    private Map<LocalDate, List<DeviceValue>> groupDataByDate(List<DeviceValue> allData) {
        Map<LocalDate, List<DeviceValue>> result = new HashMap<>();
        
        for (DeviceValue data : allData) {
            LocalDate date = DateTimeUtil.parse(data.getTime()).toLocalDate();
            result.computeIfAbsent(date, k -> new ArrayList<>()).add(data);
        }
        
        return result;
    }

    /**
     * 导出或追加单日数据到Excel
     */
    private String exportOrAppendDailyData(LocalDate date, List<DeviceValue> dailyData, String basePath) {
        if (dailyData.isEmpty()) {
            return null;
        }

        // 创建目录
        String dirPath = basePath + File.separator + "data_export";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成文件名
        String fileName = "设备数据_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String filePath = dirPath + File.separator + fileName;

        File excelFile = new File(filePath);
        
        try {
            if (excelFile.exists()) {
                // 文件存在，追加数据
                return appendToExistingExcel(filePath, dailyData);
            } else {
                // 文件不存在，创建新文件
                return createNewExcel(filePath, dailyData);
            }
            
        } catch (Exception e) {
            log.error("处理Excel文件失败: {}", filePath, e);
            return null;
        }
    }

    /**
     * 创建新的Excel文件
     */
    private String createNewExcel(String filePath, List<DeviceValue> dailyData) {
        try (ExcelWriter writer = ExcelUtil.getWriter(filePath)) {
            
            // 准备表头和数据
            List<Map<String, Object>> rows = prepareExcelData(dailyData);
            
            // 写入数据
            writer.write(rows, true);
            
            // 设置表头样式
            writer.setCurrentRow(0);
            writer.setColumnWidth(-1, 20);
            
            log.info("创建新的Excel文件成功: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            log.error("创建Excel文件失败: {}", filePath, e);
            return null;
        }
    }

    /**
     * 追加数据到已存在的Excel文件
     */
    private String appendToExistingExcel(String filePath, List<DeviceValue> newData) {
        try (ExcelWriter writer = ExcelUtil.getWriter(filePath)) {
            // Hutool的ExcelWriter在append模式下会自动定位到最后一行
            
            // 准备要追加的数据
            List<Map<String, Object>> newRows = prepareExcelData(newData);
            
            // 直接追加数据（不读取现有内容）
            writer.write(newRows, false); // false表示追加模式
            
            log.info("追加数据到Excel文件成功: {}，追加{}条数据", filePath, newRows.size());
            return filePath;
            
        } catch (Exception e) {
            log.error("追加数据到Excel文件失败: {}", filePath, e);
            return null;
        }
    }

    /**
     * 准备Excel数据
     */
    private List<Map<String, Object>> prepareExcelData(List<DeviceValue> dailyData) {
        // 按时间戳分组，同一时间戳的数据放在一行
        Map<LocalDateTime, Map<String, DeviceValue>> dataByTime = new LinkedHashMap<>();
        Set<String> allColumns = new LinkedHashSet<>();
        
        // 第一列固定为时间
        allColumns.add("时间");
        
        // 收集所有属性列
        for (DeviceValue data : dailyData) {
            String columnName = data.getDevice() + "_" + data.getNameCN();
            allColumns.add(columnName);
            
            dataByTime.computeIfAbsent(DateTimeUtil.parse(data.getTime()), k -> new HashMap<>())
                     .put(columnName, data);
        }
        
        // 构建行数据
        List<Map<String, Object>> rows = new ArrayList<>();
        
        for (Map.Entry<LocalDateTime, Map<String, DeviceValue>> timeEntry : dataByTime.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            
            // 第一列：时间
            row.put("时间", timeEntry.getKey().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 后续列：各设备数据
            Map<String, DeviceValue> timeData = timeEntry.getValue();
            for (String column : allColumns) {
                if ("时间".equals(column)) continue;
                
                DeviceValue data = timeData.get(column);
                if (data != null) {
                    row.put(column, data.getValue());
                } else {
                    row.put(column, ""); // 空单元格
                }
            }
            
            rows.add(row);
        }
        
        // 按时间排序
        rows.sort((row1, row2) -> {
            String time1 = (String) row1.get("时间");
            String time2 = (String) row2.get("时间");
            return time1.compareTo(time2);
        });
        
        return rows;
    }

    /**
     * 导出设备数据到Excel（简化版本，所有数据在一个文件）
     */
    public String exportAllToExcel(List<DeviceValue> allData, String basePath) {
        try {
            if (allData.isEmpty()) {
                log.warn("没有数据可导出");
                return null;
            }

            // 创建目录
            String dirPath = basePath + File.separator + "data_export";
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名（包含时间范围）
            LocalDateTime minTime = allData.stream()
                    .map(data -> DateTimeUtil.parse(data.getTime()))
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            LocalDateTime maxTime = allData.stream()
                    .map(data -> DateTimeUtil.parse(data.getTime()))
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            
            String fileName = String.format("设备数据_%s_%s.xlsx",
                    minTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                    maxTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            String filePath = dirPath + File.separator + fileName;

            try (ExcelWriter writer = ExcelUtil.getWriter(filePath)) {
                
                // 准备表头和数据
                List<Map<String, Object>> rows = prepareExcelData(allData);
                
                // 写入数据
                writer.write(rows, true);
                
                // 设置表头样式
                writer.setCurrentRow(0);
                writer.setColumnWidth(-1, 20);
                
                log.info("Excel文件生成成功: {}", filePath);
                return filePath;
                
            } catch (Exception e) {
                log.error("生成Excel文件失败: {}", filePath, e);
                return null;
            }

        } catch (Exception e) {
            log.error("导出Excel失败", e);
            return null;
        }
    }

    /**
     * 获取导出的文件列表
     */
    public List<String> getExportedFiles(String basePath) {
        String dirPath = basePath + File.separator + "data_export";
        File dir = new File(dirPath);
        
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".xlsx"));
        if (files == null) {
            return Collections.emptyList();
        }
        
        List<String> fileList = new ArrayList<>();
        for (File file : files) {
            fileList.add(file.getAbsolutePath());
        }
        
        // 按修改时间排序（最新的在前）
        fileList.sort((f1, f2) -> {
            File file1 = new File(f1);
            File file2 = new File(f2);
            return Long.compare(file2.lastModified(), file1.lastModified());
        });
        
        return fileList;
    }

    /**
     * 删除旧的导出文件（保留最近N天）
     */
    public void cleanOldFiles(String basePath, int keepDays) {
        String dirPath = basePath + File.separator + "data_export";
        File dir = new File(dirPath);
        
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".xlsx"));
        if (files == null) {
            return;
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(keepDays);
        
        for (File file : files) {
            try {
                String fileName = file.getName();
                // 从文件名中提取日期
                if (fileName.startsWith("设备数据_") && fileName.length() >= 16) {
                    String dateStr = fileName.substring(4, 12); // yyyyMMdd
                    LocalDate fileDate = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
                    
                    if (fileDate.isBefore(cutoffDate)) {
                        if (file.delete()) {
                            log.info("删除旧文件: {}", file.getName());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("处理文件时出错: {}", file.getName(), e);
            }
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean isFileExists(String basePath, LocalDate date) {
        String dirPath = basePath + File.separator + "data_export";
        String fileName = "设备数据_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String filePath = dirPath + File.separator + fileName;
        
        return new File(filePath).exists();
    }

    /**
     * 获取文件大小
     */
    public long getFileSize(String basePath, LocalDate date) {
        String dirPath = basePath + File.separator + "data_export";
        String fileName = "设备数据_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String filePath = dirPath + File.separator + fileName;
        
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }
}
package com.polyu.tapgen.service;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
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
     * 追加一条数据到当天的Excel文件
     */
    public boolean appendDataToDailyExcel(DeviceValue data, String basePath) {
        try {
            LocalDate date = DateTimeUtil.parse(data.getTime()).toLocalDate();
            String filePath = getDailyFilePath(date, basePath);
            File excelFile = new File(filePath);
            
            try (ExcelWriter writer = ExcelUtil.getWriter(excelFile)) {
                if (!excelFile.exists()) {
                    // 文件不存在，创建并写入表头
                    writeHeader(writer, data);
                }
                
                // 追加数据行
                Map<String, Object> row = createDataRow(data);
                writer.writeRow(row.values());
                
                log.debug("数据追加成功: {} -> {}", data.getDevice() + "_" + data.getNameCN(), filePath);
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
     * 批量追加数据到当天的Excel文件
     */
    public boolean appendBatchDataToDailyExcel(List<DeviceValue> dataList, String basePath) {
        if (dataList.isEmpty()) {
            return true;
        }
        
        try {
            // 按日期分组
            Map<LocalDate, List<DeviceValue>> dataByDate = new HashMap<>();
            for (DeviceValue data : dataList) {
                LocalDate date = DateTimeUtil.parse(data.getTime()).toLocalDate();
                dataByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(data);
            }
            
            // 为每个日期处理数据
            for (Map.Entry<LocalDate, List<DeviceValue>> entry : dataByDate.entrySet()) {
                String filePath = getDailyFilePath(entry.getKey(), basePath);
                File excelFile = new File(filePath);
                
                try (ExcelWriter writer = ExcelUtil.getWriter(excelFile)) {
                    if (!excelFile.exists()) {
                        // 文件不存在，创建并写入表头（使用第一条数据生成表头）
                        writeHeader(writer, entry.getValue().get(0));
                    }
                    
                    // 按时间排序后追加
                    List<DeviceValue> sortedData = entry.getValue().stream()
                            .sorted(Comparator.comparing(d -> DateTimeUtil.parse(d.getTime())))
                            .collect(Collectors.toList());
                    
                    for (DeviceValue data : sortedData) {
                        Map<String, Object> row = createDataRow(data);
                        writer.writeRow(row.values());
                    }
                    
                    log.info("批量追加成功: {}条数据 -> {}", sortedData.size(), filePath);
                    
                } catch (Exception e) {
                    log.error("批量追加数据到Excel失败: {}", filePath, e);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("处理批量数据失败", e);
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
     * 写入表头
     */
    private void writeHeader(ExcelWriter writer, DeviceValue sampleData) {
        // 第一列固定为时间
        List<Object> header = new ArrayList<>();
        header.add("时间");
        
        // 生成所有可能的列头（设备_属性名）
        Set<String> columns = getAllColumns();
        header.addAll(columns);
        
        writer.writeRow(header);
        writer.setColumnWidth(-1, 20);
        
        log.info("创建新的Excel文件并写入表头");
    }

    /**
     * 创建数据行
     */
    private Map<String, Object> createDataRow(DeviceValue data) {
        Map<String, Object> row = new LinkedHashMap<>();
        
        // 第一列：时间
        String timeStr = DateTimeUtil.parse(data.getTime())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        row.put("时间", timeStr);
        
        // 数据列：设备_属性名
        String columnName = data.getDevice() + "_" + data.getNameCN();
        row.put(columnName, data.getValue());
        
        return row;
    }

    /**
     * 获取所有可能的列头（根据DeviceValue枚举预定义）
     */
    private Set<String> getAllColumns() {
        Set<String> columns = new LinkedHashSet<>();
        
        // 这里可以根据您的设备枚举预定义所有列
        // 示例列头
        columns.add("K24_总累计流量");
        columns.add("K24_平均流量");
        columns.add("K24_瞬时流量");
        columns.add("BS600_差压");
        columns.add("单相电表_电压");
        columns.add("单相电表_电流");
        columns.add("单相电表_用功功率");
        columns.add("单相电表_视在功率");
        columns.add("单相电表_正相电能");
        columns.add("SUI-201_电压");
        columns.add("SUI-201_电流");
        columns.add("SUI-201_功率");
        columns.add("SUI-201_累计电量");
        
        return columns;
    }

    /**
     * 检查当天的Excel文件是否存在
     */
    public boolean isTodayFileExists(String basePath) {
        String filePath = getDailyFilePath(LocalDate.now(), basePath);
        return new File(filePath).exists();
    }

    /**
     * 获取当天文件的数据行数
     */
    public int getTodayFileRowCount(String basePath) {
        try {
            String filePath = getDailyFilePath(LocalDate.now(), basePath);
            File file = new File(filePath);
            if (!file.exists()) {
                return 0;
            }
            
            // 使用Hutool读取行数（表头算1行）
            return ExcelUtil.getReader(file).getRowCount();
            
        } catch (Exception e) {
            log.error("获取文件行数失败", e);
            return 0;
        }
    }
}
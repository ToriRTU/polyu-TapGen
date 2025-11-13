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

    // 预定义表头顺序
    private final List<String> headerColumns;

    public ExcelExportService() {
        // 初始化时生成固定的表头顺序
        this.headerColumns = generateHeaderColumns();
    }

    /**
     * 生成固定的表头顺序
     */
    private List<String> generateHeaderColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("时间"); // 第一列固定为时间
        
        // 按设备分组，确保表头顺序一致
        Map<String, List<DevicePoint>> pointsByDevice = Arrays.stream(DevicePoint.values())
                .collect(Collectors.groupingBy(DevicePoint::getDeivceCode));
        
        // 按设备顺序添加列
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
            
            boolean isNewFile = !excelFile.exists();
            
            try (ExcelWriter writer = ExcelUtil.getWriter(excelFile)) {
                if (isNewFile) {
                    // 文件不存在，创建并写入表头
                    writer.writeRow(headerColumns);
                    writer.setColumnWidth(-1, 20);
                    writer.setCurrentRow(2);
                    log.info("创建新的Excel文件并写入表头，共{}列", headerColumns.size());
                }else{
                    int lastrow = writer.getRowCount();
                    writer.setCurrentRow(lastrow + 1);
                }
                
                // 创建一行数据（按照表头顺序）
                List<Object> rowData = createDataRow(dataList, timestamp);
                writer.writeRow(rowData);
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
     * 创建数据行（按照表头顺序）
     */
    private List<Object> createDataRow(List<DeviceValue> dataList, LocalDateTime timestamp) {
        List<Object> row = new ArrayList<>();
        
        // 第一列：时间
        String timeStr = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        row.add(timeStr);
        
        // 创建数据映射，便于查找
        Map<String, Double> dataMap = dataList.stream()
                .collect(Collectors.toMap(
                    data -> data.getDevice() + "/" + data.getNameCN(),
                    DeviceValue::getValue
                ));
        
        // 按照表头顺序填充数据（跳过第一列"时间"）
        for (int i = 1; i < headerColumns.size(); i++) {
            String column = headerColumns.get(i);
            Double value = dataMap.get(column);
            row.add(value != null ? value : ""); // 有数据填数据，没有填空字符串
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

    /**
     * 获取表头信息（用于调试）
     */
    public List<String> getHeaderColumns() {
        return new ArrayList<>(headerColumns);
    }
}
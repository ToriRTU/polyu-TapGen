package com.polyu.tapgen.service;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.polyu.tapgen.device.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ExcelExportService {
    
    private final Map<String, ExcelWriter> excelWriters = new ConcurrentHashMap<>();
    private final Map<String, LocalDate> currentDates = new ConcurrentHashMap<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 设备属性中英文映射
    private final Map<String, Map<String, String>> deviceFieldMappings = new HashMap<>();
    
    public ExcelExportService() {
        initFieldMappings();
    }
    
    /**
     * 初始化设备字段中英文映射
     */
    private void initFieldMappings() {
        // K24流量计字段映射
        Map<String, String> k24Mapping = new LinkedHashMap<>();
        k24Mapping.put("flowRate", "瞬时流量(Flow Rate)");
        k24Mapping.put("totalAccumulated", "总累计(Total Accumulated)");
        k24Mapping.put("shiftAccumulated", "班累(Shift Accumulated)");
        k24Mapping.put("averageFlowVelocity", "平均流速(Avg Flow Velocity)");
        k24Mapping.put("instantaneousVelocity", "瞬时流速(Instant Velocity)");
        k24Mapping.put("price", "单价(Price)");
        k24Mapping.put("unit", "单位(Unit)");
        k24Mapping.put("coefficient", "系数(Coefficient)");
        k24Mapping.put("calibrationPulse", "校正脉冲(Calibration Pulse)");
        k24Mapping.put("timestampRegister", "时间戳(Timestamp)");
        k24Mapping.put("timeUnit", "时间单位(Time Unit)");
        deviceFieldMappings.put("k24", k24Mapping);
        
        // BS600差压计字段映射
        Map<String, String> bs600Mapping = new LinkedHashMap<>();
        bs600Mapping.put("intMainValue", "整型主变量(Int Main Value)");
        bs600Mapping.put("intBoardTemp", "板卡温度(Board Temp)");
        bs600Mapping.put("floatMainValue", "浮点主变量(Float Main Value)");
        bs600Mapping.put("floatBoardTemp", "浮点板卡温度(Float Board Temp)");
        bs600Mapping.put("modbusAddress", "Modbus地址(Modbus Address)");
        bs600Mapping.put("baudRate", "波特率(Baud Rate)");
        bs600Mapping.put("parity", "校验位(Parity)");
        bs600Mapping.put("mainUnit", "主变量单位(Main Unit)");
        bs600Mapping.put("subUnit", "副变量单位(Sub Unit)");
        bs600Mapping.put("mainDecimal", "主变量小数位(Main Decimal)");
        bs600Mapping.put("subDecimal", "副变量小数位(Sub Decimal)");
        bs600Mapping.put("mainOffset", "主变量偏移(Main Offset)");
        bs600Mapping.put("mainGain", "主变量增益(Main Gain)");
        deviceFieldMappings.put("bs600", bs600Mapping);
        
        // SUI201功率计字段映射
        Map<String, String> sui201Mapping = new LinkedHashMap<>();
        sui201Mapping.put("voltage", "电压(Voltage)");
        sui201Mapping.put("current", "电流(Current)");
        sui201Mapping.put("power", "功率(Power)");
        sui201Mapping.put("accumulatedEnergy", "累计电量(Accumulated Energy)");
        sui201Mapping.put("baudRate", "波特率(Baud Rate)");
        sui201Mapping.put("modbusAddress", "Modbus地址(Modbus Address)");
        sui201Mapping.put("energyUnit", "电量单位(Energy Unit)");
        sui201Mapping.put("samplingFrequency", "采样频率(Sampling Frequency)");
        sui201Mapping.put("accumulationMode", "累积模式(Accumulation Mode)");
        sui201Mapping.put("voltageRange", "电压档位(Voltage Range)");
        sui201Mapping.put("coulombCorrection", "库仑计修正(Coulomb Correction)");
        deviceFieldMappings.put("sui201", sui201Mapping);
        
        // 单相电表字段映射
        Map<String, String> singlePhaseMapping = new LinkedHashMap<>();
        singlePhaseMapping.put("voltage", "电压(Voltage)");
        singlePhaseMapping.put("current", "电流(Current)");
        singlePhaseMapping.put("activePower", "有功功率(Active Power)");
        singlePhaseMapping.put("reactivePower", "无功功率(Reactive Power)");
        singlePhaseMapping.put("apparentPower", "视在功率(Apparent Power)");
        singlePhaseMapping.put("powerFactor", "功率因素(Power Factor)");
        singlePhaseMapping.put("frequency", "频率(Frequency)");
        singlePhaseMapping.put("forwardActiveEnergy", "正向有功电能(Forward Energy)");
        singlePhaseMapping.put("reverseActiveEnergy", "反向有功电能(Reverse Energy)");
        singlePhaseMapping.put("alarmOutput", "报警输出(Alarm Output)");
        singlePhaseMapping.put("digitalInput", "开关量输入(Digital Input)");
        singlePhaseMapping.put("password", "密码(Password)");
        singlePhaseMapping.put("modbusAddress", "Modbus地址(Modbus Address)");
        singlePhaseMapping.put("baudRate", "波特率(Baud Rate)");
        singlePhaseMapping.put("parity", "校验位(Parity)");
        singlePhaseMapping.put("ptRatio", "PT变比(PT Ratio)");
        singlePhaseMapping.put("ctRatio", "CT变比(CT Ratio)");
        deviceFieldMappings.put("single_phase_meter", singlePhaseMapping);
    }
    
    /**
     * 检查是否需要创建新的Excel文件（每天一个文件）
     */
    private void checkAndCreateExcelFile(String groupName) {
        LocalDate today = LocalDate.now();
        LocalDate currentDate = currentDates.get(groupName);
        
        // 如果日期变化或文件不存在，创建新文件
        if (currentDate == null || !currentDate.equals(today)) {
            closeCurrentWriter(groupName);
            createNewExcelFile(groupName, today);
        }
    }
    
    /**
     * 创建新的Excel文件
     */
    private void createNewExcelFile(String groupName, LocalDate date) {
        try {
            String fileName = "设备数据_" + groupName + "_" + date.format(dateFormatter) + ".xlsx";
            
            // 创建Excel写入器
            ExcelWriter writer = ExcelUtil.getWriter(fileName);
            
            // 设置表头
            setExcelHeader(writer, groupName);
            
            excelWriters.put(groupName, writer);
            currentDates.put(groupName, date);
            
            log.info("创建新的Excel文件: {}", fileName);
            
        } catch (Exception e) {
            log.error("创建Excel文件失败: {}", groupName, e);
        }
    }
    
    /**
     * 设置Excel表头
     */
    private void setExcelHeader(ExcelWriter writer, String groupName) {
        List<String> headers = new ArrayList<>();
        
        // 第一列：时间
        headers.add("时间(Time)");
        
        // 为每个设备类型创建列
        for (Map.Entry<String, Map<String, String>> entry : deviceFieldMappings.entrySet()) {
            String deviceType = entry.getKey();
            Map<String, String> fields = entry.getValue();
            
            for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
                headers.add(getDeviceDisplayName(deviceType) + "-" + fieldEntry.getValue());
            }
        }
        
        // 写入表头
        writer.writeHeadRow(headers);
    }
    
    /**
     * 获取设备显示名称
     */
    private String getDeviceDisplayName(String deviceType) {
        switch (deviceType) {
            case "k24": return "流量计";
            case "bs600": return "差压计";
            case "sui201": return "功率计";
            case "single_phase_meter": return "单相电表";
            default: return deviceType;
        }
    }
    
    /**
     * 写入设备数据到Excel
     */
    public synchronized void writeDataToExcel(String groupName, 
                                            K24FlowMeterData k24Data,
                                            BS600DifferentialPressureData bs600Data,
                                            SUI201PowerData sui201Data,
                                            SinglePhaseMeterData singlePhaseData) {
        try {
            // 检查是否需要创建新文件
            checkAndCreateExcelFile(groupName);
            
            ExcelWriter writer = excelWriters.get(groupName);
            if (writer == null) {
                log.warn("Excel写入器未初始化: {}", groupName);
                return;
            }
            
            // 创建数据行
            List<Object> rowData = new ArrayList<>();
            
            // 第一列：时间
            rowData.add(LocalDateTime.now().format(timeFormatter));
            
            // 添加K24数据
            addDeviceDataToRow(rowData, "k24", k24Data);
            
            // 添加BS600数据
            addDeviceDataToRow(rowData, "bs600", bs600Data);
            
            // 添加SUI201数据
            addDeviceDataToRow(rowData, "sui201", sui201Data);
            
            // 添加单相电表数据
            addDeviceDataToRow(rowData, "single_phase_meter", singlePhaseData);
            
            // 写入行数据
            writer.writeRow(rowData);
            
            // 立即刷新到文件（确保数据不丢失）
            writer.flush();
            
            log.debug("数据写入Excel成功: {}", groupName);
            
        } catch (Exception e) {
            log.error("写入Excel失败: {}", groupName, e);
        }
    }
    
    /**
     * 添加设备数据到行
     */
    private void addDeviceDataToRow(List<Object> rowData, String deviceType, Object data) {
        Map<String, String> fieldMapping = deviceFieldMappings.get(deviceType);
        
        if (data == null) {
            // 如果没有数据，添加空值
            for (int i = 0; i < fieldMapping.size(); i++) {
                rowData.add("-");
            }
            return;
        }
        
        try {
            for (String fieldName : fieldMapping.keySet()) {
                java.lang.reflect.Field field = data.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(data);
                
                if (value != null) {
                    rowData.add(value);
                } else {
                    rowData.add("-");
                }
            }
        } catch (Exception e) {
            log.error("添加设备数据失败: {}", deviceType, e);
            // 添加空值占位
            for (int i = 0; i < fieldMapping.size(); i++) {
                rowData.add("-");
            }
        }
    }
    
    /**
     * 关闭当前的Excel写入器
     */
    private void closeCurrentWriter(String groupName) {
        try {
            ExcelWriter writer = excelWriters.get(groupName);
            if (writer != null) {
                writer.close();
                excelWriters.remove(groupName);
                log.info("关闭Excel写入器: {}", groupName);
            }
        } catch (Exception e) {
            log.error("关闭Excel写入器失败: {}", groupName, e);
        }
    }
    
    /**
     * 获取所有活跃的Excel文件
     */
    public Set<String> getActiveWorkbooks() {
        return excelWriters.keySet();
    }
    
    /**
     * 强制关闭所有Excel写入器
     */
    public void closeAllWriters() {
        for (String groupName : excelWriters.keySet()) {
            closeCurrentWriter(groupName);
        }
        currentDates.clear();
    }
    
    /**
     * 检查并处理日期变更
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    public void handleDateChange() {
        log.info("=== 处理日期变更，创建新的Excel文件 ===");
        for (String groupName : new ArrayList<>(excelWriters.keySet())) {
            checkAndCreateExcelFile(groupName);
        }
    }
    
    /**
     * 应用关闭时清理资源
     */
    public void destroy() {
        closeAllWriters();
    }
}
package com.polyu.tapgen.db;

import com.google.gson.Gson;
import com.polyu.tapgen.config.DeviceConfig;
import com.polyu.tapgen.config.DeviceGroup;
import com.polyu.tapgen.dto.DeviceDataDTO;
import com.polyu.tapgen.manager.ModbusConnectionManager;
import com.polyu.tapgen.modbus.DevicePoint;
import com.polyu.tapgen.modbus.DeviceValue;
import com.polyu.tapgen.service.ModbusBatchReaderService;
import com.polyu.tapgen.task.ModbusDataCollector;
import com.polyu.tapgen.utils.DateTimeUtil;
import com.serotonin.modbus4j.ModbusMaster;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.common.value.qual.StringVal;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceService {

    @Resource
    private InfluxService influxService;
    @Resource
    private ModbusConnectionManager connectionManager;
    @Resource
    private ModbusBatchReaderService readerService;
    // 小数格式化（仅用于显示）
    private final DecimalFormat decimalFormat = new DecimalFormat("#.###");

    /**
     * 采集数据
     */
    public List<DeviceValue> dataCollection(String groupName, DeviceConfig config) {
        List<DeviceValue> result = new ArrayList<>();
        List<DevicePoint> points = DevicePoint.findByDevice(config.getType());
        String time = DateTimeUtil.now();
        String deviceName = config.getName();
        int slaveId = config.getSlaveId();
        if (!connectionManager.ensureConnected(deviceName)) {
            log.warn("设备未连接，跳过读取: {}", deviceName);
            return result;
        }
        ModbusMaster master = connectionManager.getMaster(deviceName);

        try {
            for(DevicePoint point: points){
                short[] registerValues = readerService.readHoldingRegisters(master, slaveId, point.getAddress(), point.getLength());
                Double val = null;
                switch (point.getDataType()) {
                    case INT16:
                        val =((Integer) readerService.getInt16(registerValues)).doubleValue();
                        break;
                    case UINT16:
                        val =((Integer) readerService.getUInt16(registerValues)).doubleValue();
                        break;
                    case INT32:
                        val =((Integer) readerService.getInt32(registerValues)).doubleValue();
                        break;
                    case INT32_SWAP:
                        val =((Integer) readerService.getInt32Swapped(registerValues)).doubleValue();
                        break;
                    case UINT32:
                        val =((Long) readerService.getUInt32(registerValues)).doubleValue();
                        break;
                    case UINT32_SWAP:
                        val =((Long) readerService.getUInt32Swapped(registerValues)).doubleValue();
                        break;
                    case FLOAT:
                        val =((Float) readerService.getFloat(registerValues)).doubleValue();
                        break;
                    case FLOAT_SWAP:
                        val =((Float) readerService.getFloatSwapped(registerValues)).doubleValue();
                        break;
                    default:
                        throw new IllegalArgumentException("不支持的数据类型: " + point.getDataType());
                }

                if(val != null){
                    val = val * point.getMultiplier();
                    BigDecimal bd = new BigDecimal(val);
                    bd = bd.setScale(3, RoundingMode.HALF_UP);
                    val = bd.doubleValue();
                    result.add(new DeviceValue(point,groupName,deviceName, val, time));
                }
            }
            // 读取配置寄存器
            connectionManager.updateConnectionStatus(deviceName, true);
        } catch (Exception e) {
            log.error("读取SUI-201功率计数据失败: {}", deviceName, e);
            connectionManager.updateConnectionStatus(deviceName, false);
        }

        return result;
    }

    public void saveToDB(List<DeviceValue> datas){
        List<DeviceDataDTO> list = datas.stream().map(e -> new DeviceDataDTO(e.getGroup(), e.getDeviceName(), e.getCode(), e.getValue(), DateTimeUtil.parse(e.getTime()).atZone(ZoneId.systemDefault()).toEpochSecond())).collect(Collectors.toList());
        log.info("{}", new Gson().toJson(list));
        influxService.writeData(list);
    }
}

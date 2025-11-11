package com.polyu.tapgen.db;

import com.polyu.tapgen.device.BS600DifferentialPressureData;
import com.polyu.tapgen.device.K24FlowMeterData;
import com.polyu.tapgen.device.SUI201PowerData;
import com.polyu.tapgen.device.SinglePhaseMeterData;
import com.polyu.tapgen.dto.DeviceDataDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceService {

    @Resource
    private InfluxService influxService;

    /**
     * 存储K24流量计数据到InfluxDB
     */
    public void saveK24Data(String groupName, K24FlowMeterData data) {
        if (data == null) return;

        List<DeviceDataDTO> points = convertK24ToPoints(groupName, data);
        influxService.writeData(points);
    }

    /**
     * 存储BS600差压计数据到InfluxDB
     */
    public void saveBS600Data(String groupName, BS600DifferentialPressureData data) {
        if (data == null) return;
        List<DeviceDataDTO> points = convertBS600ToPoints(groupName, data);
        influxService.writeData(points);
    }

    /**
     * 存储SUI201功率计数据到InfluxDB
     */
    public void saveSUI201Data(String groupName, SUI201PowerData data) {
        if (data == null) return;
        List<DeviceDataDTO> points = convertSUI201ToPoints(groupName, data);
        influxService.writeData(points);
    }

    /**
     * 存储单相电表数据到InfluxDB
     */
    public void saveSinglePhaseMeterData(String groupName, SinglePhaseMeterData data) {
        if (data == null) return;
        List<DeviceDataDTO> points = convertSinglePhaseMeterToPoints(groupName, data);
        influxService.writeData(points);
    }

    /**
     * 将K24数据转换为设备数据点列表
     */
    private List<DeviceDataDTO> convertK24ToPoints(String groupName, K24FlowMeterData data) {
        List<DeviceDataDTO> points = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        // 添加各个属性作为独立的数据点
        if (data.getFlowRate() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "flow_rate", data.getFlowRate(), timestamp));
        }
        if (data.getTotalAccumulated() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "total_accumulated", data.getTotalAccumulated(), timestamp));
        }
        if (data.getShiftAccumulated() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "shift_accumulated", data.getShiftAccumulated(), timestamp));
        }
        if (data.getAverageFlowVelocity() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "average_flow_velocity", data.getAverageFlowVelocity(), timestamp));
        }
        if (data.getInstantaneousVelocity() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "instantaneous_velocity", data.getInstantaneousVelocity(), timestamp));
        }
        if (data.getPrice() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "price", data.getPrice(), timestamp));
        }
        if (data.getCoefficient() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "coefficient", data.getCoefficient(), timestamp));
        }
        if (data.getCalibrationPulse() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "calibration_pulse", data.getCalibrationPulse().doubleValue(), timestamp));
        }

        return points;
    }

    /**
     * 将BS600数据转换为设备数据点列表
     */
    private List<DeviceDataDTO> convertBS600ToPoints(String groupName, BS600DifferentialPressureData data) {
        List<DeviceDataDTO> points = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        if (data.getIntMainValue() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "int_main_value", data.getIntMainValue().doubleValue(), timestamp));
        }
        if (data.getIntBoardTemp() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "int_board_temp", data.getIntBoardTemp().doubleValue(), timestamp));
        }
        if (data.getFloatMainValue() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "float_main_value", data.getFloatMainValue().doubleValue(), timestamp));
        }
        if (data.getFloatBoardTemp() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "float_board_temp", data.getFloatBoardTemp().doubleValue(), timestamp));
        }
        if (data.getMainOffset() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "main_offset", data.getMainOffset().doubleValue(), timestamp));
        }
        if (data.getMainGain() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "main_gain", data.getMainGain().doubleValue(), timestamp));
        }

        return points;
    }

    /**
     * 将SUI201数据转换为设备数据点列表
     */
    private List<DeviceDataDTO> convertSUI201ToPoints(String groupName, SUI201PowerData data) {
        List<DeviceDataDTO> points = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        if (data.getVoltage() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "voltage", data.getVoltage().doubleValue(), timestamp));
        }
        if (data.getCurrent() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "current", data.getCurrent().doubleValue(), timestamp));
        }
        if (data.getPower() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "power", data.getPower().doubleValue(), timestamp));
        }
        if (data.getAccumulatedEnergy() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "accumulated_energy", data.getAccumulatedEnergy().doubleValue(), timestamp));
        }

        return points;
    }

    /**
     * 将单相电表数据转换为设备数据点列表
     */
    private List<DeviceDataDTO> convertSinglePhaseMeterToPoints(String groupName, SinglePhaseMeterData data) {
        List<DeviceDataDTO> points = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        if (data.getVoltage() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "voltage", data.getVoltage(), timestamp));
        }
        if (data.getCurrent() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "current", data.getCurrent(), timestamp));
        }
        if (data.getActivePower() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "active_power", data.getActivePower(), timestamp));
        }
        if (data.getReactivePower() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "reactive_power", data.getReactivePower(), timestamp));
        }
        if (data.getApparentPower() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "apparent_power", data.getApparentPower(), timestamp));
        }
        if (data.getPowerFactor() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "power_factor", data.getPowerFactor(), timestamp));
        }
        if (data.getFrequency() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "frequency", data.getFrequency(), timestamp));
        }
        if (data.getForwardActiveEnergy() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "forward_active_energy", data.getForwardActiveEnergy(), timestamp));
        }
        if (data.getReverseActiveEnergy() != null) {
            points.add(new DeviceDataDTO(groupName, data.getDeviceName(), "reverse_active_energy", data.getReverseActiveEnergy(), timestamp));
        }

        return points;
    }

}

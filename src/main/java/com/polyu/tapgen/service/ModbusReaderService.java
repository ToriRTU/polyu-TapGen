package com.polyu.tapgen.service;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.locator.BaseLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModbusReaderService {
    
    public Object readHoldingRegister(ModbusMaster master, int slaveId, int register, int dataType) {
        try {
            BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, register, dataType);
            return master.getValue(locator);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("读取保持寄存器失败: slaveId={}, register={}, dataType={}", slaveId, register, dataType, e);
            return null;
        }
    }
    
    public Object readInputRegister(ModbusMaster master, int slaveId, int register, int dataType) {
        try {
            BaseLocator<Number> locator = BaseLocator.inputRegister(slaveId, register, dataType);
            return master.getValue(locator);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("读取输入寄存器失败: slaveId={}, register={}, dataType={}", slaveId, register, dataType, e);
            return null;
        }
    }
    
    public Number readRegister(ModbusMaster master, int slaveId, int register, int dataType, boolean isHolding) {
        if (isHolding) {
            return (Number) readHoldingRegister(master, slaveId, register, dataType);
        } else {
            return (Number) readInputRegister(master, slaveId, register, dataType);
        }
    }
}
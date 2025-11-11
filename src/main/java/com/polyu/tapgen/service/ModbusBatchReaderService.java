package com.polyu.tapgen.service;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModbusBatchReaderService {
    
    /**
     * 批量读取保持寄存器 - 真正的单次请求批量读取
     */
    public short[] readHoldingRegisters(ModbusMaster master, int slaveId, int startRegister, int quantity) {
        try {
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startRegister, quantity);
            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
            
            if (response.isException()) {
                log.error("批量读取保持寄存器异常: slaveId={}, startRegister={}, quantity={}, exceptionCode={}", 
                         slaveId, startRegister, quantity, response.getExceptionCode());
                return new short[quantity];
            }
            
            // 获取响应数据
            byte[] data = response.getData();
            short[] registers = new short[quantity];
            
            // 将字节数据转换为short数组
            for (int i = 0; i < quantity; i++) {
                int index = i * 2;
                if (index + 1 < data.length) {
                    // Modbus是大端字节序
                    int value = ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
                    registers[i] = (short) value;
                } else {
                    registers[i] = 0;
                }
            }
            
            return registers;
            
        } catch (ModbusTransportException e) {
            log.error("批量读取保持寄存器通信失败: slaveId={}, startRegister={}, quantity={}", 
                     slaveId, startRegister, quantity, e);
            return new short[quantity];
        } catch (Exception e) {
            log.error("批量读取保持寄存器失败: slaveId={}, startRegister={}, quantity={}", 
                     slaveId, startRegister, quantity, e);
            return new short[quantity];
        }
    }
    
    /**
     * 从寄存器数组中提取32位有符号整数（字节交换）
     */
    public int getInt32Swapped(short[] registers, int startIndex) {
        if (startIndex + 1 >= registers.length) {
            return 0;
        }
        int low = registers[startIndex] & 0xFFFF;
        int high = registers[startIndex + 1] & 0xFFFF;
        return (high << 16) | low;
    }
    
    /**
     * 从寄存器数组中提取32位无符号整数（字节交换）
     */
    public long getUInt32Swapped(short[] registers, int startIndex) {
        if (startIndex + 1 >= registers.length) {
            return 0;
        }
        long low = registers[startIndex] & 0xFFFFL;
        long high = registers[startIndex + 1] & 0xFFFFL;
        return (high << 16) | low;
    }
    
    /**
     * 从寄存器数组中提取32位浮点数（字节交换）
     */
    public float getFloatSwapped(short[] registers, int startIndex) {
        if (startIndex + 1 >= registers.length) {
            return 0;
        }
        // 将两个16位寄存器转换为32位整数
        int intValue = getInt32Swapped(registers, startIndex);
        return Float.intBitsToFloat(intValue);
    }
    
    /**
     * 从寄存器数组中提取16位无符号整数
     */
    public int getUInt16(short[] registers, int index) {
        if (index >= registers.length) {
            return 0;
        }
        return registers[index] & 0xFFFF;
    }
    
    /**
     * 从寄存器数组中提取16位有符号整数
     */
    public int getInt16(short[] registers, int index) {
        if (index >= registers.length) {
            return 0;
        }
        return (short) registers[index];
    }
    
    /**
     * 从寄存器数组中提取32位有符号整数（不交换字节）
     */
    public int getInt32(short[] registers, int startIndex) {
        if (startIndex + 1 >= registers.length) {
            return 0;
        }
        int high = registers[startIndex] & 0xFFFF;
        int low = registers[startIndex + 1] & 0xFFFF;
        return (high << 16) | low;
    }
    
    /**
     * 从寄存器数组中提取32位无符号整数（不交换字节）
     */
    public long getUInt32(short[] registers, int startIndex) {
        if (startIndex + 1 >= registers.length) {
            return 0;
        }
        long high = registers[startIndex] & 0xFFFFL;
        long low = registers[startIndex + 1] & 0xFFFFL;
        return (high << 16) | low;
    }
}
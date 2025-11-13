package com.polyu.tapgen.modbus;

public enum DataType {
        INT16,      // 16位有符号整数
        UINT16,     // 16位无符号整数  
        INT32,      // 32位有符号整数 (ABCD顺序)
        INT32_SWAP, // 32位有符号整数 (CDAB顺序)
        UINT32,     // 32位无符号整数 (ABCD顺序)
        UINT32_SWAP,// 32位无符号整数 (CDAB顺序)
        FLOAT,      // 32位浮点数 (ABCD顺序)
        FLOAT_SWAP  // 32位浮点数 (CDAB顺序)
    }
# PolyU SolarNexus DataSuite 技术手册

## 项目介绍

PolyU SolarNexus DataSuite 是一个专为太阳能系统设计的数据采集、处理和报表生成平台。该系统负责从各种设备（如Modbus设备、GL840设备等）收集数据，将数据存储到InfluxDB时序数据库中，并提供数据可视化、报表生成等功能。系统采用Spring Boot框架开发，提供REST API接口和定时任务服务。

## 技术栈

### 核心框架
- **Spring Boot 2.7.0**：应用程序的基础框架
- **Java 17**：开发语言

### 数据存储
- **InfluxDB**：时序数据库，用于存储设备采集的数据

### 设备通信
- **Modbus4j**：用于Modbus协议通信
- **jSerialComm & jssc**：用于串口通信

### 数据处理与报表
- **Apache POI**：用于Excel报表的生成和处理
- **Hutool**：工具类库，提供Excel操作、日期处理等功能

### 其他工具库
- **Lombok**：简化Java代码
- **Gson**：JSON处理
- **Caffeine**：本地缓存
- **Apache Commons Lang3**：通用工具类

## 技术特点

### 1. 多设备数据采集
系统支持多种类型的设备数据采集，包括：
- Modbus设备（如逆变器、电表等）
- GL840设备（数据记录仪）
- MPPT设备（最大功率点跟踪器）

设备配置采用JSON文件进行管理，位于resources/config目录下，便于扩展和维护。

### 2. 时序数据存储
系统使用InfluxDB时序数据库存储设备采集的数据，具有以下特点：
- 高效存储和查询时间序列数据
- 支持数据标签（tags）和字段（fields）
- 提供灵活的数据聚合和分析功能

### 3. 定时报表生成
系统提供定时报表生成功能，通过ReportService实现：
- 支持按时间范围自动创建或更新Excel报表
- 动态文件管理，避免数据重复和遗漏
- 支持通过环境变量配置报表存储路径

### 4. 可扩展的设备配置
系统采用配置文件方式管理设备信息，便于添加新设备和修改设备参数：
- modbus_device.json：Modbus设备配置
- modbus_attribute.json：Modbus设备属性配置
- mppt_attribute.json：MPPT设备属性配置
- gl840_device.json：GL840设备配置

## 配置项

### 应用配置（application.yml）

```yaml
server:
  port: 8000  # 应用服务端口

spring:
  application:
    name: solar-nexus-datasuite  # 应用名称
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss  # 日期格式
    time-zone: GMT+8  # 时区

influx:
  url: http://192.168.0.109:8086  # InfluxDB服务器地址
  token: xxxxxxxx  # InfluxDB访问令牌
  bucket: iot  # InfluxDB存储桶
  org: yisquare  # InfluxDB组织

logging:
  level:
    root: info
    com.polyu.solarnexus: debug  # 日志级别
  config: classpath:logback-spring.xml  # 日志配置文件
```

### 环境变量

- **SOLAR_REPORT_DIR**：报表存储路径，默认为`/home/user/dev/report`

## 主要组件

### 1. InfluxService
负责与InfluxDB交互，提供数据写入和查询功能。

### 2. DeviceService
管理设备配置和设备数据处理。

### 3. DashboardService
提供数据可视化和仪表盘功能。

### 4. ReportService
负责定时生成Excel报表，支持按时间范围自动管理文件。

### 5. UserService
用户管理服务。

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- InfluxDB 2.x

### 构建与运行

```bash
# 克隆项目
git clone [项目地址]

# 进入项目目录
cd polyu-datasuite

# 构建项目
mvn clean package

# 运行项目
java -jar target/datasuite-0.0.1.jar
```

### 配置报表目录

```bash
# 设置报表存储路径环境变量
export SOLAR_REPORT_DIR=/your/custom/path
```

## 常见问题

### 1. 报表生成失败
- 检查报表存储目录是否存在且有写入权限
- 检查InfluxDB连接配置是否正确
- 查看日志中的详细错误信息

### 2. 设备数据采集异常
- 检查设备连接状态
- 验证设备配置文件中的参数是否正确
- 检查通信协议和端口配置

### 3. InfluxDB连接问题
- 确认InfluxDB服务是否正常运行
- 验证配置文件中的URL、token、bucket和org是否正确
- 检查网络连接是否正常
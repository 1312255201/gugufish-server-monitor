package cn.gugufish.utils;

import cn.gugufish.entity.dto.RuntimeData;
import cn.gugufish.entity.vo.request.RuntimeDetailVO;
import cn.gugufish.entity.vo.request.RuntimeHistoryVO;
import com.alibaba.fastjson2.JSONObject;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * InfluxDB工具类
 * 该工具类用于与InfluxDB时序数据库进行交互，主要功能包括：
 * 1. 初始化InfluxDB客户端连接
 * 2. 写入服务器运行时监控数据
 * 3. 读取服务器历史监控数据
 * InfluxDB是一个用于存储和分析时间序列数据的数据库，特别适合用于监控数据、
 * 实时分析和IoT应用场景。本工具类封装了对InfluxDB的基本操作，简化了数据的存取过程。
 */
@Component
public class InfluxDbUtils {

    /**
     * InfluxDB服务器URL地址，从配置文件中注入
     */
    @Value("${spring.influx.url}")
    String url;
    
    /**
     * InfluxDB用户名，从配置文件中注入
     */
    @Value("${spring.influx.user}")
    String user;
    
    /**
     * InfluxDB密码，从配置文件中注入
     */
    @Value("${spring.influx.password}")
    String password;
    
    /**
     * InfluxDB存储桶名称，用于存储监控数据
     */
    private final String BUCKET = "monitor";
    
    /**
     * InfluxDB组织名称
     */
    private final String ORG = "my_org";
    
    /**
     * InfluxDB客户端实例
     */
    private InfluxDBClient client;
    /**
     * 初始化InfluxDB客户端连接
     * <p>
     * 在Spring容器启动时自动执行，创建与InfluxDB的连接
     * </p>
     */
    @PostConstruct
    public void init() {
        client = InfluxDBClientFactory.create(url, user, password.toCharArray());
    }

    /**
     * 写入服务器运行时监控数据到InfluxDB
     * 将客户端上报的运行时数据(CPU、内存、磁盘、网络等使用情况)写入到InfluxDB中
     * 
     * @param clientId 客户端ID，用于标识数据来源
     * @param vo 运行时数据视图对象，包含CPU使用率、内存使用率等监控指标
     */
    public void writeRuntimeData(int clientId, RuntimeDetailVO vo) {
        // 创建RuntimeData对象并复制属性
        RuntimeData data = new RuntimeData();
        BeanUtils.copyProperties(vo, data);
        // 设置时间戳和客户端ID
        data.setTimestamp(new Date(vo.getTimestamp()).toInstant());
        data.setClientId(clientId);
        // 获取写入API并写入数据
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writeMeasurement(BUCKET, ORG, WritePrecision.NS, data);
    }

    /**
     * 读取服务器历史运行时监控数据
     * 从InfluxDB中查询指定客户端的历史监控数据，默认查询最近1小时的数据
     * 
     * @param clientId 客户端ID，用于查询特定客户端的数据
     * @return 包含历史监控数据的视图对象，数据以时间序列形式返回
     */
    public RuntimeHistoryVO readRuntimeData(int clientId) {
        RuntimeHistoryVO vo = new RuntimeHistoryVO();
        // 构建Flux查询语句，查询最近1小时的数据
        String query = """
                from(bucket: "%s")
                |> range(start: %s)
                |> filter(fn: (r) => r["_measurement"] == "runtime")
                |> filter(fn: (r) => r["clientId"] == "%s")
                """;
        String format = String.format(query, BUCKET, "-1h", clientId);
        // 执行查询
        List<FluxTable> tables = client.getQueryApi().query(format, ORG);
        int size = tables.size();
        if (size == 0) return vo; // 如果没有数据，直接返回空对象
        // 处理查询结果
        List<FluxRecord> records = tables.getFirst().getRecords();
        for (int i = 0; i < records.size(); i++) {
            // 为每个时间点创建一个JSON对象
            JSONObject object = new JSONObject();
            // 设置时间戳
            object.put("timestamp", records.get(i).getTime());
            // 遍历所有表格，获取不同指标的值
            for (int j = 0; j < size; j++) {
                FluxRecord record = tables.get(j).getRecords().get(i);
                // 将字段名和值添加到JSON对象中
                object.put(record.getField(), record.getValue());
            }
            // 将JSON对象添加到结果列表中
            vo.getList().add(object);
        }
        
        return vo;
    }
}
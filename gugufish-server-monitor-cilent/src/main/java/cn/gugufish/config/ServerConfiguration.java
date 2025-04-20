package cn.gugufish.config;

import cn.gugufish.entity.ConnectionConfig;
import cn.gugufish.util.MonitorUtils;
import cn.gugufish.util.NetUtils;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * 服务器配置类
 * 负责客户端与监控服务器的连接配置管理，包括：
 * 1. 读取已保存的连接配置
 * 2. 如果配置不存在，引导用户注册到服务器
 * 3. 保存连接配置到本地文件
 * 4. 应用启动时向服务端更新基本信息
 */
@Slf4j
@Configuration
public class ServerConfiguration implements ApplicationRunner {

    /**
     * 网络工具类，用于与服务端通信
     */
    @Resource
    NetUtils netUtils;

    /**
     * 监控工具类，用于获取系统信息
     */
    @Resource
    MonitorUtils monitorUtils;

    /**
     * 创建连接配置Bean
     * 尝试从配置文件读取连接信息，如果不存在则引导用户注册到服务器
     * @return 连接配置对象
     */
    @Bean
    ConnectionConfig connectionConfig(){
        log.info("正在加载服务器连接配置...");
        ConnectionConfig connectionConfig = this.readConnectionConfigFromFile();
        if(connectionConfig == null){
            connectionConfig = this.registerToServer();
        }
        return connectionConfig;
    }

    /**
     * 应用启动后执行的方法
     * 向服务端更新客户端的基本信息
     * 
     * @param args 应用参数
     * @throws Exception 可能的异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("正在向服务端更新基本信息");
        netUtils.updateBaseDetails(monitorUtils.monitorBaseDetail());
    }

    /**
     * 引导用户注册到监控服务器
     * 交互式地获取服务器地址、令牌和网络接口信息，并尝试注册到服务器
     * 
     * @return 成功注册后的连接配置
     */
    private ConnectionConfig registerToServer() {
        Scanner scanner = new Scanner(System.in);
        String token, address, ifName;
        do{
            System.out.println("请输入服务器地址 地址实例 'http://127.0.0.1:8080' :");
            address = scanner.nextLine();
            //去掉最后的/防止输入bug导致链接失败
            if(address.endsWith("/"))
                address = address.substring(0, address.length()-1);
            log.info("请输入服务端生成的令牌:");
            token = scanner.nextLine();
            List<String> ifs = monitorUtils.listNetworkInterfaceName();
            if(ifs.size() > 1) {
                log.info("检测到您的主机有多个网卡设备: {}", ifs);
                do {
                    log.info("请选择需要监控的设备名称:");
                    ifName = scanner.nextLine();
                } while (!ifs.contains(ifName));
            } else {
                ifName = ifs.get(0);
            }
        }while (!netUtils.registerToServer(address,token));
        ConnectionConfig config = new ConnectionConfig(address, token, ifName);
        this.saveConnectionConfigToFile(config);
        return  config;
    }

    /**
     * 保存连接配置到本地文件
     * 将连接配置序列化为JSON并保存到config/server.json文件
     * @param connectionConfig 要保存的连接配置对象
     */
    private void saveConnectionConfigToFile(ConnectionConfig connectionConfig){
        File dir = new File("config");
        if(!dir.exists() && dir.mkdirs())
            log.info("创建配置文件夹成功");
        File file = new File("config/server.json");
        try(FileWriter fileWriter = new FileWriter(file)){
            fileWriter.write(JSONObject.toJSONString(connectionConfig));
            log.info("保存配置文件成功");
        }catch (IOException e){
            log.error("保存配置文件出错",e);
        }
        log.info("连接信息保存成功");
    }

    /**
     * 从本地文件读取连接配置
     * 从config/server.json文件读取并反序列化为ConnectionConfig对象
     * @return 读取到的连接配置，如果文件不存在或读取失败则返回null
     */
    private ConnectionConfig readConnectionConfigFromFile(){
        File configurationFile = new File("config/server.json");
        if(configurationFile.exists()){
            try (FileInputStream stream = new FileInputStream(configurationFile)){
                String raw =  new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                return JSONObject.parseObject(raw,ConnectionConfig.class);
            } catch (IOException e){
              log.error("读取配置文件出错" , e);
            }
        }
        return null;
    }
}

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

@Slf4j
@Configuration
public class ServerConfiguration implements ApplicationRunner {

    @Resource
    NetUtils netUtils;

    @Resource
    MonitorUtils monitorUtils;

    @Bean
    ConnectionConfig connectionConfig(){
        log.info("正在加载服务器连接配置...");
        ConnectionConfig connectionConfig = this.readConnectionConfigFromFile();
        if(connectionConfig == null){
            connectionConfig = this.registerToServer();
        }
        return connectionConfig;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("正在向服务端更新基本信息");
        netUtils.updateBaseDetails(monitorUtils.monitorBaseDetail());
    }

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

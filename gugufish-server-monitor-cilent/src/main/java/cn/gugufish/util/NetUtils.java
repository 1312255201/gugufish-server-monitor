package cn.gugufish.util;

import cn.gugufish.entity.ConnectionConfig;
import cn.gugufish.entity.Response;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class NetUtils {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Lazy
    @Resource
    ConnectionConfig connectionConfig;

    public boolean registerToServer(String address, String token) {
        log.info("正在向服务器注册...");
        Response response = this.doGet("/register",address,token);
        if(response.isSuccess()){
            log.info("客户端注册成功");
        }
        else{
            log.error("客户端注册失败:{}",response.message());
        }
        return response.isSuccess();
    }
    private Response doGet(String url) {
        return doGet(url,connectionConfig.getAddress(),connectionConfig.getToken());
    }
    private Response doGet(String url, String address, String token) {
        try{
            HttpRequest request = HttpRequest.newBuilder().GET()
                    .uri(URI.create(address+"/monitor"+url))
                    .header("Authorization", token).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return JSONObject.parseObject(response.body(),Response.class);
        }
        catch (Exception e){
            log.error("在发起服务端请求的时候出现了问题",e);
            return Response.errorResponse(e);
        }
    }
}

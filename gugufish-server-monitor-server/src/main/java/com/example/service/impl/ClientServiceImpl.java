package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Client;
import com.example.mapper.ClientMapper;
import com.example.service.ClientService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    private String registerToken = this.generateNewToken();

    private final Map<Integer,Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String,Client> clientTokenCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initClientCache(){
        this.list().forEach(this::addClientCache);
    }

    @Override
    public String registerToken() {
        return registerToken;
    }

    @Override
    public Client findClientById(Integer id) {
        return clientIdCache.get(id);
    }

    @Override
    public Client findClientByToken(String token) {
        return clientTokenCache.get(token);
    }

    @Override
    public boolean verifyAndRegister(String token) {
        if(this.registerToken.equals(token)){
            // 防止重复注册相同的ID
            int id = this.randomClientId();
            QueryWrapper<Client> wrapper = new QueryWrapper<>();
            while (this.exists(wrapper)) {
                id = this.randomClientId();
                wrapper.eq("id", id);
            }
            Client client = new Client(id,"未命名主机",token,new Date());
            if(this.save(client)){
                registerToken = this.generateNewToken();
                this.addClientCache(client);
                return true;
            }
        }
        return false;
    }
    private void addClientCache(Client client){
        clientIdCache.put(client.getId(),client);
        clientTokenCache.put(client.getToken(),client);
    }
    /**
     * 生成一个随机的客户端ID。
     * 生成的ID范围是从10000000到99999999（包含边界值）。
     *
     * @return 一个随机生成的客户端ID
     */
    private int randomClientId() {
        return new Random().nextInt(90000000) + 10000000;
    }
    /**
     * 生成一个随机的客户端ID。
     * 生成的ID范围是从10000000到99999999（包含边界值）。
     *
     * @return 一个随机生成的客户端ID
     */
    private String generateNewToken() {
        String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++)
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        System.out.println(sb);
        return sb.toString();
    }
}

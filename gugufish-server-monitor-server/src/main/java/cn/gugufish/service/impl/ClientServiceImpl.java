package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ClientDetail;
import cn.gugufish.entity.vo.request.ClientDetailVO;
import cn.gugufish.entity.vo.request.RuntimeDetailVO;
import cn.gugufish.mapper.ClientDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.gugufish.entity.dto.Client;
import cn.gugufish.mapper.ClientMapper;
import cn.gugufish.service.ClientService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    private String registerToken = this.generateNewToken();

    private final Map<Integer,Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String,Client> clientTokenCache = new ConcurrentHashMap<>();

    @Resource
    ClientDetailMapper clientDetailMapper;

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
            Client client = new Client(id,"未命名主机",token,"cn","未命名节点",new Date());
            if(this.save(client)){
                registerToken = this.generateNewToken();
                this.addClientCache(client);
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateClientDetail(ClientDetailVO vo, Client client) {
        ClientDetail detail = new ClientDetail();
        BeanUtils.copyProperties(vo,detail);
        detail.setId(client.getId());
        if(Objects.nonNull(clientDetailMapper.selectById(client.getId()))){
            clientDetailMapper.updateById(detail);
        }else{
            clientDetailMapper.insert(detail);
        }
    }

    private Map<Integer, RuntimeDetailVO> currentRuntime = new ConcurrentHashMap<>();

    @Override
    public void updateRuntimeDetail(RuntimeDetailVO vo, Client client) {
        currentRuntime.put(client.getId(), vo);
        System.out.println(vo);
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

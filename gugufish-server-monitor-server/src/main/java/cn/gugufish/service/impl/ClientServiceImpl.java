package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ClientDetail;
import cn.gugufish.entity.dto.ClientSsh;
import cn.gugufish.entity.vo.response.SshSettingsVO;
import cn.gugufish.entity.vo.request.*;
import cn.gugufish.mapper.ClientDetailMapper;
import cn.gugufish.mapper.ClientSshMapper;
import cn.gugufish.utils.InfluxDbUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.gugufish.entity.dto.Client;
import cn.gugufish.mapper.ClientMapper;
import cn.gugufish.service.ClientService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    private String registerToken = this.generateNewToken();

    private final Map<Integer,Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String,Client> clientTokenCache = new ConcurrentHashMap<>();

    @Resource
    ClientDetailMapper clientDetailMapper;

    @Resource
    InfluxDbUtils influx;

    @Resource
    ClientSshMapper sshMapper;

    @PostConstruct
    public void initClientCache(){
        clientTokenCache.clear();
        clientIdCache.clear();
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

    private final Map<Integer, RuntimeDetailVO> currentRuntime = new ConcurrentHashMap<>();

    @Override
    public void updateRuntimeDetail(RuntimeDetailVO vo, Client client) {
        currentRuntime.put(client.getId(), vo);
        influx.writeRuntimeData(client.getId(), vo);
    }
    @Override
    public List<ClientPreviewVO> listClients() {
        return clientIdCache.values().stream().map(client -> {
            ClientPreviewVO vo = client.asViewObject(ClientPreviewVO.class);
            BeanUtils.copyProperties(clientDetailMapper.selectById(vo.getId()), vo);
            RuntimeDetailVO runtime = currentRuntime.get(client.getId());
            if(this.isOnline(runtime)) {
                BeanUtils.copyProperties(runtime, vo);
                vo.setOnline(true);
            }
            return vo;
        }).toList();
    }
    @Override
    public List<ClientSimpleVO> listSimpleList() {
        return clientIdCache.values().stream().map(client -> {
            ClientSimpleVO vo = client.asViewObject(ClientSimpleVO.class);
            BeanUtils.copyProperties(clientDetailMapper.selectById(vo.getId()), vo);
            return vo;
        }).toList();
    }
    @Override
    public void renameClient(RenameClientVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId()).set("name", vo.getName()));
        this.initClientCache();
    }

    @Override
    public void renameNode(RenameNodeVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId())
                .set("node", vo.getNode()).set("location", vo.getLocation()));
        this.initClientCache();
    }

    @Override
    public ClientDetailsVO clientDetails(int clientId) {
        ClientDetailsVO vo = this.clientIdCache.get(clientId).asViewObject(ClientDetailsVO.class);
        BeanUtils.copyProperties(clientDetailMapper.selectById(clientId), vo);
        vo.setOnline(this.isOnline(currentRuntime.get(clientId)));
        return vo;
    }

    @Override
    public RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId) {
        RuntimeHistoryVO vo = influx.readRuntimeData(clientId);
        ClientDetail detail = clientDetailMapper.selectById(clientId);
        BeanUtils.copyProperties(detail, vo);
        return vo;
    }

    @Override
    public RuntimeDetailVO clientRuntimeDetailsNow(int clientId) {
        return currentRuntime.get(clientId);
    }

    @Override
    public void deleteClient(int clientId) {
        this.removeById(clientId);
        clientDetailMapper.deleteById(clientId);
        this.initClientCache();
        currentRuntime.remove(clientId);
    }

    @Override
    public void saveClientSshConnection(SshConnectionVO vo) {
        Client client = clientIdCache.get(vo.getId());
        if(client == null) return;
        ClientSsh ssh = new ClientSsh();
        BeanUtils.copyProperties(vo, ssh);
        if(Objects.nonNull(sshMapper.selectById(client.getId()))) {
            sshMapper.updateById(ssh);
        } else {
            sshMapper.insert(ssh);
        }
    }

    @Override
    public SshSettingsVO sshSettings(int clientId) {
        ClientDetail detail = clientDetailMapper.selectById(clientId);
        ClientSsh ssh = sshMapper.selectById(clientId);
        SshSettingsVO vo;
        if(ssh == null) {
            vo = new SshSettingsVO();
        } else {
            vo = ssh.asViewObject(SshSettingsVO.class);
        }
        vo.setIp(detail.getIp());
        return vo;
    }

    private boolean isOnline(RuntimeDetailVO runtime) {
        return runtime != null && System.currentTimeMillis() - runtime.getTimestamp() < 60 * 1000;
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
        return sb.toString();
    }
}

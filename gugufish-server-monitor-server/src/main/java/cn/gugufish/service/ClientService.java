package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ClientDetailVO;
import cn.gugufish.entity.vo.request.RuntimeDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.gugufish.entity.dto.Client;

public interface ClientService extends IService<Client> {
    String registerToken();
    Client findClientById(Integer id);
    Client findClientByToken(String token);
    boolean verifyAndRegister(String token);
    void updateClientDetail(ClientDetailVO vo,Client client);
    void updateRuntimeDetail(RuntimeDetailVO vo, Client client);
}

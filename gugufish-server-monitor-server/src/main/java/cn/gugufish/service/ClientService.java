package cn.gugufish.service;

import cn.gugufish.entity.vo.response.SshSettingsVO;
import cn.gugufish.entity.vo.request.*;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.gugufish.entity.dto.Client;

import java.util.List;

public interface ClientService extends IService<Client> {
    String registerToken();
    Client findClientById(Integer id);
    Client findClientByToken(String token);
    boolean verifyAndRegister(String token);
    void updateClientDetail(ClientDetailVO vo,Client client);
    void updateRuntimeDetail(RuntimeDetailVO vo, Client client);
    List<ClientPreviewVO> listClients();
    List<ClientSimpleVO> listSimpleList();
    void renameClient(RenameClientVO vo);
    void renameNode(RenameNodeVO vo);
    ClientDetailsVO clientDetails(int clientId);
    RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId);
    RuntimeDetailVO clientRuntimeDetailsNow(int clientId);
    void deleteClient(int clientId);
    void saveClientSshConnection(SshConnectionVO vo);
    SshSettingsVO sshSettings(int clientId);
}

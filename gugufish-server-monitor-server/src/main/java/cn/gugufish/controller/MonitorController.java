package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.dto.Account;
import cn.gugufish.entity.vo.SshSettingsVO;
import cn.gugufish.entity.vo.request.*;
import cn.gugufish.service.AccountService;
import cn.gugufish.service.ClientService;
import cn.gugufish.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * 用于获取服务器信息，以及前端操作服务器的相关接口
 */
@Tag(name = "客户端相关", description = "包括客户机注册，详细信息储存，运行数据存储。")
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Resource
    ClientService service;
    @Resource
    AccountService accountService;
    @Operation(summary = "获取主机列表")
    @GetMapping("/list")
    public RestBean<List<ClientPreviewVO>> listAllClient(@RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                         @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        List<ClientPreviewVO> clients = service.listClients();
        if(this.isAdminAccount(userRole)) {
            return RestBean.success(clients);
        } else {
            List<Integer> ids = this.accountAccessClients(userId);
            return RestBean.success(clients.stream()
                    .filter(vo -> ids.contains(vo.getId()))
                    .toList());
        }
    }
    @Operation(summary = "获取主机简略信息")
    @GetMapping("/simple-list")
    public RestBean<List<ClientSimpleVO>> simpleClientList(@RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.isAdminAccount(userRole)) {
            return RestBean.success(service.listSimpleList());
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "主机重命名")
    @PostMapping("/rename")
    public RestBean<Void> renameClient(@RequestBody @Valid RenameClientVO vo,
                                       @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                       @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.permissionCheck(userId, userRole, vo.getId())) {
            service.renameClient(vo);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "主机节点重命名")
    @PostMapping("/node")
    public RestBean<Void> renameNode(@RequestBody @Valid RenameNodeVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                     @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.permissionCheck(userId, userRole, vo.getId())) {
            service.renameNode(vo);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "主机详细信息")
    @GetMapping("/details")
    public RestBean<ClientDetailsVO> details(int clientId,
                                             @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                             @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(service.clientDetails(clientId));
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "主机运行历史记录")
    @GetMapping("/runtime-history")
    public RestBean<RuntimeHistoryVO> runtimeDetailsHistory(int clientId,
                                                            @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                            @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(service.clientRuntimeDetailsHistory(clientId));
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "主机当前时刻运行记录")
    @GetMapping("/runtime-now")
    public RestBean<RuntimeDetailVO> runtimeDetailsNow(int clientId,
                                                       @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                       @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(service.clientRuntimeDetailsNow(clientId));
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "主机注册Tocken获取")
    @GetMapping("/register")
    public RestBean<String> registerToken(@RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole)) {
            return RestBean.success(service.registerToken());
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "主机删除")
    @GetMapping("/delete")
    public RestBean<String> deleteClient(int clientId,
                                         @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole)) {
            service.deleteClient(clientId);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "ssh信息保存")
    @PostMapping("/ssh-save")
    public RestBean<Void> saveSshConnection(@RequestBody @Valid SshConnectionVO vo,
                                            @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                            @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.permissionCheck(userId, userRole, vo.getId())) {
            service.saveClientSshConnection(vo);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }
    @Operation(summary = "ssh信息获取")
    @GetMapping("/ssh")
    public RestBean<SshSettingsVO> sshSettings(int clientId,
                                               @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                               @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if(this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(service.sshSettings(clientId));
        } else {
            return RestBean.noPermission();
        }
    }
    private List<Integer> accountAccessClients(int uid) {
        Account account = accountService.getById(uid);
        return account.getClientList();
    }

    private boolean isAdminAccount(String role) {
        role = role.substring(5);
        return Const.ROLE_ADMIN.equals(role);
    }

    private boolean permissionCheck(int uid, String role, int clientId) {
        if(this.isAdminAccount(role)) return true;
        return this.accountAccessClients(uid).contains(clientId);
    }
}
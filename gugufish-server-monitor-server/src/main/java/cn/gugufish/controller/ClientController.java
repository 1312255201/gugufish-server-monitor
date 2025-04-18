package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.dto.Client;
import cn.gugufish.entity.vo.request.ClientDetailVO;
import cn.gugufish.entity.vo.request.RuntimeDetailVO;
import cn.gugufish.service.ClientService;
import cn.gugufish.utils.Const;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用于验证相关接受客户端信息
 */
@Tag(name = "客户端相关", description = "包括客户机注册，详细信息储存，运行数据存储。")
@RestController
@RequestMapping("/monitor")
public class ClientController {

    @Resource
    ClientService clientService;

    @GetMapping("/register")
    public RestBean<Void> registerClient(@RequestHeader("Authorization") String token){
        return clientService.verifyAndRegister(token) ? RestBean.success() : RestBean.failure(401, "客户端注册失败，token验证失败");
    }

    @PostMapping("/detail")
    public RestBean<Void> updateClientDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                              @RequestBody @Valid ClientDetailVO vo) {
        clientService.updateClientDetail(vo,client);
        return RestBean.success();
    }

    @PostMapping("/runtime")
    public RestBean<Void> updateRuntimeDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                               @RequestBody @Valid RuntimeDetailVO vo) {
        clientService.updateRuntimeDetail(vo, client);
        return RestBean.success();
    }
}

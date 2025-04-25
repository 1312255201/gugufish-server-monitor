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
 * 客户端控制器
 * 该控制器负责处理来自监控客户端的请求，包括：
 * 1. 客户端注册 - 验证客户端身份并将其注册到系统中
 * 2. 客户端详细信息更新 - 接收并存储客户端的详细配置信息
 * 3. 客户端运行时数据更新 - 接收并存储客户端的实时运行数据
 * 注意：此控制器的所有接口都是供客户端程序调用的，不是给前端页面使用的
 */
@Tag(name = "客户端相关", description = "包括客户机注册，详细信息储存，运行数据存储。")
@RestController
@RequestMapping("/monitor")
public class ClientController {

    /**
     * 客户端服务，用于处理客户端相关的业务逻辑
     */
    @Resource
    ClientService clientService;

    /**
     * 客户端注册接口
     * 该接口用于验证客户端的身份并将其注册到系统中。客户端需要在请求头中提供有效的授权令牌。
     * 如果令牌验证成功，则注册客户端并返回成功响应；否则返回失败响应。
     * 
     * @param token 授权令牌，包含在请求头的Authorization字段中
     * @return 注册结果，成功返回RestBean.success()，失败返回带有错误信息的RestBean.failure()
     */
    @GetMapping("/register")
    public RestBean<Void> registerClient(@RequestHeader("Authorization") String token){
        return clientService.verifyAndRegister(token) ? RestBean.success() : RestBean.failure(401, "客户端注册失败，token验证失败");
    }

    /**
     * 更新客户端详细信息接口
     * 该接口用于接收并存储客户端的详细配置信息，如操作系统信息、硬件配置等。
     * 客户端需要先完成注册，然后才能调用此接口更新详细信息。
     * 
     * @param client 当前客户端对象，通过请求属性注入，由拦截器根据客户端身份信息设置
     * @param vo 客户端详细信息数据对象，包含需要更新的详细配置信息
     * @return 更新结果，成功返回RestBean.success()
     */
    @PostMapping("/detail")
    public RestBean<Void> updateClientDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                              @RequestBody @Valid ClientDetailVO vo) {
        clientService.updateClientDetail(vo,client);
        return RestBean.success();
    }

    /**
     * 更新客户端运行时数据接口
     * 该接口用于接收并存储客户端的实时运行数据，如CPU使用率、内存使用情况、磁盘使用情况等。
     * 客户端会定期调用此接口上报最新的运行状态数据，系统会保存这些数据用于监控和分析。
     * 
     * @param client 当前客户端对象，通过请求属性注入，由拦截器根据客户端身份信息设置
     * @param vo 运行时详细数据对象，包含CPU、内存、磁盘等实时运行数据
     * @return 更新结果，成功返回RestBean.success()
     */
    @PostMapping("/runtime")
    public RestBean<Void> updateRuntimeDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                               @RequestBody @Valid RuntimeDetailVO vo) {
        clientService.updateRuntimeDetail(vo, client);
        return RestBean.success();
    }
}

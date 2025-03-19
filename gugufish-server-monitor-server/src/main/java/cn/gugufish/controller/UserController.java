package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.ChangePasswordVO;
import cn.gugufish.entity.vo.request.CreateSubAccountVO;
import cn.gugufish.entity.vo.request.ModifyEmailVO;
import cn.gugufish.entity.vo.request.SubAccountVO;
import cn.gugufish.service.AccountService;
import cn.gugufish.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    AccountService service;
    @Operation(summary = "密码重置操作")
    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestBody @Valid ChangePasswordVO vo,
                                         @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        return service.changePassword(userId, vo.getPassword(), vo.getNew_password()) ?
                RestBean.success() : RestBean.failure(401, "原密码输入错误！");
    }
    @Operation(summary = "邮箱重置操作")
    @PostMapping("/modify-email")
    public RestBean<Void> modifyEmail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ModifyEmailVO vo) {
        String result = service.modifyEmail(id, vo);
        if(result == null) {
            return RestBean.success();
        } else {
            return RestBean.failure(401, result);
        }
    }
    @Operation(summary = "子用户创建")
    @PostMapping("/sub/create")
    public RestBean<Void> createSubAccount(@RequestBody @Valid CreateSubAccountVO vo) {
        service.createSubAccount(vo);
        return RestBean.success();
    }
    @Operation(summary = "子用户删除")

    @GetMapping("/sub/delete")
    public RestBean<Void> deleteSubAccount(int uid,
                                           @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        if(uid == userId)
            return RestBean.failure(401, "非法参数");
        service.deleteSubAccount(uid);
        return RestBean.success();
    }
    @Operation(summary = "子用户列表")
    @GetMapping("/sub/list")
    public RestBean<List<SubAccountVO>> subAccountList() {
        return RestBean.success(service.listSubAccount());
    }
}
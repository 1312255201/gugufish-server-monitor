package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.ChangePasswordVO;
import cn.gugufish.service.AccountService;
import cn.gugufish.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    AccountService service;

    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestBody @Valid ChangePasswordVO vo,
                                         @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        return service.changePassword(userId, vo.getPassword(), vo.getNew_password()) ?
                RestBean.success() : RestBean.failure(401, "原密码输入错误！");
    }

}
package cn.gugufish.service;

import cn.gugufish.entity.vo.request.CreateSubAccountVO;
import cn.gugufish.entity.vo.request.SubAccountVO;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.gugufish.entity.dto.Account;
import cn.gugufish.entity.vo.request.ConfirmResetVO;
import cn.gugufish.entity.vo.request.EmailResetVO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface AccountService extends IService<Account>, UserDetailsService {
    Account findAccountByNameOrEmail(String text);
    String registerEmailVerifyCode(String type, String email, String address);
    String resetEmailAccountPassword(EmailResetVO info);
    String resetConfirm(ConfirmResetVO info);
    boolean changePassword(int id, String oldPass, String newPass);
    void createSubAccount(CreateSubAccountVO vo);
    void deleteSubAccount(int uid);
    List<SubAccountVO> listSubAccount();
}

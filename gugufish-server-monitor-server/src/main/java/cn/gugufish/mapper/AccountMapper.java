package cn.gugufish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.gugufish.entity.dto.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}

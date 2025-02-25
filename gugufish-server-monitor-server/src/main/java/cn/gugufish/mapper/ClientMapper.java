package cn.gugufish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.gugufish.entity.dto.Client;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientMapper extends BaseMapper<Client> {

}

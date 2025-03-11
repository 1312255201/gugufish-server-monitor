package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_client_ssh")
public class ClientSsh implements BaseData {
    @TableId
    Integer id;
    Integer port;
    String username;
    String password;
}
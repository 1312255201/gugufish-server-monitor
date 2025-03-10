package cn.gugufish.entity.vo.request;

import com.alibaba.fastjson2.JSONArray;
import lombok.Data;

@Data
public class SubAccountVO {
    int id;
    String username;
    String email;
    JSONArray clientList;
}
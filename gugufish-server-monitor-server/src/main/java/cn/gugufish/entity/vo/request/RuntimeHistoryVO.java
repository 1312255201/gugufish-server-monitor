package cn.gugufish.entity.vo.request;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class RuntimeHistoryVO {
    double disk;
    double memory;
    List<JSONObject> list = new LinkedList<>();
}
package cn.gugufish.entity;

import com.alibaba.fastjson2.JSONObject;

public record Response(Long id, int code, Object data , String message) {
    public boolean isSuccess(){
        return code==200;
    }
    public JSONObject asJson(){
        return JSONObject.from(data);
    }
    public String asString(){
        return data.toString();
    }
    public static Response errorResponse(Exception e){
        return new Response(0L,500,null,e.getMessage());
    }
}

package cn.gugufish.entity.vo.request;

import lombok.Data;

@Data
public class ClientDetailsVO {
    int id;
    String name;
    boolean online;
    String node;
    String location;
    String ip;
    String cpuName;
    String osName;
    String osVersion;
    double memory;
    int cpuCore;
    double disk;
}
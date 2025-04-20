package cn.gugufish.util;

import cn.gugufish.entity.BaseDetail;
import cn.gugufish.entity.ConnectionConfig;
import cn.gugufish.entity.RuntimeDetail;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.*;

/**
 * 系统监控工具类
 * 该工具类用于获取系统的各项硬件和运行时信息，包括：
 * 1. 基本系统信息：操作系统类型、版本、CPU信息、内存大小、磁盘容量等
 * 2. 运行时状态：CPU使用率、内存使用情况、磁盘使用情况、网络流量等
 * 该类使用OSHI库获取底层系统信息，支持跨平台监控。
 */
@Slf4j
@Component
public class MonitorUtils {
    /**
     * 连接配置，用于获取网络接口信息
     * 使用懒加载避免循环依赖
     */
    @Lazy
    @Resource
    ConnectionConfig config;
    
    /**
     * 系统信息对象，用于获取硬件和操作系统信息
     */
    private final SystemInfo systemInfo = new SystemInfo();
    
    /**
     * 系统属性，用于获取操作系统架构等信息
     */
    private final Properties properties = System.getProperties();

    /**
     * 获取系统基本信息
     * 收集系统的静态信息，包括：
     * - 操作系统类型、版本、架构和位数
     * - CPU名称和核心数
     * - 内存总容量（GB）
     * - 磁盘总容量（GB）
     * - 系统IP地址
     *
     * @return 包含系统基本信息的BaseDetail对象
     */
    public BaseDetail monitorBaseDetail(){
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        double memory = hardware.getMemory().getTotal() / 1024.0 / 1024 / 1024;
        double diskSize = Arrays.stream(File.listRoots()).mapToLong(File::getTotalSpace).sum() / 1024.0 / 1024 / 1024;
        String ip = Objects.requireNonNull(this.findNetworkInterface(hardware)).getIPv4addr()[0];
        return new BaseDetail()
                .setOsArch(properties.getProperty("os.arch"))
                .setOsName(operatingSystem.getFamily())
                .setOsVersion(operatingSystem.getVersionInfo().getVersion())
                .setOsBit(operatingSystem.getBitness())
                .setCpuName(hardware.getProcessor().getProcessorIdentifier().getName())
                .setCpuCore(hardware.getProcessor().getLogicalProcessorCount())
                .setMemory(memory)
                .setDisk(diskSize)
                .setIp(ip);

    }
    /**
     * 监控系统运行时状态
     * 收集系统的动态运行信息，包括：
     * - CPU使用率
     * - 内存使用量（GB）
     * - 磁盘使用量（GB）
     * - 网络上传和下载速率（KB/s）
     * - 磁盘读写速率（MB/s）
     * - 当前时间戳
     * 该方法会在指定的统计时间内（默认1秒）采集两次数据，计算资源使用变化率
     *
     * @return 包含系统运行时状态的RuntimeDetail对象，如果获取失败则返回null
     */
    public RuntimeDetail monitorRuntimeDetail() {
        double statisticTime = 1;
        try {
            HardwareAbstractionLayer hardware = systemInfo.getHardware();
            NetworkIF networkInterface = Objects.requireNonNull(this.findNetworkInterface(hardware));
            CentralProcessor processor = hardware.getProcessor();
            double upload = networkInterface.getBytesSent(), download = networkInterface.getBytesRecv();
            double read = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum();
            double write = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum();
            long[] ticks = processor.getSystemCpuLoadTicks();
            Thread.sleep((long) (statisticTime * 1000));
            networkInterface = Objects.requireNonNull(this.findNetworkInterface(hardware));
            upload = (networkInterface.getBytesSent() - upload) / statisticTime;
            download =  (networkInterface.getBytesRecv() - download) / statisticTime;
            read = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum() - read) / statisticTime;
            write = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum() - write) / statisticTime;
            double memory = (hardware.getMemory().getTotal() - hardware.getMemory().getAvailable()) / 1024.0 / 1024 / 1024;
            double disk = Arrays.stream(File.listRoots())
                    .mapToLong(file -> file.getTotalSpace() - file.getFreeSpace()).sum() / 1024.0 / 1024 / 1024;
            return new RuntimeDetail()
                    .setCpuUsage(this.calculateCpuUsage(processor, ticks))
                    .setMemoryUsage(memory)
                    .setDiskUsage(disk)
                    .setNetworkUpload(upload / 1024)
                    .setNetworkDownload(download / 1024)
                    .setDiskRead(read / 1024/ 1024)
                    .setDiskWrite(write / 1024 / 1024)
                    .setTimestamp(new Date().getTime());
        } catch (Exception e) {
            log.error("读取运行时数据出现问题", e);
        }
        return null;
    }

    /**
     * 计算CPU使用率
     * 根据两次采集的CPU时钟周期差值计算CPU使用率
     * 计算公式：(系统时间 + 用户时间) / 总时间
     *
     * @param processor CPU处理器对象
     * @param prevTicks 上一次采集的CPU时钟周期数组
     * @return CPU使用率，范围0-1之间的小数
     */
    private double calculateCpuUsage(CentralProcessor processor, long[] prevTicks) {
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
                - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softIrq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
                - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long cUser = ticks[CentralProcessor.TickType.USER.getIndex()]
                - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long ioWait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = cUser + nice + cSys + idle + ioWait + irq + softIrq + steal;
        return (cSys + cUser) * 1.0 / totalCpu;
    }
    /**
     * 获取系统所有网络接口名称列表
     * 用于在系统初始化时让用户选择要监控的网络接口
     *
     * @return 网络接口名称列表
     */
    public List<String> listNetworkInterfaceName() {
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        return hardware.getNetworkIFs()
                .stream()
                .map(NetworkIF::getName)
                .toList();
    }

    /**
     * 查找指定的网络接口
     * 根据配置中的网络接口名称，从系统中查找对应的网络接口对象
     * 如果找不到指定的网络接口，将抛出IOException异常
     *
     * @param hardware 硬件抽象层对象
     * @return 找到的网络接口对象，如果未找到则返回null
     */
    private NetworkIF findNetworkInterface(HardwareAbstractionLayer hardware) {
        try{
            // 从配置中获取目标网络接口名称
            String target = config.getNetworkInterface();
            // 过滤出匹配名称的网络接口
            List<NetworkIF> ifs = hardware.getNetworkIFs()
                    .stream()
                    .filter(inter -> inter.getName().equals(target))
                    .toList();
            // 如果找到匹配的网络接口，返回第一个
            if (!ifs.isEmpty()) {
                return ifs.get(0);
            } else {
                // 未找到匹配的网络接口，抛出异常
                throw new IOException("网卡信息错误，找不到网卡: " + target);
            }
        }
        catch (IOException e){
            // 记录网络信息获取失败的错误日志
            log.error("获取网络信息失败",e);
        }
        // 发生异常时返回null
        return null;
    }
}

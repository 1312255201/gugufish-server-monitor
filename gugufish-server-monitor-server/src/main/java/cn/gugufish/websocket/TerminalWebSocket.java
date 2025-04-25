package cn.gugufish.websocket;

import cn.gugufish.entity.dto.ClientDetail;
import cn.gugufish.entity.dto.ClientSsh;
import cn.gugufish.mapper.ClientDetailMapper;
import cn.gugufish.mapper.ClientSshMapper;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import jakarta.annotation.Resource;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于处理与终端的WebSocket连接，
 * 实现与客户端的SSH连接和交互功能。
 * 该类使用Spring组件注解进行管理，并通过WebSocket服务端点注解定义访问路径。
 */
@Slf4j
@Component
@ServerEndpoint("/terminal/{clientId}")
public class TerminalWebSocket {
    /**
     * 静态的客户端详情数据映射器，用于从数据库中查询客户端详情信息。
     */
    private static ClientDetailMapper detailMapper;
    /**
     * 注入客户端详情数据映射器到静态变量中。
     * 由于WebSocket端点不是Spring管理的Bean，使用静态变量和setter方法注入依赖。
     *
     * @param detailMapper 客户端详情数据映射器实例
     */
    @Resource
    public void setDetailMapper(ClientDetailMapper detailMapper) {
        TerminalWebSocket.detailMapper = detailMapper;
    }
    /**
     * 静态的客户端SSH配置数据映射器，用于从数据库中查询客户端SSH配置信息。
     */
    private static ClientSshMapper sshMapper;
    /**
     * 注入客户端SSH配置数据映射器到静态变量中。
     * 由于WebSocket端点不是Spring管理的Bean，使用静态变量和setter方法注入依赖。
     *
     * @param sshMapper 客户端SSH配置数据映射器实例
     */
    @Resource
    public void setSshMapper(ClientSshMapper sshMapper) {
        TerminalWebSocket.sshMapper = sshMapper;
    }
    /**
     * 静态映射，用于存储WebSocket会话和对应的Shell实例。
     * 键为WebSocket会话，值为对应的Shell实例，方便管理和查找。
     */
    private static final Map<Session, Shell> sessionMap = new ConcurrentHashMap<>();
    /**
     * 单线程执行器，用于异步执行读取SSH输入流的任务。
     */
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    /**
     * 当WebSocket连接建立时触发的方法。
     * 尝试从数据库中获取客户端详情和SSH配置信息，若信息存在则创建SSH连接。
     *
     * @param session 建立的WebSocket会话
     * @param clientId 客户端ID，从URL路径参数中获取
     * @throws Exception 可能抛出的异常，包括数据库查询异常和SSH连接异常
     */
    @OnOpen
    public void onOpen(Session session,
                       @PathParam(value = "clientId") String clientId) throws Exception {
        // 从数据库中查询客户端详情信息
        ClientDetail detail = detailMapper.selectById(clientId);
        // 从数据库中查询客户端SSH配置信息
        ClientSsh ssh = sshMapper.selectById(clientId);
        // 若客户端详情或SSH配置信息不存在，则关闭WebSocket连接
        if(detail == null || ssh == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "无法识别此主机"));
            return;
        }
        // 尝试创建SSH连接，若成功则记录日志
        if(this.createSshConnection(session, ssh, detail.getIp())) {
            log.info("主机 {} 的SSH连接已创建", detail.getIp());
        }
    }
    /**
     * 当接收到WebSocket消息时触发的方法。
     * 将接收到的消息发送到对应的SSH连接中。
     *
     * @param session 发送消息的WebSocket会话
     * @param message 接收到的消息
     * @throws IOException 可能的输入输出异常
     */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        Shell shell = sessionMap.get(session);
        OutputStream output = shell.output;
        output.write(message.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
    /**
     * 当WebSocket连接关闭时触发的方法。
     * 关闭对应的SSH连接并从会话映射中移除该会话。
     *
     * @param session 关闭的WebSocket会话
     * @throws IOException 可能的输入输出异常
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        Shell shell = sessionMap.get(session);
        if(shell != null) {
            shell.close();
            sessionMap.remove(session);
            log.info("主机 {} 的SSH连接已断开", shell.js.getHost());
        }
    }
    /**
     * 当WebSocket连接出现错误时触发的方法。
     * 记录错误日志并关闭WebSocket连接。
     *
     * @param session 出现错误的WebSocket会话
     * @param error 抛出的异常
     * @throws IOException 可能的输入输出异常
     */
    @OnError
    public void onError(Session session, Throwable error) throws IOException {
        log.error("用户WebSocket连接出现错误", error);
        session.close();
    }
    /**
     * 创建SSH连接的私有方法。
     * 使用JSch库创建SSH会话和Shell通道，并将其与WebSocket会话关联。
     *
     * @param session 对应的WebSocket会话
     * @param ssh 客户端SSH配置信息
     * @param ip 客户端IP地址
     * @return 若SSH连接创建成功返回true，否则返回false
     * @throws IOException 可能的输入输出异常
     */
    private boolean createSshConnection(Session session, ClientSsh ssh, String ip) throws IOException{
        try {
            // 创建JSch实例
            JSch jSch = new JSch();
            //创建SSH会话
            com.jcraft.jsch.Session js = jSch.getSession(ssh.getUsername(), ip, ssh.getPort());
            //设置SSH会话账号密码
            js.setPassword(ssh.getPassword());
            //禁用严格密钥检查
            js.setConfig("StrictHostKeyChecking", "no");
            //设置超时时间
            js.setTimeout(3000);
            //连接SSH服务器
            js.connect();
            //打开shh通道
            ChannelShell channel = (ChannelShell) js.openChannel("shell");
            // 设置终端类型为xterm
            channel.setPtyType("xterm");
            //链接到shell通道
            channel.connect(1000);
            //将实例存储起来
            sessionMap.put(session, new Shell(session, js, channel));
            return true;
        } catch (JSchException e) {
            String message = e.getMessage();
            if(message.equals("Auth fail")) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        "登录SSH失败，用户名或密码错误"));
                log.error("连接SSH失败，用户名或密码错误，登录失败");
            } else if(message.contains("Connection refused")) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        "连接被拒绝，可能是没有启动SSH服务或是放开端口"));
                log.error("连接SSH失败，连接被拒绝，可能是没有启动SSH服务或是放开端口");
            } else {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, message));
                log.error("连接SSH时出现错误", e);
            }
        }
        return false;
    }
    /**
     * 内部类，用于管理SSH连接的相关资源，包括输入输出流、SSH会话和Shell通道。
     */
    private class Shell {
        private final Session session;
        private final com.jcraft.jsch.Session js;
        private final ChannelShell channel;
        private final InputStream input;
        private final OutputStream output;

        public Shell(Session session, com.jcraft.jsch.Session js, ChannelShell channel) throws IOException {
            this.js = js;
            this.session = session;
            this.channel = channel;
            this.input = channel.getInputStream();
            this.output = channel.getOutputStream();
            service.submit(this::read);
        }

        private void read() {
            try {
                byte[] buffer = new byte[1024 * 1024];
                int i;
                while ((i = input.read(buffer)) != -1) {
                    String text = new String(Arrays.copyOfRange(buffer, 0, i), StandardCharsets.UTF_8);
                    session.getBasicRemote().sendText(text);
                }
            } catch (Exception e) {
                log.error("读取SSH输入流时出现问题", e);
            }
        }

        public void close() throws IOException {
            input.close();
            output.close();
            channel.disconnect();
            js.disconnect();
            service.shutdown();
        }
    }
}
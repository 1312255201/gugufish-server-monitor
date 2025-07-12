package cn.gugufish.listener;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用于处理邮件发送的消息队列监听器
 */
@Component
@Slf4j
@RabbitListener(queues = "mail")
public class MailQueueListener {

    @Resource
    JavaMailSender sender;

    @Value("${spring.mail.username}")
    String username;

    /**
     * 处理邮件发送
     * @param data 邮件信息
     */
    @RabbitHandler
    public void sendMailMessage(Map<String, Object> data) {
        try {
            if (data == null || !data.containsKey("email") || !data.containsKey("type")) {
                log.error("邮件数据格式不正确: {}", data);
                throw new AmqpRejectAndDontRequeueException("邮件数据格式不正确");
            }

            String email = data.get("email").toString();
            Integer code = data.containsKey("code") ? (Integer) data.get("code") : null;
            String ip = data.containsKey("ip") ? (String) data.get("ip") : null;

            SimpleMailMessage message = switch (data.get("type").toString()) {
                case "reset" ->
                        createMessage("您的密码重置邮件",
                                "你好，您正在执行重置密码操作，验证码: "+code+"，有效时间3分钟，如非本人操作，请无视。",
                                email);
                case "modify" ->
                        createMessage("您的邮件修改验证邮件",
                                "您好，您正在绑定新的电子邮件地址，验证码: "+code+"，有效时间3分钟，如非本人操作，请无视",
                                email);
                case "logininfo" ->
                        createMessage("登录提示，你的监控系统账户被登录",
                                    "你的账户被登录，登录远程IP为" + ip+
                                "如非本人操作，请注意你的账户安全",
                                email);
                case "loginwarn" ->
                        createMessage("登录提示，你的监控系统账户可能遭受暴力破解",
                                "你的账户被多次尝试登录，登录远程IP为" + ip+
                                "如非本人操作，请注意你的账户安全,你的账号多次被尝试登录",
                                email);
                default -> null;
            };

            if(message == null) {
                log.warn("未知的邮件类型: {}", data.get("type"));
                throw new AmqpRejectAndDontRequeueException("未知的邮件类型");
            }

            sender.send(message);
            log.info("成功发送邮件到: {}, 类型: {}", email, data.get("type"));
        } catch (MailException e) {
            log.error("邮件发送失败: {}", e.getMessage());
            // 抛出这个异常会告诉RabbitMQ不要重新入队这条消息
            throw new AmqpRejectAndDontRequeueException("邮件发送失败", e);
        } catch (Exception e) {
            log.error("处理邮件消息时发生错误: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("处理邮件消息时发生错误", e);
        }
    }

    /**
     * 快速封装简单邮件消息实体
     * @param title 标题
     * @param content 内容
     * @param email 收件人
     * @return 邮件实体
     */
    private SimpleMailMessage createMessage(String title, String content, String email){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);
        message.setText(content);
        message.setTo(email);
        message.setFrom(username);
        return message;
    }
}

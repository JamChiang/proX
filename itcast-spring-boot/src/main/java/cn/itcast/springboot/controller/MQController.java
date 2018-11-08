package cn.itcast.springboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/mq")
@RestController
public class MQController {

    @Autowired
    JmsMessagingTemplate jmsMessagingTemplate;

    @GetMapping("/send")
    public String sendMapMsg(){

        Map<String, Object> map = new HashMap<>();
        map.put("name", "i_am_ljb");
        map.put("address", "吉山村");

        //参数1：队列的名称 参数2：要发送的消息
        jmsMessagingTemplate.convertAndSend("spring.boot.map.queue", map);

        return "成功发送到spring.boot.map.queue队列中。";
    }

    @GetMapping("/sendmq")
    public String sendMQMsg(){

        Map<String, Object> map = new HashMap<>();
        map.put("mobile", "18575751138");
        map.put("signName", "黑马");
        map.put("templateCode", "SMS_125018593");
        map.put("templateParam", "{\"code\":654321}");

        //参数1：队列的名称 参数2：要发送的消息
        jmsMessagingTemplate.convertAndSend("itcast_sms_queue", map);

        return "成功发送到itcast_sms_queue队列中。";
    }
}

package cn.itcast.springboot.activemq.listener;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MessageListener {

    /**
     * 接收对应队列的消息；
     * destination 队列的名称
     * @param map 具体的接收到的消息
     */
    @JmsListener(destination = "spring.boot.map.queue")
    public void receiveMsg(Map<String, Object> map){
        System.out.println(map);
    }
}

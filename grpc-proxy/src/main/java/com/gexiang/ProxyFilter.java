package com.gexiang;

import com.gexiang.core.GrpcConManger;
import com.gexiang.vo.ConstValues;
import com.gexiang.vo.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 *
 */
@Component
@Order(-10)
public class ProxyFilter implements WebFilter {
    private final static Pattern pattern = Pattern.compile(
            "(2[5][0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})");
    private static Logger logger = LoggerFactory.getLogger(ProxyFilter.class);
    private static AtomicLong SessionId = new AtomicLong(0L);
    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        serverWebExchange.getRequest().getHeaders();
        GrpcConManger.getInstance().addSession(new SessionInfo(SessionId.getAndIncrement(), getIp(serverWebExchange)));
        return webFilterChain.filter(serverWebExchange);
    }

    private static String getIp(ServerWebExchange exchange){
        String ip;
        ip = getIpFromHead(exchange.getRequest().getHeaders());
        if(ip != null){
            logger.debug("IP:{}", ip);
        }

        ip = getValIp(exchange.getRequest().getHeaders().get(HttpHeaders.FROM));
        if(ip != null){
            logger.debug("IP:{}", ip);
            return ip;
        }

        if(exchange.getRequest().getRemoteAddress() != null){
            ip = exchange.getRequest().getRemoteAddress().getHostString();
            logger.debug("IP:{}", ip);
            return ip;
        }

        return ConstValues.IG_IP;
    }

    private static String getIpFromHead(HttpHeaders headers){
        String[] headNames = new String[]{"X-Real-IP", "X-Forwarded-For", "Proxy-Client-IP",
                                          "WL-Proxy-Client-IP","HTTP_CLIENT_IP","HTTP_X_FORWARDED_FOR"};
        for (String n:headNames){
            List<String> list = headers.get(n);
            String ip = getValIp(list);
            if(ip != null){
                return ip;
            }
        }

        return null;
    }

    private static String getValIp(List<String> list){
        if(list == null){
            return null;
        }

        for(String ip:list){
            if(isValidIP(ip)){
                return ip;
            }
        }
        return null;
    }

    private static boolean isValidIP(String ip){
        if((ip == null) || ip.isEmpty()){
            return false;
        }

        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}

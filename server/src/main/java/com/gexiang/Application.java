package com.gexiang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
public class Application {
    private static volatile boolean stop = false;
    private static Logger logger = LoggerFactory.getLogger(Application.class);
    public static void main( String[] args ) {
        try {
            logger.info("Hello grpc gate!");
            SpringApplication.run(Application.class, args);
            Thread daemon = new Thread(()->{
                while (!stop){
                    try {
                        Thread.sleep(3000L);
                    }catch (Throwable t){

                    }
                }
            });
            daemon.setName("application.daemon");
            daemon.start();
        }catch (Throwable t){
            t.printStackTrace();
        }
    }

    @PostConstruct
    public void init(){
        logger.info("Post construct ......");
    }

    @PreDestroy
    public void  dostory(){
        logger.info("Post dostory ......");
        stop = true;
    }
}

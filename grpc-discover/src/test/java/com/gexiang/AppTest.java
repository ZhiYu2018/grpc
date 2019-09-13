package com.gexiang;

import com.gexiang.core.EtcdData;
import com.gexiang.util.GrpcBackOff;
import com.gexiang.util.Helper;
import io.etcd.jetcd.KeyValue;
import javafx.util.Pair;
import org.junit.Test;

import java.net.Inet4Address;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

public class AppTest {
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }
    @Test
    public void testDiscover(){
        EtcdData etcdData = new EtcdData("http://localhost:2379;http://localhost:3379;http://localhost:4379");
//        for(int i = 0; i < 100000; i++) {
//            etcdData.put("abc" + i, String.valueOf(i));
//        }
        System.out.println("Get:" + etcdData.get("abc"));
        List<KeyValue> kvs = etcdData.listPreKey("abc");
        System.out.println("Kvs size:" + kvs.size());
        //LeaseGrantResponse lrsp = etcdData.getLeaseId();
        //System.out.println("LeaseId:" + lrsp.getID());
    }

    @Test
    public void testFunc(){
        Pair<String,String> kp = Helper.getServerKeyData("grpc.io/com.server/127.0.0.1:8080/v1.0");
        System.out.println("s:" + kp.getKey() + "," + kp.getValue());
    }

    @Test
    public void testGetIp(){
        Optional<Inet4Address> opt = Helper.getLocalIp4Address();
        System.out.println("ip:" + opt.get().getHostAddress());
    }

    @Test
    public void testBackOff(){
        GrpcBackOff backOff = new GrpcBackOff();
        Consumer<String> f = System.out::println;
        for(int i = 1; i < 11; i++){
            f.accept(i + ":" + backOff.nextBackOffMillis());
        }
    }
}

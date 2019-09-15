# grpc & 
## Rest to grpc proxy
* REST API 转grpc 服务，利用http2 达到连接复用，在接入层消化连接。
* 同时利用etcd 达到负载均衡 + 服务发现.
* 基于WebFlux 和grpc 异步特征 达到事件驱动，从而提高吞吐量，减少线程。
## 使用方法
* grpc 服务，在启动服务的时候，要支持addService(ProtoReflectionService.newInstance())，具体参考server。
* 配置：etcd.host=http://localhost:2379;http://localhost:3379;http://localhost:4379
* 服务提供者在成功启动之后，要注册服务利用GrpcRegister.grpcRegister().register(GrpcGateGrpc.SERVICE_NAME, String.valueOf(p), "v1.0");
## 版本依赖
* grpc 版本 1.23.0
* jetcd 版本 > 0.3.0，建议 下载这里的jetcd.

## REST 请求格式

* POST \ Get 到ip:8080/grpc/route，端口可以自己修改

* http 头部：X-Grpc-Method，比如com.gexiang.grpc.GrpcGate/Route;X-Grpc-Ver 接口版本号，比如v1.0

* 请求格式目前只支持JSON

* 返回格式是json 建议类似 `{String.format("{\"code\":\"%d\",\"msg\":\"%s\"}", code, msg)}`，code 200 成功，其他失败。其他字段自己定义。

  

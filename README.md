# grpc
## Rest to grpc proxy
* REST API 转grpc 服务，利用http2 达到连接服用
* 同时利用etcd 达到负载均衡 + 服务发现.
* 基于WebFlux 和grpc 异步特征 达到事件驱动，从而提高吞吐量，减少线程。
## 使用方法
* grpc 服务，在启动服务的时候，要支持addService(ProtoReflectionService.newInstance())，具体参考server。
* 配置：etcd.host=http://localhost:2379;http://localhost:3379;http://localhost:4379
* 服务提供者在成功启动之后，要注册服务利用GrpcRegister.grpcRegister().register(GrpcGateGrpc.SERVICE_NAME, String.valueOf(p), "v1.0");


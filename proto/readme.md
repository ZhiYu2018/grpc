..\bin\protoc.exe grpc_gate.proto --java_out=..\..\server\src\main\java

..\bin\protoc.exe --plugin=protoc-gen-grpc-java=..\bin\gen-grpc-java.exe –-grpc-java_out=..\..\server\src\main\java ./grpc_gate.proto
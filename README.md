# simple-rpc

一个简单的Rpc框架。

服务端代码样例：
<pre><code>
RpcServer rpcServer = RpcServer.builder()
    .port(8998)
    .zoo("127.0.0.1:2181")
    .bossThread(1)
    .workThread(4)
    .addListener(new ServerInvocationListener() {
        @Override
        public Object around(MethodInvocationPoint point) throws Throwable {
            // to do something as framework level without any business.
            // to add monitor.
            return point.proceed();
        }
    })
    .build();
rpcServer.start(); //启动服务
rpcServer.register(HelloWordService.class, new HelloWordServiceImpl()); //注册服务
</code></pre>

关闭服务
<pre><code>
rpcServer.shutdown();
</code></pre>

客户端代码样例：
<pre><code>
RpcClient rpcClient = RpcClient.builder()
    .zoo("127.0.0.1:2181")
    .timeout(3000)
    .addListener(new ClientInvocationListener() {
        @Override
        public Object around(MethodInvocationPoint point) throws Throwable {
            // to do something as framework level without any business.
            // to add monitor.
            return point.proceed();
        }
    })
    .build();
HelloWordService helloWordService = rpcClient.proxy(HelloWordService.class); //代理服务
helloWordService.sayHello(); //调用远程服务
</code></pre>

关闭客户端
<pre><code>
rpcClient.close();
</code></pre>
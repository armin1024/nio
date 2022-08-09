package cn.arminxss.msb.rpcdemo.proxy;
import cn.arminxss.msb.rpcdemo.Dispatcher;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyContent;
import cn.arminxss.msb.rpcdemo.rpc.transport.ClientFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

public class MyProxy {

    public static <T>T proxyGet(Class<T> interfaceInfo) {
        ClassLoader loader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};
        return (T) Proxy.newProxyInstance(loader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Dispatcher dispatcher = Dispatcher.getInstance();
                String name = interfaceInfo.getName(); // 可根据name（接口名称）获取到实现该接口类型的实现类
                Object o = dispatcher.get(name);
                Object res;
                if (o == null) {
                    // rpc，走代理
                    String methodName = method.getName();
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    MyContent content = new MyContent();
                    content.setName(name);
                    content.setMethodName(methodName);
                    content.setParameterTypes(parameterTypes);
                    content.setArgs(args);
                    CompletableFuture<Object> transport = ClientFactory.transport(content);
                    res = transport.get(); // 阻塞的
                } else {
                    // 本地调用
                    // 走代理多了一些插入插件的机会，做一些扩展
                    System.out.println("local FC...");
                    Class<?> aClass = o.getClass();
                    Method m = aClass.getMethod(method.getName(), method.getParameterTypes());
                    res = m.invoke(o, args);
                }
                return res;
            }
        });
    }

}

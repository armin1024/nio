package cn.arminxss.msb.rpcdemo;

import java.util.concurrent.ConcurrentHashMap;

public class Dispatcher {

    private static Dispatcher dispatcher = new Dispatcher();

    static ConcurrentHashMap<String, Object> invokeMap = new ConcurrentHashMap<>();

    private Dispatcher() {

    }

    public static Dispatcher getInstance() {
        return dispatcher;
    }

    public void register(String key, Object val) {
        invokeMap.put(key, val);
    }

    public Object get(String key) {
        return invokeMap.get(key);
    }
}
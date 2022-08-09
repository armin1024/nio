package cn.arminxss.msb.rpcdemo.rpc;

import cn.arminxss.msb.rpcdemo.util.PackageMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ResCallback {
    static ConcurrentHashMap<Long, CompletableFuture<Object>> mapping = new ConcurrentHashMap<>();

    public static void addCallBack(long requestId, CompletableFuture<Object> completableFuture) {
        mapping.putIfAbsent(requestId, completableFuture);
    }

    public static void runCallBack(PackageMessage message) {
        CompletableFuture<Object> res = mapping.get(message.getHeader().getRequestId());
        res.complete(message.getContent().getRes());
        remove(message.getHeader().getRequestId());
    }

    private static void remove(long requestId) {
        mapping.remove(requestId);
    }

}
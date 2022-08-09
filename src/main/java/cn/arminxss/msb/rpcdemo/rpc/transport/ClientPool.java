package cn.arminxss.msb.rpcdemo.rpc.transport;

import io.netty.channel.socket.nio.NioSocketChannel;

public class ClientPool {
    NioSocketChannel[] clients;
    Object[] lock;

    ClientPool(int size) {
        clients = new NioSocketChannel[size]; // init 链接是空的
        lock = new Object[size];
        for (int i = 0; i < lock.length; i++) {
            lock[i] = new Object();
        }
    }

}
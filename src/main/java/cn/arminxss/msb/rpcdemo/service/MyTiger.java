package cn.arminxss.msb.rpcdemo.service;

public class MyTiger implements Tiger {
    @Override
    public void eat(String food) {
        System.out.println("[Tiger]server get client arg:" + food);
    }
}

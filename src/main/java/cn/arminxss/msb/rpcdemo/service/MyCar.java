package cn.arminxss.msb.rpcdemo.service;

public class MyCar implements Car {

    @Override
    public String drive(String msg) {
        System.out.println("[Car]server get client arg:" + msg);
        return "server res:" + msg;
    }

    @Override
    public Person driver(String name, Integer age) {
        Person person = new Person();
        person.setAge(age);
        person.setName(name);
        return person;
    }
}

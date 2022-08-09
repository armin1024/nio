package cn.arminxss.msb.rpcdemo.rpc.transport;

import cn.arminxss.msb.rpcdemo.Dispatcher;
import cn.arminxss.msb.rpcdemo.rpc.protocal.MyContent;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MyHttpRpcHandler extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            ServletInputStream in = req.getInputStream();
            ObjectInputStream oin = new ObjectInputStream(in);
            try {
                MyContent myContent = (MyContent) oin.readObject();
                String name = myContent.getName();
                String methodName = myContent.getMethodName();
                Object car = Dispatcher.getInstance().get(name);
                Class<?> aClass = car.getClass();
                Method method = aClass.getMethod(methodName, myContent.getParameterTypes());
                Object res = method.invoke(car, myContent.getArgs());

                MyContent resContent = new MyContent();
                resContent.setRes(res);

                ServletOutputStream out = resp.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);
                oout.writeObject(resContent);

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
    }
package rpc.lizhi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

import static sun.jvm.hotspot.runtime.PerfMemory.start;

/**
 * Created by user on 16/7/14.
 */
public class RpcFramework {

    public static void export(final Object service, int port){
        if(service == null)
            throw  new IllegalArgumentException("service is null");

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                final Socket socket = serverSocket.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ObjectInputStream inputStream = null;
                        try {
                            inputStream = new ObjectInputStream(socket.getInputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String methodName = null;
                        try {
                            methodName = inputStream.readUTF();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            Class<?> [] paramateTyeps = (Class<?>[]) inputStream.readObject();
                            Object [] args = (Object[]) inputStream.readObject();
                            try {
                                Method method = service.getClass().getMethod(methodName,paramateTyeps);
                                Object result = method.invoke(service,args);
                                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                                outputStream.writeObject(result);
                            } catch (NoSuchMethodException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }

                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } 
    } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static <T> T refer(final Class<T> intefaceClass,final String host,final int port){

        if(intefaceClass == null)
            throw new IllegalArgumentException("interfaceClass must not be null.");


        return  (T) java.lang.reflect.Proxy.newProxyInstance(intefaceClass.getClassLoader(), new Class<?>[]{intefaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                try {
                    Socket socket = new  Socket(host,port);
                        try{
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                            try {
                                outputStream.writeUTF(method.getName());
                                outputStream.writeObject(method.getParameterTypes());
                                outputStream.writeObject(args);

                                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                                try {
                                    Object result = inputStream.readObject();
                                    if (result instanceof Throwable) {
                                        return (Throwable) result;
                                    }
                                    return result;
                                }catch (Throwable t){
                                    return t;
                                }finally {
                                    inputStream.close();
                                }
                            }catch (IOException e){

                            }finally {
                                outputStream.close();
                            }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                            socket.close();
                        }
                } catch (IOException e){

                }
                return null;
            }
        });
    }

}


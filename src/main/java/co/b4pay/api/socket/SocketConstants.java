package co.b4pay.api.socket;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * socket 相关常量类
 * Created with IntelliJ IDEA
 * Created By AZain
 * Date: 2018-09-06
 * Time: 11:01
 */
public class SocketConstants {

    //public static Map<SocketAddress,Socket> SOCKET_MAP = new HashMap<>();
    public static Map<SocketAddress, Socket> SOCKET_MAP = new HashMap<>();

    public final static Integer CODE_SOCKET_PORT = 1314;//端口

    public final static Integer GRService_SOCKET_PORT = 8888;//端口

    public final static Integer SOCKET_BUFFER_SIZE = 1024;//字节流长度

    public final static Integer SOCKET_THREAD_SELLP_TIME = 10;//线程休眠时间


}

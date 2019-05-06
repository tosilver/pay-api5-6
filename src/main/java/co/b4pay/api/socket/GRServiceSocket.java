package co.b4pay.api.socket;


import co.b4pay.api.service.CodePayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static co.b4pay.api.socket.SocketConstants.GRService_SOCKET_PORT;

/**
 * @author Tosdom
 */
@Component
public class GRServiceSocket {

    private static final Logger logger = LoggerFactory.getLogger(GRServiceSocket.class);

    private static GRServiceSocket grServiceSocket;

    @PostConstruct
    public void init() {
        grServiceSocket = this;
    }

    @Autowired
    private CodePayService codePayService;

    //socket状态
    private volatile boolean running = false;

    //监听狗
    private Thread connWatchDog;


    /**
     * 将web服务器的状态改为运行，创建监听狗线程并启动
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        connWatchDog = new Thread(new ConnWatchDog());
        connWatchDog.start();
    }

    /**
     * 将web服务器的状态改为关闭，并且关闭监听狗线程
     */
    @SuppressWarnings("deprecation")
    public void stop() {
        if (running) {
            running = false;
        }
        if (connWatchDog != null) {
            connWatchDog.stop();
        }
    }

    class ConnWatchDog implements Runnable {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(GRService_SOCKET_PORT);
                logger.warn(String.format("Socket创建，开始侦听，端口号:%s", GRService_SOCKET_PORT));
                while (running) {
                    Socket socket = serverSocket.accept();
                    SocketConstants.SOCKET_MAP.put(socket.getRemoteSocketAddress(), socket);
                    logger.warn(String.format("[%s]Socket连接成功，端口号:%s", socket.getRemoteSocketAddress(), GRService_SOCKET_PORT));
                    logger.warn(String.format("add socket，socket总个数:%s ", SocketConstants.SOCKET_MAP.size()));
                    new Thread(new SocketAction(socket)).start();
                }
            } catch (IOException e) {
                GRServiceSocket.this.stop();
                logger.error(String.format("[%s]Socket创建失败,失败原因:%s", GRService_SOCKET_PORT, e.getMessage()));
            }

        }
    }


    //------------------------------ socketAction ----------------------
    class SocketAction implements Runnable {

        Socket socket;

        SocketUtil socketUtil = null;

        boolean run = true;

        public SocketAction(Socket socket) {
            this.socket = socket;
            socketUtil = new SocketUtil(socket);
        }

        @Override
        public void run() {
            //web服务器在运行,并且本线程也在运行中
            while (running && run) {
                try {
                    String test = socketUtil.receiveData();
                    logger.warn(String.format("从[%s]接收到的数据：%s", socket.getRemoteSocketAddress(), test));
                    String data = grServiceSocket.codePayService.fixedCodePayNotify(socket, test);
                    logger.warn(String.format("给[%s]返回的数据：%s", socket.getRemoteSocketAddress(), data));
                    socketUtil.sendData(data);
                } catch (Exception e) {
                    overSocketAction();
                    logger.error(String.format("[%s]Socket通讯数据读取失败,失败原因:%s", GRService_SOCKET_PORT, e.getMessage()));
                }
            }
        }

        //关闭方法
        private void overSocketAction() {
            if (run) {
                run = false;
            }
            if (socket != null) {
                try {
                    SocketConstants.SOCKET_MAP.remove(socket.getRemoteSocketAddress());
                    socket.close();
                    logger.error(String.format("[%s]Socket关闭,IP:%s", GRService_SOCKET_PORT, socket.getRemoteSocketAddress()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}

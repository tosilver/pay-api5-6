package co.b4pay.api.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static co.b4pay.api.socket.SocketConstants.*;

/**
 * Socket工具类
 * Created with IntelliJ IDEA
 * Created By AZain
 * Date: 2018-09-06
 * Time: 11:19
 */
public class SocketUtil {

    private static final Logger logger = LoggerFactory.getLogger(SocketUtil.class);

//    /**
//     * 接收数据
//     * @return
//     * @throws IOException
//     * @throws InterruptedException
//     */
//    public static String receiveData() throws IOException,InterruptedException {
//        if (SOCKET == null) {
//            CodeSocket server = new CodeSocket();//手机端监听
//            server.start();
//        }
//        byte[] receiveByte = new byte[SOCKET_BUFFER_SIZE];
//        InputStream  inputStream= SOCKET.getInputStream();
//        while(true) {
//            int read = inputStream.read(receiveByte);
//            if(read > -1) {
//                return new String(receiveByte, 0, read);
//            }else {
//                Thread.sleep(SOCKET_THREAD_SELLP_TIME);//睡眠0.01秒
//            }
//        }
//    }
//
//    /**
//     * 数据源
//     * @param data
//     * @throws IOException
//     */
//    public static void sendData(String data) throws IOException {
//        OutputStream outputStream = SOCKET.getOutputStream();
//        outputStream.write(data.getBytes());
//    }

    Socket socket = null;

    public SocketUtil(Socket socket) {
        super();
        this.socket = socket;
    }


    /**
     * 发送数据
     *
     * @param data 待发送的额数据
     * @throws IOException
     */
    public void sendData(String data) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(data.getBytes());
        //outputStream.flush();
        //outputStream.close();
    }


    /**
     * 接收数据
     *
     * @return 接收到的数据
     * @throws IOException
     */
    public String receiveData() throws IOException {
        byte[] receiveByte = new byte[SOCKET_BUFFER_SIZE];
        InputStream inputStream = socket.getInputStream();
        while (true) {
            int read = inputStream.read(receiveByte);
            if (read > -1) {
                return new String(receiveByte, 0, read);
            } else {
                try {
                    Thread.sleep(SOCKET_THREAD_SELLP_TIME);//睡眠0.01秒
                } catch (InterruptedException e) {
                    logger.error("socket 睡眠失败：" + e.getMessage());
                }
            }
        }
    }

}

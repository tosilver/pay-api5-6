package co.b4pay.api.common.zengutils;

import co.b4pay.api.common.tosdomutils.HttpClient;
import co.b4pay.api.service.MallPayTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;

/**
 * 向查询端口发送参数
 */
public class GetToQuery {
    private static final Logger logger = LoggerFactory.getLogger(GetToQuery.class);
    //线上环境
    private static String query="http://202.53.137.124:8080/query.do";
    //测试环境
    //private static String query="http://192.168.101.69:8081/query.do";

    /**
     * 向查询端口发送参数
     * @param outTradeNo
     * @param totalFee
     * @param type
     * @return
     */
    public static void getToQuery1(String outTradeNo,int totalFee ,String type,String address){

        try {
            //构建请求参数
            StringBuilder sb= new StringBuilder();
            sb.append("?");
            sb.append("out_trade_no=").append(outTradeNo).append("&");
            sb.append("total_fee=").append(totalFee).append("&");
            sb.append("pay_type=").append(type).append("&");
            sb.append("address=").append(address).append("&");
            sb.append("ports=").append(1);
            logger.info("向查询端口发送的参数是:"+sb.toString());
            HttpClient httpClient = new HttpClient(query + sb.toString());
            // 返回数据
            logger.warn("请求开始:");
            httpClient.get();
            logger.warn("请求结束!!!!!!!");
            String content = httpClient.getContent();
            logger.info("[查询端口]应答报文:" + content);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void  getToQuery2(String outTradeNo,int totalFee ,String type,String address){
        try {
            //构建请求参数
            StringBuilder sb= new StringBuilder();
            sb.append("?");
            sb.append("out_trade_no=").append(outTradeNo).append("&");
            sb.append("total_fee=").append(totalFee).append("&");
            sb.append("pay_type=").append(type).append("&");
            sb.append("address=").append(address).append("&");
            sb.append("ports=").append(2);
            logger.info("向查询端口发送的参数是:"+sb.toString());
            HttpClient httpClient = new HttpClient(query + sb.toString());
            // 返回数据
            logger.warn("请求开始:");
            httpClient.get();
            logger.warn("请求结束!!!!!!!");
            String content = httpClient.getContent();
            logger.info("[查询端口]应答报文:" + content);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }



}

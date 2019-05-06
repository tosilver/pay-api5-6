package co.b4pay.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.KoubeiTradeTicketTicketcodeQueryRequest;
import com.alipay.api.response.KoubeiTradeTicketTicketcodeQueryResponse;
import org.junit.Test;

public class KouBeiTest {

    @Test
    public void test() {
        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", "app_id", "your private_key", "json", "GBK", "alipay_public_key", "RSA2");
        KoubeiTradeTicketTicketcodeQueryRequest request = new KoubeiTradeTicketTicketcodeQueryRequest();
        request.setBizContent("{" +
                "\"ticket_code\":\"016569843362\"," +
                "\"shop_id\":\"2017071200077000000039734370\"" +
                "  }");
        KoubeiTradeTicketTicketcodeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
    }
}

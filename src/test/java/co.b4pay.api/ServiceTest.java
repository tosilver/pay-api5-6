package co.b4pay.api;

import co.b4pay.api.model.Trade;
import co.b4pay.api.model.Transin;
import co.b4pay.api.service.TradeService;
import co.b4pay.api.service.TransinService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

//替换运行器
@RunWith(SpringRunner.class)//指定 springboot 的引导类
@SpringBootTest(classes = ApplicationStartup.class)
public class ServiceTest {

    @Autowired
    private TransinService transinService;

    @Autowired
    private TradeService tradeService;

    @Test
    public void test() {
       /* Trade trade = tradeService.findByMerchantOrderNo("201901091741061595796");
        String id = trade.getId();
        System.out.println(id);*/
    }
}

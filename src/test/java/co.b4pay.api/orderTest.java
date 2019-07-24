package co.b4pay.api;


import co.b4pay.api.dao.TradeDao;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner. class)
@SpringBootTest(classes = ApplicationStartup. class)
public class orderTest {

    @Autowired
    private TradeDao tradeDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void test(){

        //1.从redis中获取数据,数据的形式为json字符串
        String s = redisTemplate.boundValueOps("trade.findone").get();
        //2.判断redis中是否存在数据
        if (null==s){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("no","0001");
            jsonObject.put("status",1);
            redisTemplate.boundValueOps("trade.findone").set(jsonObject.toJSONString());
            System.out.println("从数据库开始,获取trade数据========");
        }else {
            System.out.println("从redis开始,获取trade数据========");
        }
        System.out.println(s);
    }
}

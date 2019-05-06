package co.b4pay.api.paytest;

import co.b4pay.api.common.tosdomutils.HttpClient;
import co.b4pay.api.common.utils.HttpsUtils;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NotifyTest {

    @Test
    public void notfyTest() {
        try {
            String url = "http://192.168.0.158:9988/notify/aliIndividual.do";
            int status = 0;
            JSONObject notifyJson = new JSONObject();
            notifyJson.put("aa", "bb");
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : notifyJson.entrySet()) {
                map.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            Map<String, String> header = new HashMap<>();
            HttpClient http = new HttpClient(url,map);
            http.post();
            String content = http.getContent();
            System.out.println(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

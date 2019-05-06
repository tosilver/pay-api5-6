package co.b4pay.api.service;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Socket相关Service
 * Created with IntelliJ IDEA
 * Created By AZain
 * Date: 2018-08-22
 * Time: 16:55
 */
@Service
@Transactional
public class SocketService extends BasePayService {

    private static final Logger logger = LoggerFactory.getLogger(SocketService.class);

    public void getway(String receiveData) {

    }

    private JSONObject handleData(String jsonString) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("returnCode", -1);
        if (StringUtils.isNotBlank(jsonString)) {
            try {
                jsonObject = JSONObject.parseObject(jsonString);
                if (jsonObject.containsKey("code") &&
                        jsonObject.containsKey("data")) {
                    String code = jsonObject.getString("code");
                    String data = jsonObject.getString("data");
                    if (StringUtils.isNotBlank(code)
                            && StringUtils.isNotBlank(data)) {
                        try {
                            Map dataMap = JSONObject.parseObject(data);
                            if (dataMap.containsKey("merchantId") &&
                                    dataMap.containsKey("phoneCode")) {
                                String merchantId = (String) dataMap.get("merchantId");
                                String phoneCode = (String) dataMap.get("phoneCode");
                                code = code.replaceAll(merchantId + phoneCode, "");

                            } else {
                                jsonObject.put("msg", String.format("data :[%s] 参数不完整，缺少 merchantId 或者 phoneCode", data));
                                logger.warn(String.format("data :[%s] 参数不完整，缺少 merchantId 或者 phoneCode", data));
                            }
                        } catch (Exception e) {
                            jsonObject.put("msg", String.format("data : [%s] 不是json字符串", data));
                            logger.warn(String.format("data : [%s] 不是json字符串", data));
                        }
                    } else {
                        jsonObject.put("msg", String.format("[%s] code为空或者data为空", jsonString));
                        logger.warn(String.format("[%s] code为空或者data为空", jsonString));
                    }
                } else {
                    jsonObject.put("msg", String.format("[%s] 参数不完整，缺少 code 或者 data", jsonString));
                    logger.warn(String.format("[%s] 参数不完整，缺少 code 或者 data", jsonString));
                }
            } catch (JSONException ex) {
                jsonObject.put("msg", String.format("[%s] 不是json字符串", jsonString));
                logger.error(String.format("[%s] 不是json字符串", jsonString));
            }
        } else {

            logger.warn("Socket 接收的消息为空");
        }
        return jsonObject;
    }

}

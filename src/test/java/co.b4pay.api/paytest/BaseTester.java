package co.b4pay.api.paytest;


import co.b4pay.api.common.constants.Constants;
import co.b4pay.api.common.kit.PropKit;
import co.b4pay.api.common.utils.HttpsUtils;
import co.b4pay.api.signature.HmacSHA1Signature;
import co.b4pay.api.signature.SignatureUtil;

import java.io.File;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.Map;

public abstract class BaseTester {


    //private static final String serverUrl = "http://api.dev.b4pay.co"; // 测试环境
    //private static final String serverUrl = "http://test1.b4chain.hk"; // 测试环境
    //private static final String serverUrl = "http://127.0.0.1:9988"; // 本地环境
    //private static final String serverUrl = "http://api.b4187.com"; // 本地环境
    //private static final String serverUrl = "http://api.b4bisi.com"; // 本地环境
    private static final String serverUrl = "http://api.jbhsh.net"; // 本地环境

    private static final HmacSHA1Signature hmacSHA1Signature = new HmacSHA1Signature();

    static {
        //System.out.println(System.getProperty("user.home"));
        PropKit.use(new File(System.getProperty("user.home"), "user.properties"));
    }

    protected static String execute(String apiUri, Map<String, String> params) throws IOException, SignatureException {
        params.remove("signature");
        // 系统级别参数 merchantId、timestamp、signature
        //params.put("merchantId", PropKit.get("merchantId"));
        params.put("merchantId", "100000000000005");
        params.put("timestamp", String.valueOf(Calendar.getInstance().getTimeInMillis()));
        //params.put("timestamp", "2019-01-16 16:39:16");
        String content = SignatureUtil.getSignatureContent(params, true);
        //System.out.println("content:"+content);
        String sign = hmacSHA1Signature.sign(content, PropKit.get("secretKey"), Constants.CHARSET_UTF8);
        //System.out.println("sign:"+sign);
        params.put("signature", sign);
        String result = HttpsUtils.post(serverUrl + apiUri, null, params);
        return result;
    }
}

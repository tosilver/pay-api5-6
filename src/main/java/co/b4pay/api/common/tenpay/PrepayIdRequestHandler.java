package co.b4pay.api.common.tenpay;

import co.b4pay.api.common.tenpay.client.TenpayHttpClient;
import co.b4pay.api.common.tenpay.util.*;
import co.b4pay.api.common.tenpay.client.TenpayHttpClient;
import co.b4pay.api.common.tenpay.util.XMLUtil;
import co.b4pay.api.common.tenpay.client.TenpayHttpClient;
import co.b4pay.api.common.tenpay.util.XMLUtil;
import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSONObject;
import org.jdom.JDOMException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PrepayIdRequestHandler extends RequestHandler {

    public PrepayIdRequestHandler(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    public String createMD5Sign() {
        StringBuffer sb = new StringBuffer();
        Set es = super.getAllParameters().entrySet();
        Iterator it = es.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String k = (String) entry.getKey();
            String v = (String) entry.getValue();
            if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        sb.append("key=" + this.getKey());

        String enc = TenpayUtil.getCharacterEncoding(this.request, this.response);
        String sign = MD5Util.MD5Encode(sb.toString(), enc).toUpperCase();

        // debug信息
        this.setDebugInfo(sb.toString() + " => sign:" + sign);

        return sign;
    }

    /**
     * 创建签名SHA1
     *
     * @throws Exception
     */
    public String createSHA1Sign() {
        StringBuffer sb = new StringBuffer();
        Set es = super.getAllParameters().entrySet();
        Iterator it = es.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String k = (String) entry.getKey();
            String v = (String) entry.getValue();
            sb.append(k + "=" + v + "&");
        }
        String params = sb.substring(0, sb.lastIndexOf("&"));
        String appsign = Sha1Util.getSha1(params);
        this.setDebugInfo(this.getDebugInfo() + "\r\n" + "sha1 sb:" + params);
        this.setDebugInfo(this.getDebugInfo() + "\r\n" + "app sign:" + appsign);
        return appsign;
    }

    // 提交预支付
    public String sendPrepay() throws JDOMException, IOException {
        String params = "<xml>" + getXmlBody() + "</xml>";
        String requestUrl = super.getGateUrl();
        this.setDebugInfo(this.getDebugInfo() + "\r\n" + "requestUrl:" + requestUrl);
        TenpayHttpClient httpClient = new TenpayHttpClient();
        httpClient.setCharset("UTF-8");
        httpClient.setReqContent(requestUrl);
        this.setDebugInfo(this.getDebugInfo() + "\r\n" + "post data:" + params);
        if (httpClient.callHttpPost(requestUrl, params)) {
            String resContent = httpClient.getResContent();
            this.setDebugInfo(this.getDebugInfo() + "\r\n" + "resContent:" + resContent);
            if (resContent != null && !"".equals(resContent.trim())) {
                return JSONUtils.toJSONString(XMLUtil.doXMLParse(resContent));
            }
        }
        return null;
    }

    // 判断access_token是否失效
    public String sendAccessToken() {
        String accesstoken = "";
        StringBuffer sb = new StringBuffer("{");
        Set es = super.getAllParameters().entrySet();
        Iterator it = es.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String k = (String) entry.getKey();
            String v = (String) entry.getValue();
            if (null != v && !"".equals(v) && !"appkey".equals(k)) {
                sb.append("\"" + k + "\":\"" + v + "\",");
            }
        }
        String params = sb.substring(0, sb.lastIndexOf(","));
        params += "}";

        String requestUrl = super.getGateUrl();
        // this.setDebugInfo(this.getDebugInfo() + "\r\n" + "requestUrl:"
        // + requestUrl);
        TenpayHttpClient httpClient = new TenpayHttpClient();
        httpClient.setReqContent(requestUrl);
        String resContent = "";
        // this.setDebugInfo(this.getDebugInfo() + "\r\n" + "post data:" +
        // params);
        if (httpClient.callHttpPost(requestUrl, params)) {
            resContent = httpClient.getResContent();
            if (2 == resContent.indexOf(ConstantUtil.ERRORCODE)) {
                accesstoken = resContent.substring(11, 16);// 获取对应的errcode的值
            }
        }
        return accesstoken;
    }

}

package co.b4pay.api.common.zengutils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

/* 
 * 利用HttpClient进行post请求的工具类 
 */  
public class HttpClientUtil {  
    public String doPost(String url,String params,String charset){  
        HttpClient httpClient = null;
        HttpPost httpPost = null;
        String result = null;  
        try{  
            httpClient = new SSLClient();  
            httpPost = new HttpPost(url);
    		StringEntity s = new StringEntity(params, charset);
    		s.setContentEncoding(charset);
    		s.setContentType("application/json");// 发送json格式的数据需要设置contentType
    		httpPost.setEntity(s);
            HttpResponse response = httpClient.execute(httpPost);
            if(response != null){  
                HttpEntity resEntity = response.getEntity();
                if(resEntity != null){  
                    result = EntityUtils.toString(resEntity,charset);
                }  
            }  
        }catch(Exception ex){  
            ex.printStackTrace();  
        }  
        return result;  
    }  
} 

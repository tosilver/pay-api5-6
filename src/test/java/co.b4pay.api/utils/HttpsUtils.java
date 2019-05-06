package co.b4pay.api.utils;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpsUtils {

    private static CloseableHttpClient httpClient = getHttpClient();

    private static int SocketTimeout = 30000;//30秒
    private static int ConnectTimeout = 30000;//30秒
    private static Boolean SetTimeOut = true;

    /**
     * get
     *
     * @param url     请求的url
     * @param headers 请求的头，没有可以传null
     * @param queries 请求的参数，在浏览器？后面的数据，没有可以传null
     * @return
     * @throws IOException
     */
    public static String get(String url, Map<String, String> headers, Map<String, String> queries) throws IOException {
        HttpGet httpGet = new HttpGet(buildUrlAndQueries(url, queries));
        return execute(httpGet, headers);
    }

    /**
     * post
     *
     * @param url     请求的url
     * @param headers 请求的头，没有可以传null
     * @param content post 提交的字符串参数内容
     * @return
     * @throws IOException
     */
    public static String post(String url, Map<String, String> headers, String content) throws IOException {
        //指定url,和http方式
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(content, Consts.UTF_8));
        return execute(httpPost, headers);
    }

    /**
     * post
     *
     * @param url     请求的url
     * @param headers 请求的头，没有可以传null
     * @param params  post form 提交的参数
     * @return
     * @throws IOException
     */
    public static String post(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        //指定url,和http方式
        HttpPost httpPost = new HttpPost(url);
        //添加参数
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (params != null && params.keySet().size() > 0) {
            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
        return execute(httpPost, headers);
    }

    /**
     * 执行请求
     *
     * @param httpRequest {@see org.apache.http.client.methods.HttpUriRequest }
     * @param headers     请求的头，没有可以传null
     * @return
     * @throws IOException
     */
    private static String execute(HttpRequestBase httpRequest, Map<String, String> headers) throws IOException {
        if (SetTimeOut) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(SocketTimeout)
                    .setConnectTimeout(ConnectTimeout).build();//设置请求和传输超时时间
            httpRequest.setConfig(requestConfig);
        }
        // 设置请求头
        if (headers != null && !headers.isEmpty()) {
            for (String name : headers.keySet()) {
                httpRequest.addHeader(name, headers.get(name));
            }
        }
        //请求数据
        CloseableHttpResponse response = httpClient.execute(httpRequest);
        try {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String responseBody = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
                return responseBody;
            } else {
                throw new HttpStatusException(statusLine.getReasonPhrase(), statusLine.getStatusCode(), httpRequest.getURI().getPath());
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    /**
     * 补全URL和请求参数
     *
     * @param url     请求的url
     * @param queries 请求的参数，在浏览器？后面的数据
     * @return 带参数完整的URL地址
     */
    public static String buildUrlAndQueries(String url, Map<String, String> queries) {
        if (queries == null || queries.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        boolean firstFlag = true;
        for (Map.Entry<String, String> entry : queries.entrySet()) {
            if (firstFlag) {
                sb.append("?").append(entry.getKey()).append("=").append(entry.getValue());
                firstFlag = false;
            } else {
                sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return sb.toString();
    }


    private static CloseableHttpClient getHttpClient() {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
        ConnectionSocketFactory plainSF = new PlainConnectionSocketFactory();
        registryBuilder.register("http", plainSF);
        try {
            // 指定信任密钥存储对象和连接套接字工厂
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            // 信任任何链接
            TrustStrategy anyTrustStrategy = new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            };
            SSLContext sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore, anyTrustStrategy).build();
            LayeredConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registryBuilder.register("https", sslSF); //支持https
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Registry<ConnectionSocketFactory> registry = registryBuilder.build();
        //设置连接管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
//      connManager.setDefaultConnectionConfig(connConfig);
//      connManager.setDefaultSocketConfig(socketConfig);
        //构建客户端
        return HttpClients.custom().setConnectionManager(connManager).build();
    }

    public static class HttpStatusException extends IOException {
        private int statusCode;
        private String url;

        public HttpStatusException(String message, int statusCode, String url) {
            super(message);
            this.statusCode = statusCode;
            this.url = url;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return super.toString() + ". Status=" + statusCode + ", URL=" + url;
        }
    }
}

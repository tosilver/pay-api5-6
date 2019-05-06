package co.b4pay.api.common.alipay;

public class AlipayConfig {
    // 商户appid
    public static String APPID = "2016091800540351";
    // 私钥 pkcs8格式的
    public static String RSA_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDiwxiqspCfFhT9P51zqt0ojinNDNn04a6QqVs7vJNnu0XWHypXe6xj5kE9vZ0zJ9j+BYlbHdGO0V+tarUuSFvtIXN8VYLtT2ImJf1ij5NMIWmzXLN1TKJx2ORdo82FQhT4tbmZOI0tkHSScEG/5+axz82eMfO2KD1QwbzQnnp57TmE1IkEiwskUY3bZWLbP6TDvrvIYCaygPbB+GivnEq4KkEuU3LyYQyvg2LF1dDKtg8IDuC84Sy8zeD9j5EGDQigR0y0o9coX2lOn3ePV66oZP1wfoyAaPWpNjNTujId+OsuP4BJbQnSpWifgayzuxs/oyKOtLPX8lAihDOSQfUFAgMBAAECggEANH4bQLCTX/BZrYJDbZo5FgFK1efKsLBpm51IvFxb9yBX+0g9ogDEGGAd7C/vqv7ncW5QrKILd7cqIdfY1zzy4sLim/6jj1HYE6KptF4uJ+p4MIgeFuJFnHsr6i1YDygL+MMAkuKPc+PDyH5qCgWh1rmXDK7djuZjw0UF83ksOvrqqVnqR2WdPRsSLW9LmY/zwcBftvchRqXZv4AGf8F1NgP0WBoSKkUKYOcqfUOTs7DTORG0hdQ/xCCVuTQ2Hm4LWUWaSb6gYX0kGyRGu7UnT8BtYhGu5gDwPHVAhUTPhDjdXf/fkX5BpArwuQuwcdZbuP/fdhDzzDrkiJmx9TuQxQKBgQD9y8lrJWawmoFajcZIGYfPjWfN4nv7P1q8w+KneJBY9fNST4iQwhazW2OPb7aOCtextmQeinXlr+EESsk6uIcM3rtELDpUNv8V+fDbJJm+RaOULyIZgwLAxmOUNlDjQz0wGufypN+9vGFb3TQKwSz2J1NDpJFm43lX5o3IPD04cwKBgQDkuzXh5Sar7YSu4c26RZkquCa+y85l1rBNYAtK7gyWDkOmdjLXc2m1EzvDQ8umL2psNDSpiNpif6XOk/9D+79tYdqKPV+jTktS53cUbWBR8Axl9NoYg9+QACRcazrj9IcXAk/QtkGL1UGXE+8lJzTWL0jk1qhw7YrcQ+O/M9bWpwKBgQCaUVQqgzZagcfPcM80vdlXeUq55FGwpogcqRri66sRfJMz5EpMgsdczV/PMoUU/0DBvP5Jl7UyMOwoOPT8cIElcTT6sc3RsRMPoMcz6KBXEm7xRmt8ia2d28NmtlQeq57D5khMwLWO38FvIyRmuakGD0lQovsKTZxVt0lUjFRJQwKBgD4pjtxEnbzuAThSD8pG7fiJMaZ61y4gKavtpUQI7Ay/9azAxNJ/AESA5KYNv8P7cO3VRlao5ckNLe+1kxNT0NOWW4FkaqCEP75ZP6iijSHXnlb7M2akOFb3YupnDgszwp8DNtPfJHMvUvMPLNgpDpgDI8lleUOvmyR/ot6s5P9TAoGBAMIDkxYjALzJUL8w2VcKioH6Kcu8yxW3B3UX230B7UPPSqdQd7YnN3YATjrVQTAEzvIMQKbvQKPbjHU0t7T9+9lKzvbc4xSBJnJYuUs5zkDLidh7YRgkk4fM6kUGFnuqDFiba5OlEKiYJj7sTK7oZ5sfyhJtHYVX4OMfIfy9s6Oq";
    // 服务器异步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String notify_url = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/notify_url.jsp";
    // 页面跳转同步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问 商户可以自定义同步跳转地址
    public static String return_url = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/return_url.jsp";
    // 请求网关地址
    public static String URL = "https://openapi.alipaydev.com/gateway.do"; //"https://openapi.alipay.com/gateway.do";
    // 编码
    public static String CHARSET = "UTF-8";
    // 返回格式
    public static String FORMAT = "json";
    // 支付宝公钥
    public static String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuWTJcL/Tyf0t1j/dytbkD4qZ/6AgLAG4GHt60UnarexED2dDlULwm8aEl/5FDF+a3xEaAngzBNchVnQqQxI586mQJ7nhQQd3bkYtTHOYljQ/hiVtOR+LV3svV3p4VfYY63a8SQlZ0gqoa1et7yu2T1s6IAr7+Zuaw/Yp8zNbdnUo0pf7yE+Xrj4q3SElLpmZ2QPE1OerT/0QxloH2QyeKm7U7qdonE5gQJmqwBA0t1aZZhV1FTW3mjbF3+Hwtpm6zZXo0RGJ0EfJlPj7FlfQt0iThHukPZcqTDYZscMHIsHirUISSlh1ENM/UXa3YjjnzOQ9oiOKcOZzs10MVrNNWQIDAQAB";
    // 日志记录目录
    public static String log_path = "/log";
    // RSA2
    public static String SIGNTYPE = "RSA2";
}

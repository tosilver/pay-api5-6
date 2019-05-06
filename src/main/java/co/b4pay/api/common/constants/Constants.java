package co.b4pay.api.common.constants;

/**
 * Web 常量
 *
 * @author YK
 * @version $Id: Constants.java, v 0.1 2016年5月5日 下午6:49:32 YK Exp $
 */
public final class Constants {

    public static final String MERCHANT_ID = "merchantId";

    public static final String LOGIN_ID_KEY = "user_id";

    public static final String OPEN_ID_KEY = "open_id";

    @Deprecated
    /** 默认字符类型 */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * UTF-8字符类型
     */
    public static final String CHARSET_UTF8 = "UTF-8";

    /**
     * 验证码
     */
    public static final String CAPTCHA = "captcha";

    /**
     * 请求转发标示 forward:
     */
    public static final String FORWARD = "forward:";

    /**
     * 请求重定向标示 redirect:
     */
    public static final String REDIRECT = "redirect:";

    /**
     * 请求重定向隐藏标示 _redirect_
     */
    public static final String REDIRECT_HIDDEN = "_redirect_";

    /**
     * message标识
     */
    public static final String MESSAGE = "message";

    /**
     * HEADER
     */
    public static final String BASE64_HEADER = "data:image/png;base64,";

    public static final String SUCCESS = "SUCCESS";

    public static final String FAILURE = "FAILURE";
}

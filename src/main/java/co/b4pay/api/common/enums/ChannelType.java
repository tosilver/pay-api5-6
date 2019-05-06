package co.b4pay.api.common.enums;

/**
 * 支付使用的第三方支付渠道
 *
 * @author YK
 * @version $Id: Channel.java, v 0.1 2016年5月5日 下午3:50:56 YK Exp $
 */
public enum ChannelType {

    YUEMAN("YUEMAN", "越满金融"),

    WEIXIN("WEIXIN", "官方微信"),

    /**
     * alipay: 支付宝手机支付
     */
    ALIPAY("alipay", "支付宝手机支付"),

    /**
     * kjpay
     */
    KJPAY("kjpay","快捷支付"),

    /**
     * kjpay
     */
    YEDFPAY("yedfpay","余额代付"),

    /**
     * shpay: 商户中转支付
     */
    SHPAY("shpay", "商户中转支付"),

    /**
     * alipay: 任意额支付
     */
    QRCODE("qrCode", "二维码支付"),

    /**
     * alipay_wap: 支付宝手机网页支付
     */
    ALIPAY_WAP("alipay_wap", "支付宝手机网页支付"),

    /**
     * alipay_qr: 支付宝扫码支付
     */
    ALIPAY_QR("alipay_qr", "支付宝扫码支付"),

    /**
     * alipay_pc_direct: 支付宝 PC 网页支付
     */
    ALIPAY_PC_DIRECT("alipay_pc_direct", "支付宝 PC 网页支付"),

    /**
     * bfb: 百度钱包移动快捷支付
     */
    BFB("bfb", "百度钱包移动快捷支付"),

    /**
     * bfb_wap: 百度钱包手机网页支付
     */
    BFB_WAP("bfb_wap", "百度钱包手机网页支付"),

    /**
     * upacp: 银联全渠道支付（2015 年 1 月 1 日后的银联新商户使用。若有疑问，请与 Ping++ 或者相关的收单行联系）
     */
    UPACP("upacp", "银联全渠道支付"),

    /**
     * upacp_wap: 银联全渠道手机网页支付（2015 年 1 月 1 日后的银联新商户使用。若有疑问，请与 Ping++ 或者相关的收单行联系）
     */
    UPACP_WAP("upacp_wap", "银联全渠道手机网页支付"),

    /**
     * upacp_pc: 银联 PC 网页支付
     */
    UPACP_PC("upacp_pc", "银联 PC 网页支付"),

    /**
     * cp_b2b: 银联企业网银支付
     */
    CP_B2B("cp_b2b", "银联企业网银支付"),

    /**
     * wx: 微信支付
     */
    WX("wx", "微信支付"),

    /**
     * wx_pub: 微信公众账号支付
     */
    WX_PUB("wx_pub", "微信公众账号支付"),

    /**
     * wx_pub_qr: 微信公众账号扫码支付
     */
    WX_PUB_QR("wx_pub_qr", "微信公众账号扫码支付"),

    /**
     * yeepay_wap: 易宝手机网页支付
     */
    YEEPAY_WAP("yeepay_wap", "易宝手机网页支付"),

    /**
     * jdpay_wap: 京东手机网页支付
     */
    JDPAY_WAP("jdpay_wap", "京东手机网页支付"),

    /**
     * cnp_u: 应用内快捷支付（银联）
     */
    CNP_U("cnp_u", "应用内快捷支付（银联）"),

    /**
     * cnp_f: 应用内快捷支付（外卡）
     */
    CNP_F("cnp_f", "应用内快捷支付（外卡）"),

    /**
     * applepay_upacp: Apple Pay
     */
    APPLEPAY_UPACP("applepay_upacp", "Apple Pay"),

    /**
     * fqlpay_wap: 分期乐支付
     */
    FQLPAY_WAP("fqlpay_wap", "分期乐支付"),

    /**
     * qgbc_wap: 量化派支付
     */
    QGBC_WAP("qgbc_wap", "量化派支付"),

    ;

    private ChannelType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private String code;
    private String desc;

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}

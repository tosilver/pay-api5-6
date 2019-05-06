package co.b4pay.api.common.zengutils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @描述: 支付类型.
 * @author:
 * @date: 2017年4月24日 下午7:02:28 
 * @version: V1.0   
 */
public enum PayTypeEnum {
	
	//ALIPAY_SCAN_CODE_PAY("支付宝扫码支付(主扫-返URL)","009"),//	支付宝扫码支付(主扫-返URL，客户端生成二维码主扫)
	//WECHAT_SCAN_CODE_PAY("微信扫码支付(主扫-返URL)","011"),//	微信扫码支付(主扫-返URL，客户端生成二维码主扫
	B2C_GATEWAY_PAY("银联在线","015"),
	//QQ_SCAN_CODE_PAY("QQ扫码支付","020"),
	//ALIPAY_H5_PAY("支付宝H5支付","040"),
	BALANCE_PAY("余额代付","035");
	
	
	
	/** 描述 */
	private String desc;
	/** 枚举值 */
	private String value;

	private PayTypeEnum(String desc, String value) {
		this.desc = desc;
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	public static PayTypeEnum getEnum(String value){
		PayTypeEnum resultEnum = null;
		PayTypeEnum[] enumAry = PayTypeEnum.values();
		for(int i = 0;i<enumAry.length;i++){
			if(enumAry[i].getValue().equals(value)){
				resultEnum = enumAry[i];
				break;
			}
		}
		return resultEnum;
	}
	
	public static Map<String, Map<String, Object>> toMap() {
		PayTypeEnum[] ary = PayTypeEnum.values();
		Map<String, Map<String, Object>> enumMap = new HashMap<String, Map<String, Object>>();
		for (int num = 0; num < ary.length; num++) {
			Map<String, Object> map = new HashMap<String, Object>();
			String key = String.valueOf(getEnum(ary[num].getValue()));
			map.put("value", ary[num].getValue());
			map.put("desc", ary[num].getDesc());
			enumMap.put(key, map);
		}
		return enumMap;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List toList(){
		PayTypeEnum[] ary = PayTypeEnum.values();
		List list = new ArrayList();
		for(int i=0;i<ary.length;i++){
			Map<String,String> map = new HashMap<String,String>();
			map.put("value",ary[i].getValue());
			map.put("desc", ary[i].getDesc());
			list.add(map);
		}
		return list;
	}

}

package co.b4pay.api.common.zengutils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class HCMD5 {
	private static final Logger logger = LoggerFactory.getLogger(HCMD5.class);


	private final static String[] strDigits = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	public HCMD5() {
	}

	// 返回形式为数字跟字符串
	private static String byteToArrayString(byte bByte) {
		int iRet = bByte;
		if (iRet < 0) {
			iRet += 256;
		}
		int iD1 = iRet / 16;
		int iD2 = iRet % 16;
		return strDigits[iD1] + strDigits[iD2];
	}

	// 返回形式只为数字
	@SuppressWarnings("unused")
	private static String byteToNum(byte bByte) {
		int iRet = bByte;
		System.out.println("iRet1=" + iRet);
		if (iRet < 0) {
			iRet += 256;
		}
		return String.valueOf(iRet);
	}

	// 转换字节数组为16进制字串
	private static String byteToString(byte[] bByte) {
		StringBuffer sBuffer = new StringBuffer();
		for (int i = 0; i < bByte.length; i++) {
			sBuffer.append(byteToArrayString(bByte[i]));
		}
		return sBuffer.toString();
	}
	
	/**
	 * UTF8编码
	 * @param source
	 * @return
	 */
	public static String codeFor(String source) {
		if(HCConfig.getBoolean("testEnv")){
			logger.info("参与签名字符串为：" + source);
		}
		return codeFor(source, "UTF-8");
	}

	/**
	 * 进行加密
	 * @param source 加密字符串
	 * @return
	 */
	public static String codeFor(String source,String charset) {
		String resultString = null;
		try {
			resultString = new String(source);
			MessageDigest md = MessageDigest.getInstance("MD5");
			// md.digest() 该函数返回值为存放哈希值结果的byte数组
			resultString = byteToString(md.digest(source.getBytes(charset)));
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return resultString;
	}
	
	/** 参数排序md5加密 */
	public static String sortMd5Encrypt(Map<String, String> map,String encryptKey){
		String sign = "";
		String encryptData = "";
		Set<String> keySet = map.keySet();
		List<String> keyList = new ArrayList<String>(keySet);
		//log.info("排序前："+keyList.toString());
		//先将key冒泡排序从小到大排序
		for(int i = 0;i<keyList.size();i++){
			String key = keyList.get(i);
			for(int j = i+1;j<keyList.size();j++){
				String key2 = keyList.get(j);
				if(key.compareTo(key2)>0){
					String temp = key;
					keyList.set(i, key2);
					keyList.set(j, temp);
					key = key2;
				}
			}
		}
		//log.info("排序后："+keyList.toString());
		int i = 0;
		for(String key : keyList){
			i++;
			encryptData += (i!=1?"&":"")+key + "=" + map.get(key);
		}
		encryptData += encryptKey;
		try {
			sign = codeFor(encryptData,"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return "exception";
		}
		return sign;
	}
	
	/** 参数排序md5加密 */
	public static String unionPayEncrypt(Map<String, String> map,String encryptKey){
		String sign = "";
		String encryptData = "";
		Set<String> keySet = map.keySet();
		List<String> keyList = new ArrayList<String>(keySet);
		//log.info("排序前："+keyList.toString());
		//先将key冒泡排序从小到大排序
		for(int i = 0;i<keyList.size();i++){
			String key = keyList.get(i);
			for(int j = i+1;j<keyList.size();j++){
				String key2 = keyList.get(j);
				if(key.compareTo(key2)>0){
					String temp = key;
					keyList.set(i, key2);
					keyList.set(j, temp);
					key = key2;
				}
			}
		}
		//log.info("排序后："+keyList.toString());
		System.out.println("排序后："+keyList.toString());
		for(String key : keyList){
			encryptData += key + "=" + map.get(key) + "&";
		}
		encryptData += codeFor(encryptKey,"UTF-8");
		try {
			sign = codeFor(encryptData,"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sign;
	}
	
	/** 参数排序md5加密 */
	public static String yufuPayEncrypt(Map<String, String> map,String encryptKey){
		String sign = "";
		String encryptData = "";
		try {
			Set<String> keySet = map.keySet();
			List<String> keyList = new ArrayList<String>(keySet);
			//log.info("排序前："+keyList.toString());
			//先将key冒泡排序从小到大排序
			for(int i = 0;i<keyList.size();i++){
				String key = keyList.get(i);
				for(int j = i+1;j<keyList.size();j++){
					String key2 = keyList.get(j);
					if(key.compareTo(key2)>0){
						String temp = key;
						keyList.set(i, key2);
						keyList.set(j, temp);
						key = key2;
					}
				}
			}
			//log.info("排序后："+keyList.toString());
			System.out.println("排序后："+keyList.toString());
			for(String key : keyList){
				encryptData += key + "=" + map.get(key) + "&";
			}
			encryptData = encryptData.substring(0,encryptData.length() - 1);
			encryptData += encryptKey;
			sign = MD5(encryptData,"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sign;
	}
	
	/** 参数排序md5加密 */
	public static String jiaofeiPayEncrypt(Map<String, String> map,String encryptKey){
		String sign = "";
		String encryptData = "";
		try {
			Set<String> keySet = map.keySet();
			List<String> keyList = new ArrayList<String>(keySet);
			//log.info("排序前："+keyList.toString());
			//先将key冒泡排序从小到大排序
			for(int i = 0;i<keyList.size();i++){
				String key = keyList.get(i);
				for(int j = i+1;j<keyList.size();j++){
					String key2 = keyList.get(j);
					if(key.compareTo(key2)>0){
						String temp = key;
						keyList.set(i, key2);
						keyList.set(j, temp);
						key = key2;
					}
				}
			}
			//log.info("排序后："+keyList.toString());
			System.out.println("排序后："+keyList.toString());
			for(String key : keyList){
				encryptData += map.get(key);
			}
			encryptData += encryptKey;
			sign = MD5(encryptData,"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sign;
	}
	
	
	
	public final static String MD5(String s,String charSet) {
        char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};       
        try {
            byte[] btInput = s.getBytes(charSet);
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	/**
	 * 排序签名
	 * @param map 参与签名的数据
	 * @param encryptKey 密钥
	 * @param keyexist 算签名的字符串是否需要key= true需要 false 不需要
	 * @return
	 */
	public static String sortMd5Encrypt(Map<String, Object> map,String encryptKey,boolean keyexist){
		String sign = "";
		String encryptData = "";
		try {
			Set<String> keySet = map.keySet();
			List<String> keyList = new ArrayList<String>(keySet);		
			Collections.sort(keyList);//排序或者自己写冒泡排序				
			int i = 0;
			for(String key : keyList){
				if(ESBoolean.isNull(map.get(key)))
					continue;
				i++;
				encryptData += (i!=1?"&":"")+key + "=" + map.get(key);
			}	
			encryptData = keyexist ? encryptData + "&"+"key" +"="+encryptKey:encryptData + encryptKey;
			sign = codeFor(encryptData,"UTF-8");
			
		} catch (Exception e) {
			e.printStackTrace();
			return "exception";
		}
		return sign;
	}

	public static String getMD5Singe(Map<String,String> bizObj , String key) throws Exception{

		LinkedHashMap<String, String> bizParameters = new LinkedHashMap<String, String>();

		List<Map.Entry<String, String>> infoIds = new ArrayList<Map.Entry<String, String>>(
				bizObj.entrySet());

		Collections.sort(infoIds, new Comparator<Map.Entry<String, String>>() {
			public int compare(Map.Entry<String, String> o1,
					Map.Entry<String, String> o2) {
				return (o1.getKey()).toString().compareTo(o2.getKey());
			}
		});

		for (int i = 0; i < infoIds.size(); i++) {
			
			Map.Entry<String, String> item = infoIds.get(i);
			if ( !ESBoolean.isNull(item.getKey()) && !ESBoolean.isNull(item.getValue()) ) {
				logger.info("item.getKey():" + item.getKey());
				bizParameters.put(item.getKey(), item.getValue());
				
			}
		}

		if (key == "") {
			throw new Exception("key为空！");
		}
		bizParameters.put("key", key);
		String bizString = formatQueryParaMap(bizParameters,false);
		
		logger.info("**********************************  签名数据  ************************************");
		logger.info("签名前：" + bizString);
		bizString = codeFor(bizString).toUpperCase();
		logger.info("签名后" + bizString);
		logger.info("**********************************  签名数据END  ************************************");
		
		return bizString;

	
	}
	
	public static String formatQueryParaMap(Map<String,String> paraMap , boolean urlencode) throws Exception{
		String buff = "";
		try {
			List<Map.Entry<String, String>> infoIds = new ArrayList<Map.Entry<String, String>>(
					paraMap.entrySet());

			for (int i = 0; i < infoIds.size(); i++) {
				Map.Entry<String, String> item = infoIds.get(i);
				//log.info(item.getKey());
				if (item.getKey() != "") {
					
					String key = item.getKey();
					String val = item.getValue();
					if (urlencode) {
						val = URLEncoder.encode(val, "utf-8");

					}
					buff += key + "=" + val + "&";
//					buff += key.toLowerCase() + "=" + val + "&";
				}
			}

			if (buff.isEmpty() == false) {
				buff = buff.substring(0, buff.length() - 1);
			}
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return buff;
}
	
}

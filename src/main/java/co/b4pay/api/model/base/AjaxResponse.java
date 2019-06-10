package co.b4pay.api.model.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * AJAX请求的 JSON类型的返回值结果数据对象
 *
 * @author YK
 * @version $Id: AjaxResponse.java, v 0.1 2013-9-2 上午11:38:17 YK Exp $
 */
public final class AjaxResponse implements Serializable {

    /** An empty immutable <code>String</code> . */
//    public static final String  EMPTY_STRING = "";

    /**
     * 成功状态码
     */
    private static final int SUCCESS_CODE = 1;

    /**
     * 成功返回消息
     */
    private static final String SUCCESS_MSG = "SUCCESS";

    /**
     * 失败状态码
     */
    private static final int FAILURE_CODE = -1;

    /**
     * 失败返回消息
     */
    private static final String FAILURE_MSG = "系统异常";

    /**
     * An empty immutable <code>Object</code> array.
     */
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * An empty immutable <code>Object</code> map.
     */
    private static final Map<String, Object> EMPTY_OBJECT_MAP = new HashMap<String, Object>();

    /**
     * 成功返回结果对象
     */
    private static final AjaxResponse SUCCESS = new AjaxResponse(SUCCESS_CODE, SUCCESS_MSG, null, null);

    /**
     * 失败返回结果对象
     */
    private static final AjaxResponse FAILURE = new AjaxResponse(FAILURE_CODE, FAILURE_MSG, null, null);

    /**
     * 状态码
     */
    private int code;

    /**
     * 返回消息
     */
    private String msg;

    /**
     * 返回数据
     */
    private Map<String, Object> data;//         = new HashMap<String, Object>();

    private AjaxResponse() {
    }

    /**
     * AJAX请求的 JSON类型的返回值
     *
     * @param code 状态码
     * @param msg  返回消息
     * @param key  返回值KEY
     * @param data 返回数据
     */
    private AjaxResponse(int code, String msg, String key, Object data) {
        this.code = code;
        this.msg = msg;
        if (key == null) {
            return;
        }
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, data);
    }


    /**
     * AJAX请求的 JSON类型的返回值
     *
     * @param code 状态码
     * @param msg  返回消息
     * @param map  返回值map对象
     */
    private AjaxResponse(int code, String msg, Map<String, Object> map) {
        this.code = code;
        this.msg = msg;
        if (map == null) {
            return;
        }
//        if (map.containsKey("code") || map.containsKey("msg")) {
//            throw new RuntimeException("参数不能包含系统定义参数[code和msg]");
//        }
//        if (this.data == null) {
//            this.data = new HashMap<>();
//        }
//        for (String key : map.keySet()) {
//            this.data.put(key, map.get(key));
//        }
        this.data = map;
    }

    public AjaxResponse add(String key, Object value) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, value);
        return this;
    }

    /**
     * 创建
     *
     * @param code 状态码
     * @return
     */
    public static AjaxResponse create(int code) {
        return new AjaxResponse(code, SUCCESS_MSG, null, null);
    }

    /**
     * 创建
     *
     * @param code 状态码
     * @param msg  返回消息
     * @return
     */
    public static AjaxResponse create(int code, String msg) {
        return new AjaxResponse(code, msg, null, null);
    }

    /**
     * 创建
     *
     * @param code 状态码
     * @param key  返回值KEY
     * @param data 返回数据
     * @return
     */
    public static AjaxResponse create(int code, String key, Object... data) {
        return new AjaxResponse(code, SUCCESS_MSG, key, data);
    }

    /**
     * 成功
     *
     * @return
     */
    public static AjaxResponse success() {
        return new AjaxResponse(SUCCESS_CODE, SUCCESS_MSG, null, null);
    }

    /**
     * 成功
     *
     * @param msg 返回消息
     * @return
     */
    public static AjaxResponse success(String msg) {
        return new AjaxResponse(SUCCESS_CODE, msg, null, null);
    }

    /**
     * 成功
     *
     * @param map 返回map对象
     * @return
     */
    public static AjaxResponse success(Map<String, Object> map) {
        return new AjaxResponse(SUCCESS_CODE, SUCCESS_MSG, map);
    }

    /**
     * 成功
     *
     * @param code 状态码
     * @param msg  返回消息
     * @return
     */
    public static AjaxResponse success(int code, String msg) {
        return new AjaxResponse(code, msg, null, null);
    }

    /**
     * 成功
     *
     * @param key  返回值KEY
     * @param data 返回数据
     * @return
     */
    public static AjaxResponse success(String key, Object data) {
        return new AjaxResponse(SUCCESS_CODE, SUCCESS_MSG, key, data);
    }

    /**
     * 失败
     *
     * @return
     */
    public static AjaxResponse failure() {
        return new AjaxResponse(FAILURE_CODE, FAILURE_MSG, null, null);
    }

    /**
     * 失败
     *
     * @param msg 返回消息
     * @return
     */
    public static AjaxResponse failure(String msg) {
        return new AjaxResponse(FAILURE_CODE, msg, null, null);
    }

    /**
     * 失败
     *
     * @param code 状态码
     * @param msg  返回消息
     * @return
     */
    public static AjaxResponse failure(int code, String msg) {
        return new AjaxResponse(code, msg, null,null);
    }

    /**
     * 失败
     *
     * @param key  返回消息
     * @param data 返回数据
     * @return
     */
    public static AjaxResponse failure(String key, Map<String, Object> data) {
        return new AjaxResponse(FAILURE_CODE, null, key, data);
    }

    /**
     * Getter method for property <tt>code</tt>.
     *
     * @return property value of code
     */
    public int getCode() {
        return code;
    }

    /**
     * Setter method for property <tt>code</tt>.
     *
     * @param code value to be assigned to property code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Getter method for property <tt>msg</tt>.
     *
     * @return property value of msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Setter method for property <tt>msg</tt>.
     *
     * @param msg value to be assigned to property msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * Getter method for property <tt>data</tt>.
     *
     * @return property value of data
     */
    public Object getData() {
        return data;
    }

    /**
     * Setter method for property <tt>data</tt>.
     *
     * @param data value to be assigned to property data
     */
    public void setData(Map<String, Object> data) {
        this.data = data;
    }

}

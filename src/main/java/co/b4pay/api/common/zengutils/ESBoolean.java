package co.b4pay.api.common.zengutils;

import java.util.List;

public class ESBoolean {


    /**
     * 是Null或 ""
     * @param source
     * @return
     */
    public static boolean isEmpty(String source){
        return (source == null || source.trim().isEmpty());
    }

    /**
     * 不为Null和 ""
     * @param source
     * @return
     */
    public static boolean isNotEmpty(String source){
        return (source != null && !source.trim().isEmpty());
    }

    /**
     * Null或 长度=0
     * @param source
     * @return
     */
    public static boolean isEmpty(String[] source){
        return (source == null || source.length == 0);
    }

    /**
     * 非Null 并且 长度>0 返回true
     * @param source
     * @return
     */
    public static boolean isNotEmpty(String[] source){
        return (source != null && source.length > 0);
    }

    /**
     * Null 或 长度=0 返回true
     * @param source
     * @return
     */
    public static boolean isEmpty(List<Object> source){
        return (source == null || source.size() == 0);
    }

    /**
     * 非Null 并且 长度>0 返回true
     * @param source
     * @return
     */
    public static boolean isNotEmpty(List<Object> source){
        return (source != null && source.size() > 0);
    }

    /**
     * 非Null
     * @param source
     * @return
     */
    public static boolean isNotNull(Object source){
        return source != null;
    }

    /**
     * 是Null
     * @param source
     * @return
     */
    public static boolean isNull(Object source){
        return source == null;
    }

}

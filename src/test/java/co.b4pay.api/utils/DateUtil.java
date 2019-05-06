/**
 * http://www.wdb168.com/ Copyright (c) 微贷宝. 2014-2015 All Rights Reserved.
 */
package co.b4pay.api.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author YiKe
 * @version $Id: DateUtil.java, v 0.1 2015年8月10日 下午3:10:28 DongMengYao Exp $
 */
public class DateUtil {

    public static final long ONE_DAY = 86400000l;
    public static final SimpleDateFormat Y = new SimpleDateFormat("yyyy");
    public static final SimpleDateFormat YM = new SimpleDateFormat("yyyy-MM");
    public static final SimpleDateFormat Mdhm = new SimpleDateFormat("MM-dd HH:mm");
    public static final SimpleDateFormat Mdhm_cn = new SimpleDateFormat("M月d日 HH:mm");
    public static final SimpleDateFormat YmdHM_cn = new SimpleDateFormat("yyyy-M月d日 HH:mm");
    public static final SimpleDateFormat yMd = new SimpleDateFormat("yy-MM-dd");
    public static final SimpleDateFormat YMd = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat YMdhmsS_noSpli = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    public static final SimpleDateFormat yMdhmsS_noSpli = new SimpleDateFormat("yyMMddHHmmssSSS");
    public static final SimpleDateFormat YMd_cn = new SimpleDateFormat("yyyy年MM月dd日");
    public static final SimpleDateFormat YMdhm = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat yMdhm = new SimpleDateFormat("yy-MM-dd HH:mm");
    public static final SimpleDateFormat YMdhm_cn = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
    public static final SimpleDateFormat YMdhms_cn = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
    public static final SimpleDateFormat YMdhms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat yMdhms = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat YMdhmsS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final SimpleDateFormat YMdhmsS_cn = new SimpleDateFormat("yyyy-MM-dd E, HH:mm:ss.SSS", Locale.CHINA);
    public static final SimpleDateFormat YMdhmsS_en = new SimpleDateFormat("yyyy-MM-dd E, HH:mm:ss.SSS", Locale.ENGLISH);
    public static final SimpleDateFormat YMdhms_noSpli = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat YMdhm_noSpli = new SimpleDateFormat("yyyyMMddHHmm");
    public static final SimpleDateFormat Emdhmszy_us = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
    public static final SimpleDateFormat YMd_noSpli = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat YMdhm_con = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
    public static final SimpleDateFormat Md = new SimpleDateFormat("MM月dd日");
    public static final SimpleDateFormat HHmmss = new SimpleDateFormat("HHmmss");

    public static Date strToDate(String str, SimpleDateFormat sdf) {
        if (StringUtil.isBlank(str)) {
            return null;
        }
        Date date = null;
        try {
            date = sdf.parse(str);
        } catch (Exception e) {
            throw new RuntimeException("DateParse or DateFormat Error!", e);
        }
        return date;
    }

    public static String dateToStr(Date date, SimpleDateFormat df) {
        String str = null;
        if (date == null) {
            return str;
        }
        try {
            str = df.format(date);
        } catch (Exception e) {
            throw new RuntimeException("DateFormat Error!", e);
        }
        return str;
    }

}

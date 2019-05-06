package co.b4pay.api.common.zengutils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * 日期类
 * @author 钟展峰
 *
 * 2015年8月25日
 */
public class ESDate {

    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static SimpleDateFormat timeForamt = new SimpleDateFormat("HH:mm:ss");

    private static SimpleDateFormat dateTimeNumberFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private static SimpleDateFormat dateNumberFormat = new SimpleDateFormat("yyyyMMdd");

    /**
     * 将字符串根据自定义转换规则转成日期格式
     * @param source
     * @return
     */
    public static Date parseDate(String source, String pattern){
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将yyyyMMddHHmmss转换成日期对象
     * @param source
     * @return
     */
    public static Date parseDateFromNumber(String source){
        try {
            return dateTimeNumberFormat.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date parseDateTimeString(String source){
        try {
            return dateTimeFormat.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }



    /**
     * 获取时间戳
     * @return
     */
    public static String getTimeStamp(){
        return String.valueOf(new Date().getTime());
    }

    /**
     * yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static String getDateTime(){
        return dateTimeFormat.format(new Date());
    }

    /**
     * yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static String getDateTime(Date date){
        return dateTimeFormat.format(date);
    }

    /**
     * yyyyMMddHHmmss
     * @return
     */
    public static String getDateTimeNumber(){
        return dateTimeNumberFormat.format(new Date());
    }

    /**
     * yyyyMMddHHmmss
     * @return
     */
    public static String getDateTimeNumber(Date date){
        return dateTimeNumberFormat.format(date);
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss 转换成 yyyyMMddHHmmss
     * @return
     */
    public static String getDateTimeNumber(String source){
        try {
            Date parse = dateTimeFormat.parse(source);
            return dateTimeNumberFormat.format(parse);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将 yyyyMMddHHmmss 转换成 yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static String getDateTimeFormat(String source){
        try {
            Date parse = dateTimeNumberFormat.parse(source);
            return dateTimeFormat.format(parse);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * yyyyMMdd
     * @return
     */
    public static String getDateNumber(){
        return dateNumberFormat.format(new Date());
    }

    /**
     * yyyy-MM-dd
     * @return
     */
    public static String getDate(){
        return dateFormat.format(new Date());
    }

    /**
     * yyyy-MM-dd
     * @return
     */
    public static String getDate(Date source){
        return dateFormat.format(source);
    }

    /**
     * 获取当前日期的HH:mm:ss
     * @return
     */
    public static String getTime(){
        return timeForamt.format(new Date());
    }

    /**
     * 获取日期的HH:mm:ss
     * @return
     */
    public static String getTime(Date source){
        return timeForamt.format(source);
    }

    /**
     * 在指定的日期加上second秒
     * @param source
     * @param second
     * @return
     */
    public static Date addSecond(Date source,int second){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(source);
        calendar.add(Calendar.SECOND, second);
        return calendar.getTime();
    }

    /**
     * 格式转换，比replace方法高效10倍
     * @param date yyyy-MM-dd或yyyy-MM-dd HH:mm:ss
     * @return yyyyMMdd或yyyyMMddHHmmss
     */
    public static String dateToNumber(String date){
        char[] arr;
        if(date.length()==10){
            arr = new char[8];
        }else if(date.length()==19){
            arr = new char[14];
        }else{
            return date;
        }
        arr[0] = date.charAt(0);	// 年，第1位
        arr[1] = date.charAt(1);	// 年，第2位
        // 从“年”最后两位开始，每取两位会遇到一个分隔符
        for (int i = 2, c=0; i < arr.length; i+=2,c++) {
            arr[i] = date.charAt(i+c);
            arr[i+1] = date.charAt(i+1+c);
        }
        return new String(arr);
    }

    /**
     * 获取信用卡有效期年份MAP，当前年份开始的10年
     * @return
     */
    public static LinkedHashMap<String, String> getCreditYears(){
        LinkedHashMap<String, String> years = new LinkedHashMap<>(10);
        Calendar calendar = Calendar.getInstance();
        int begin = calendar.get(Calendar.YEAR);
        for (int i = 0; i < 10; i++) {
            String year = Integer.toString(begin+i);
            years.put(year, year.substring(2));
        }
        return years;
    }

    /**
     * 获取昨天日期 yyyy-mm-dd
     * @return
     */
    public static String getYesterday(){
        return getDateWithNumber(-1);
    }

    /**
     * 获取当前日期上下浮动的天对应的日期  <br>
     * today=2016-11-11   dayNumber=-1  return 2016-11-10
     * @param dayNumber  浮动的天数
     * @return
     */
    public static String getDateWithNumber(int dayNumber){
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(System.currentTimeMillis());
        c1.add(Calendar.DATE, dayNumber);
        String str = dateFormat.format(c1.getTime());
        try {
            str = dateFormat.format(dateFormat.parse(str));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return str;
    }

}
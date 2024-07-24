package org.nerve.boot.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类, 继承org.apache.commons.lang.time.DateUtils类
 */
public class DateUtil extends org.apache.commons.lang3.time.DateUtils {

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    /**
     * yyyy-MM-dd
     */
    public static final String DATE = "yyyy-MM-dd";
    public static final String TIME = "HH:mm:ss";

    private static String[] parsePatterns = { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm" };

    /**
     * @return      得到当前日期字符串 格式（yyyy-MM-dd）
     */
    public static String getDate() {
        return getDate(DATE);
    }

    /**
     * 得到当前日期字符串 格式（yyyy-MM-dd） pattern可以为："yyyy-MM-dd" "HH:mm:ss" "E"
     * @param pattern       格式
     * @return              格式化后的当前日期
     */
    public static String getDate(String pattern) {
        return DateFormatUtils.format(new Date(), pattern);
    }

    /**
     * 得到日期字符串 默认格式（yyyy-MM-dd） pattern可以为："yyyy-MM-dd" "HH:mm:ss" "E"
     * @param date              data
     * @param pattern           格式
     * @return                  格式化后的日期
     */
    public static String formatDate(Date date, Object... pattern) {
        String formatDate = null;
        if (pattern != null && pattern.length > 0 && pattern[0] != null) {
            formatDate = DateFormatUtils.format(date, pattern[0].toString());
        } else {
            formatDate = DateFormatUtils.format(date, DATE);
        }
        return formatDate;
    }

    /**
     * 得到日期时间字符串，转换格式（yyyy-MM-dd HH:mm:ss）
     * @param date  日期
     * @return      结果
     */
    public static String formatDateTime(Date date) {
        return formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 得到当前时间字符串 格式（HH:mm:ss）
     * @return  结果
     */
    public static String getTime() {
        return formatDate(new Date(), "HH:mm:ss");
    }

    /**
     * 得到当前日期和时间字符串 格式（yyyy-MM-dd HH:mm:ss）
     * @return  结果
     */
    public static String getDateTime() {
        return formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 获取格式为 yyyyMMddHHmmss 的日期+时间字符串
     * @return
     */
    public static String getDateTimeSimple(){
        return formatDate(new Date(), "yyyyMMddHHmmss");
    }

    /**
     * 得到当前年份字符串 格式（yyyy）
     * @return  结果
     */
    public static String getYear() {
        return formatDate(new Date(), "yyyy");
    }

    /**
     * 得到当前月份字符串 格式（MM）
     * @return  结果
     */
    public static String getMonth() {
        return formatDate(new Date(), "MM");
    }

    /**
     * 得到当天字符串 格式（dd）
     * @return  结果
     */
    public static String getDay() {
        return formatDate(new Date(), "dd");
    }

    /**
     * 得到当前星期字符串 格式（E）星期几
     * @return  结果
     */
    public static String getWeek() {
        return formatDate(new Date(), "E");
    }

    /**
     * 日期型字符串转化为日期 格式{ "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm" }
     * @param str   日期（String）
     * @return      结果日期
     */
    public static Date parseDate(Object str) {
        if (str == null){
            return null;
        }
        try {
            return parseDate(str.toString(), parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date toDate(String str) throws ParseException {
        if (str == null){
            return null;
        }
        return parseDate(str, parsePatterns);
    }

    /**
     * 获取过去的天数
     * @param date  日期
     * @return      天数
     */
    public static long pastDays(Date date) {
        long t = new Date().getTime()-date.getTime();
        return t/(24*60*60*1000);
    }


    public static Date getDateStart(Date date) {
        if(date==null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date= sdf.parse(formatDate(date, DATE)+" 00:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date getDateEnd(Date date) {
        if(date==null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date= sdf.parse(formatDate(date, DATE) +" 23:59:59");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 日期格式为 yyyy-MM-dd
     * @param baseDate
     * @param days
     * @return
     */
    public static String getDateAfter(String baseDate, int days){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parseDate(baseDate));
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return formatDate(calendar.getTime(), DATE);
    }

    public static String getDateAfter(int days){
        return getDateAfter(Calendar.DAY_OF_YEAR, days);
    }

    public static String getDateAfter(int field, int step){
        return getDateAfter(field, step, null);
    }

    public static String getDateAfter(int field, int step, String pattern){
        Calendar calendar = Calendar.getInstance();
        calendar.add(field, step);
        return formatDate(calendar.getTime(), pattern);
    }

    /**
     * 计算两个时间内工作日的天数
     * @param from
     * @param to
     * @return
     */
    public static int getDutyDayBetween(Date from, Date to){
        int days = 0;
        Calendar start = Calendar.getInstance();
        start.setTime(from);
        Calendar end = Calendar.getInstance();
        end.setTime(to);
        while (start.before(end)){
            if(start.get(Calendar.DAY_OF_WEEK)!=Calendar.SUNDAY && start.get(Calendar.DAY_OF_WEEK)!= Calendar.SATURDAY)
                days ++;
            start.add(Calendar.DAY_OF_YEAR, 1);
        }
        return days;
    }
}

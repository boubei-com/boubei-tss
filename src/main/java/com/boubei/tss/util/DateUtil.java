/* ==================================================================   
 * Created [2006-6-19] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018  
 * ================================================================== 
*/
package com.boubei.tss.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * 时间、字符串转换工具
 * 注： SimpleDateFormat为非线程安全。
 * </p>
 */
public class DateUtil {
    
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    private static final String SDF_1_REG = "^\\d{2,4}\\-\\d{1,2}\\-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$";
    private static final String SDF_2_REG = "^\\d{2,4}\\-\\d{1,2}\\-\\d{1,2}$";
    private static final String SDF_3_REG = "^\\d{2,4}\\/\\d{1,2}\\/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$";
    private static final String SDF_4_REG = "^\\d{2,4}\\/\\d{1,2}\\/\\d{1,2}$";
    private static final String SDF_5_REG = "^\\d{2,4}\\/\\d{1,2}\\/\\d{1,2} \\d{1,2}:\\d{1,2}$";
    private static final String SDF_6_REG = "^\\d{2,4}\\-\\d{1,2}\\-\\d{1,2} \\d{1,2}:\\d{1,2}$";

    /**
     * <p>
     * 将日期字符串解析成日期对象，支持以下格式
     * <li>yyyy-MM-dd HH:mm:ss
     * <li>yyyy-MM-dd
     * <li>yyyy/MM/dd HH:mm:ss
     * <li>yyyy/MM/dd
     * <li>yyyy-MM-dd HH:mm
     * <li>yyyy/MM/dd HH:mm
     * </p>
     * 
     * @param str
     * @return
     */
    public static Date parse(String str) {
    	if(EasyUtils.isNullOrEmpty(str)) return null;
    	
    	str = str.trim();
    	if(str.length() > 12 && str.indexOf(".") > 0) {
    		str = str.substring(0, str.indexOf(".")); // 截掉微秒
    	}
    	str = str.replaceAll("\\.", "-"); // yyyy.MM.dd --> yyyy-MM-dd
    	
    	List<String> sdfRegArray = Arrays.asList(SDF_1_REG, SDF_2_REG, SDF_3_REG, SDF_4_REG, SDF_5_REG, SDF_6_REG);
    	
    	SimpleDateFormat SDF_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat SDF_2 = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
        SimpleDateFormat SDF_3 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        SimpleDateFormat SDF_4 = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat SDF_5 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        SimpleDateFormat SDF_6 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    	List<SimpleDateFormat> sdfArray = Arrays.asList(SDF_1, SDF_2, SDF_3, SDF_4, SDF_5, SDF_6);
    	
        Date date = null;
        try {
        	for(int index = 0; index < sdfRegArray.size(); index ++) {
        		 if (Pattern.compile(sdfRegArray.get(index)).matcher(str).matches()) {
                     return sdfArray.get(index).parse(str);
                 } 
        	}
        } catch (ParseException e) {
        }
        return date;
    }

    /**
     * <p>
     * 将日期格式化成字符串：yyyy-MM-dd
     * </p>
     * 
     * @param date
     * @return
     */
    public static String format(Date date) {
        if(date == null) return "";
        
        return new SimpleDateFormat(DateUtil.DEFAULT_DATE_PATTERN).format(date);
    }
    
    /**
     * <p>
     * 将日期格式化成字符串：yyyy-MM-dd HH:mm:ss
     * </p>
     * 
     * @param date
     * @return
     */
    public static String formatCare2Second(Date date) {
        if(date == null) return "";
        
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * <p>
     * 将日期格式化成相应格式的字符串，如：
     * <li>yyyy-MM-dd HH:mm:ss
     * <li>yyyy-MM-dd
     * <li>yyyy/MM/dd HH:mm:ss
     * <li>yyyy/MM/dd
     * </p>
     * 
     * @param date
     * @param pattern
     * @return
     */
    public static String format(Date date, String pattern) {
        if(date == null) return "";
        
        if (pattern == null || "".equals(pattern)) {
            pattern = DateUtil.DEFAULT_DATE_PATTERN; 
        }
        
        return new SimpleDateFormat(pattern).format(date);
    }
    
    /**
     * 获取今天零点。
     */
    public static Date today() {
        return noHMS(new Date());
    }
    
    public static String now() {
    	return formatCare2Second(new Date());
    }
    
    /**
     * 去掉时分秒
     */
    public static Date noHMS(Date pointedDate) {
    	Calendar c = Calendar.getInstance();
    	c.setTime(pointedDate);
    	
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
    
    public static Date addDays(double days) {
    	return addDays(new Date(), days);
    }
    public static Date addDays(Date now, double days) {
        long nowLong = now.getTime(); // 将参考日期转换为毫秒时间
        Date time = new Date(nowLong + (long) (days * 24 * 60 * 60 * 1000)); // 加上时间差毫秒数
        return time;
    }
    
    public static Date subDays(Date now, double days) {
        return addDays(now, days * -1);
    }
    
    public static Date addMonths(int mouth_num) {
    	return addMonths(new Date(), mouth_num);
    }
    public static Date addMonths(Date now, int mouth_num) {
    	Calendar calendar = new GregorianCalendar();
		calendar.add(Calendar.MONTH, mouth_num);
		return calendar.getTime();
    }
    
    public static Date addYears(int year_num) {
    	return addYears(new Date(), year_num);
    }
    public static Date addYears(Date now, int year_num) {
    	Calendar calendar = new GregorianCalendar();
		calendar.add(Calendar.YEAR, year_num);
		return calendar.getTime();
    }
    
	public static List<Date> daysBetweenFromAndTo(Date fromDate, Date toDate) {
		List<Date> rltList = new ArrayList<Date>();
		while (fromDate.before(toDate)) {
			rltList.add(fromDate);
			fromDate = addDays(fromDate, 1);
		}
		rltList.add(toDate);

		return rltList;
	}
	
    public static int getYear(Date now) {
    	Calendar pointedDay = Calendar.getInstance();
    	pointedDay.setTime(now);
        return pointedDay.get(Calendar.YEAR);
    }
    
    public static int getMonth(Date now) {
    	Calendar pointedDay = Calendar.getInstance();
    	pointedDay.setTime(now);
    	return pointedDay.get(Calendar.MONTH) + 1; // 获取月份，0表示1月份
    }
    
    public static int getDay(Date now) {
    	Calendar pointedDay = Calendar.getInstance();
    	pointedDay.setTime(now);
        return pointedDay.get(Calendar.DAY_OF_MONTH);
    }
    
    public static int getDayOfWeek(Date now) {
    	Calendar pointedDay = Calendar.getInstance();
    	pointedDay.setTime(now);
        int day = pointedDay.get(Calendar.DAY_OF_WEEK) - 1; // 星期天是1，星期六是7
		return day == 0 ? 7 : day;
    }
    
    public static int getHour(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }
    
    public static int getMinute(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }
    
    /**
     * 判断是否是月末
     */
    public static boolean isMonthEnd(Date day){
        Calendar c = Calendar.getInstance();
        c.setTime(day);
        return c.get(Calendar.DATE) == c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    public static String toYYYYMM(Object year, int monthOfYear, String seperator){
    	return year + seperator + fixMonth(monthOfYear);
    }
    
    public static String toYYYYMM(Date time, String seperator) {
    	return toYYYYMM(getYear(time), getMonth(time), seperator);
    }
    
    public static String fixMonth(int month) {
    	return  (month < 10 ? "0" : "") + month;
    }
    
    
    /**
     * 将日期的快捷写法，转换成相应日期
     */
    public static String fastCast(String val) {
    	// 将相对时间解析成绝对时间（today - 2 --> 2014-7-20）
		if (Pattern.compile("^today[\\s]*-[\\s]*\\d{1,4}").matcher(val).matches()) {
			int deltaDays = Integer.parseInt(val.split("-")[1].trim());
			Date today = DateUtil.noHMS(new Date());
			return DateUtil.format(DateUtil.subDays(today, deltaDays));
		} 
		
		// 将相对时间解析成绝对时间（today + 2 --> 2014-7-24）
		if (Pattern.compile("^today[\\s]*\\+[\\s]*\\d{1,4}").matcher(val).matches()) {
			int deltaDays = Integer.parseInt(val.split("\\+")[1].trim());
			Date today = DateUtil.noHMS(new Date());
			return DateUtil.format(DateUtil.addDays(today, deltaDays));
		} 
		
		Date now = new Date();
		if ( "cur_year_01".equals(val) ) {
			return DateUtil.getYear(now) + "-01-01";
		}
		
		if ( "cur_month_01".equals(val) ) {
			return DateUtil.toYYYYMM(now, "-") + "-01";
		}
		
		if ( "cur_week_01".equals(val) ) {
			int deltaDays = DateUtil.getDayOfWeek(now) - 1;
			return DateUtil.format(DateUtil.subDays(now, deltaDays));
		}
		
		return val;
    }
}

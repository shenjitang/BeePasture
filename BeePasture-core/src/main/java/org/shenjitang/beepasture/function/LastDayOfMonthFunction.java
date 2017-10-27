/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.NumberUtils;
import org.beetl.core.Context;
import org.beetl.core.Function;
import org.joda.time.LocalDate;

/**
 *
 * @author xiaolie
 */
public class LastDayOfMonthFunction  implements Function {

    @Override
	public Date call(Object[] paras, Context ctx) {
        try {
            Date date = new Date();
            if (paras.length > 0) {
                Object inTime = paras[0];
                if (inTime instanceof Date) {
                    date = (Date)inTime;
                } else {
                    date = toDate(inTime.toString());
                }
            }
            return doIt(date);
        } catch (ParseException e) {
            throw new RuntimeException("日期格式错误", e);
        }
	}
    
    private static Date toDate(String inTimeStr) throws ParseException {
        String dateStr = null;
        if (NumberUtils.isNumber(inTimeStr) && inTimeStr.length() >= 6) {
            dateStr = inTimeStr.substring(0, 4) + "-" + inTimeStr.substring(4, 6) + "-01";
        } else {
            Pattern pattern = Pattern.compile("[0-9][0-9][0-9][0-9]");
            Matcher matcher = pattern.matcher(inTimeStr);
            String y = null;
            if (matcher.find()) {
                y = matcher.group();
                inTimeStr = inTimeStr.replace(y, "");
            }
            if (y == null) {
                y = Integer.toString((new LocalDate()).getYear());
            }
            pattern = Pattern.compile("[0-9][0-9]?");
            matcher = pattern.matcher(inTimeStr);
            String m = null;
            if (matcher.find()) {
                m = matcher.group();
                if (m.length() == 1) {
                    m = "0" + m;
                }
            }
            dateStr = y + "-" + m + "-01";
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.parse(dateStr);
    }
    
    private static Date doIt(Date date) throws ParseException {
        LocalDate monthLocalDate = (new LocalDate(date)).plusMonths(1);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        String sLastMonth = format.format(monthLocalDate.toDate()) + "-01";
        Date lastMonthDate = format.parse(sLastMonth);
        monthLocalDate = (new LocalDate(lastMonthDate)).plusDays(-1);
        return monthLocalDate.toDate();
    }
    
    public static void main(String[] args) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println(format.format(LastDayOfMonthFunction.toDate("r2015年12月")));
        System.out.println(format.format(LastDayOfMonthFunction.toDate("2015年12月5日")));
        System.out.println(format.format(LastDayOfMonthFunction.toDate("2015年1月")));
        System.out.println(format.format(LastDayOfMonthFunction.toDate("2015年01月5日")));
        System.out.println(format.format(LastDayOfMonthFunction.toDate("201511")));
        System.out.println(format.format(LastDayOfMonthFunction.toDate("20151123")));
        Date now = format.parse("2017-03-3");
        System.out.println(format.format(LastDayOfMonthFunction.doIt(now)));
        System.out.println(format.format(LastDayOfMonthFunction.doIt(LastDayOfMonthFunction.toDate("2015年7月全国"))));
    }
}

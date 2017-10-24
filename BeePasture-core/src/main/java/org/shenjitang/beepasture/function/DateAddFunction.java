/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.beetl.core.Context;
import org.beetl.core.Function;
import org.joda.time.LocalDate;

/**
 *
 * @author xiaolie
 */
public class DateAddFunction  implements Function {

    @Override
	public Date call(Object[] paras, Context ctx) {
        Long nowTime = System.currentTimeMillis();
        String dayoffset = "0";
        if (paras.length < 1) {
            throw new RuntimeException("dateadd Error,Args (Integer dayoffset, Sting format)  or (Date date, Integer dayoffset, String format) ");
        } else if (paras.length == 1) {
            dayoffset = paras[0].toString().trim().toLowerCase();
        } else if (paras.length == 2) {
            if (paras[0] instanceof Date) {
                nowTime = ((Date)paras[0]).getTime();
            } else {
                nowTime = Long.valueOf(paras[0].toString());
            }
            dayoffset = paras[1].toString().trim().toLowerCase();
        } else {
            throw new RuntimeException("dateadd Error,Args (Integer dayoffset, Sting format)  or (Date date, Integer dayoffset, String format) ");
        }
        return doIt(nowTime, dayoffset);        
	}
    
    private static Date doIt(Long nowTime, String dayoffset) {
        LocalDate now = new LocalDate(nowTime);
        LocalDate result = null;
        if (dayoffset.endsWith("m")) {
            int months = Integer.valueOf(dayoffset.substring(0, dayoffset.length() - 1));
            result = now.plusMonths(months);
        } else if (dayoffset.endsWith("y")) {
            int years = Integer.valueOf(dayoffset.substring(0, dayoffset.length() - 1));
            result = now.plusYears(years);
        } else if (dayoffset.endsWith("d")) {
            int days = Integer.valueOf(dayoffset.substring(0, dayoffset.length() - 1));
            result = now.plusDays(days);
        } else {
            int days = Integer.valueOf(dayoffset);
            result = now.plusDays(days);
        }
        return result.toDate();
    }
    
    public static void main(String[] args) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date now = format.parse("2017-03-31");
        System.out.println(format.format(DateAddFunction.doIt(now.getTime(), "-1d")));
        System.out.println(format.format(DateAddFunction.doIt(now.getTime(), "-1m")));
        System.out.println(format.format(DateAddFunction.doIt(now.getTime(), "-1y")));
        System.out.println(format.format(DateAddFunction.doIt(now.getTime(), "-1")));
    }
}

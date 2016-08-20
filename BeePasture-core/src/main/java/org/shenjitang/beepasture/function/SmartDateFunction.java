/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 *
 * @author xiaolie
 */
public class SmartDateFunction  implements Function {

    @Override
	public Date call(Object[] paras, Context ctx) {
        //Long nowTime = System.currentTimeMillis();
        Date defaultDate = null;
        String str = null;
        if (paras.length < 1) {
            throw new RuntimeException("smartDate Error,Args (String datestr, Sting format) ");
        } else if (paras.length == 1) {
            str = paras[0].toString();
        } else if (paras.length == 2) {
            defaultDate = (Date)paras[1];
        } else {
            throw new RuntimeException("dateadd Error,Args (Integer dayoffset, Sting format)  or (Date date, Integer dayoffset, String format) ");
        }
        try {
            Long t = Long.valueOf(str);
            return new Date(t);
        } catch (Exception e) {}
        
        if (str.contains("刚刚") || str.contains("刚才") || str.contains("当前") || str.toLowerCase().contains("now") ) {
            return new Date();
        }
        if (str.indexOf("分钟前") > 0) {
            long n = Long.valueOf(str.substring(0, str.indexOf("分钟前")).trim());
            return new Date(System.currentTimeMillis() - (n*60000L));
        }
        if (str.indexOf("小时前") > 0) {
            long n = Long.valueOf(str.substring(0, str.indexOf("小时前")).trim());
            return new Date(System.currentTimeMillis() - (n*60*60000L));
        }
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        try {
            return format.parse(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultDate;
	}
    
    public Date toDate(String str, Date defaultDate) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        List<Integer> numberList = new ArrayList();
        List<String> strList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        Boolean inNumber = null;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= '0' && c <= 9) { //是数字
                if (inNumber == null) {
                    inNumber = true;
                } else if (!inNumber) {
                    inNumber = true;
                    strList.add(sb.toString());
                    sb.delete(0, sb.length());
                }
                sb.append(c);
            } else { //是字符
                if (inNumber != null) {
                    if (inNumber) {
                        inNumber = false;
                        numberList.add(Integer.valueOf(sb.toString()));
                        sb.delete(0, sb.length());
                    }
                    sb.append(c);
                }
            }
        }
        if (inNumber) {
            numberList.add(Integer.valueOf(sb.toString()));
        } else {
            strList.add(sb.toString());
        }
        if (numberList.isEmpty()) {
            return defaultDate;
        }
        //只有一个数字的情况
        if (numberList.size() == 1) {
            Integer value = numberList.get(0);
            //只有一个年份
            if (value > 1000 && value < 10000) {
                //String s = value + "-01-01";
                SimpleDateFormat format = new SimpleDateFormat("yyyy");
                return format.parse(value.toString());
            }
            //年月日：20140304
            if (value > 10000000 && value < 100000000) {
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                return format.parse(value.toString());
            }
            return defaultDate;
        }
        int year = calendar.get(Calendar.YEAR);
        int month = 0;
        int day = 0;
        int hour = 0; 
        int min = 0;
        int sec = 0;
        Integer value = numberList.get(0);
        if (value > 1000 && value < 10000) {
            year = value;
            //if ()
        }
        
        
        Pattern p = Pattern.compile("[^0-9]");
        Matcher m = p.matcher(str);
        String str1 = m.replaceAll("-");
        String[] strArray = str1.split("-");
        Integer[] dateElements = new Integer[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            dateElements[i] = Integer.valueOf(strArray[i]);
        }
        int j = 0;
        if (dateElements[j] > 1000) {
            calendar.set(Calendar.YEAR, dateElements[j]);
            j++;
        }
        if (dateElements[j] >= 0 && dateElements[j] <= 12) {
            calendar.set(Calendar.MONTH, dateElements[j]);
        }
        return new Date(calendar.getTimeInMillis());
    }
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author xiaolie
 */
public class StringFunctions {
    public static String trim(String str) {
        if (StringUtils.isNotEmpty(str)) {
            return str.trim();
        } else {
            return str;
        }
    }
    
    public static String now(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date());
    }
    
    public static String substring(String str, int beginIndex) {
        if (StringUtils.isNotEmpty(str)) {
            return str.substring(beginIndex);
        } else {
            return str;
        }
    }

    public static String substring(String str, int beginIndex, int endIndex) {
        if (StringUtils.isNotEmpty(str)) {
            return str.substring(beginIndex, endIndex);
        } else {
            return str;
        }
    }

    public static int indexOf(String str, String indexOfStr) {
        return str.indexOf(indexOfStr);
    }
    
    public static String unicode2str(String str) {
        StringBuilder sb = new StringBuilder();
        int i = -1;  
        int pos = 0;  

        while((i=str.indexOf("\\u", pos)) != -1){  
            sb.append(str.substring(pos, i));  
            if(i+5 < str.length()){  
                pos = i+6;  
                sb.append((char)Integer.parseInt(str.substring(i+2, i+6), 16));  
            }  
        }  
        return sb.toString();  
    }
    
    public static Date smartDate(String str) {
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
            return null;
        }
    }
}

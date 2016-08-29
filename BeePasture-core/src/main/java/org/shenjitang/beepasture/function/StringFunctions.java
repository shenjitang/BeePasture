/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    public static String replaceAll(String str, String src, String dest) {
        return str.replaceAll(src, dest);
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
    
    public static String urlEncod(String str, String charset) throws Exception {
        if (StringUtils.isNotBlank(str)) {
            return URLEncoder.encode(str, charset);
        } else {
            return str;
        }
    }
    
    public static String urlDecode(String str, String charset) throws Exception {
        if (StringUtils.isNotBlank(str)) {
            return URLDecoder.decode(str, charset);
        } else {
            return str;
        }
    }
    
    public static String scheme(String str) {
        URI uri = URI.create(str);
        return uri.getScheme();
    }
    
    public static String authority(String str) {
        URI uri = URI.create(str);
        return uri.getAuthority();
    }

    public static String host(String str) {
        URI uri = URI.create(str);
        return uri.getHost();
    }

    public static Integer port(String str) {
        URI uri = URI.create(str);
        return uri.getPort();
    }
    public static String queryStr(String str) {
        URI uri = URI.create(str);
        return uri.getQuery();
    }
    public static Map query(String str) {
        Map map = new HashMap();
        URI uri = URI.create(str);
        String query = uri.getQuery();
        if (StringUtils.isNotBlank(query)) {
            String[] querys = query.split("&");
            for (String q : querys) {
                String[] qc = q.split("=");
                map.put(qc[0], qc[1]);
            }
        }
        return map;
    }
    
    public static String regex(String str, String regex, int n) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(n);
        } else {
            return str;
        }        
    }
    public static String regex(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return str;
        }
    }
    
    public static String[] split(String str) {
        return str.split(" ");
    }
    
    public static String[] split(String str, String regex) {
        return str.split(regex);
    }

    public static String[] split(String str, String regex, int limit) {
        return str.split(regex, limit);
    }
}

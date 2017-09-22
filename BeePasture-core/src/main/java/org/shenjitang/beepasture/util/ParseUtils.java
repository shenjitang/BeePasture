/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.util;

import com.google.common.collect.Lists;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author xiaolie
 */
public class ParseUtils {
    
    public static String correctDateStr(String str) {
        StringBuilder sb = new StringBuilder();
        int numCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') { //是数字
                numCount++;
            } else { //是字符
                if (numCount == 1) {
                    sb.insert(sb.length() - 1, '0');
                }
                numCount = 0;
            }
            sb.append(c);
        }
        if (numCount == 1) {
            sb.insert(sb.length() - 1, '0');
        }
        return sb.toString();
    }  
    
    
    public static boolean maybeScript(Object str) {
        return str != null && str instanceof String && str.toString().contains("${");
    }

    public static List toList(Object page) {
        return page instanceof List ? (List) page : Lists.newArrayList(page);
    }
    
    public static List string2list1(String value, int idx) {
        List list = new ArrayList();
        String s1 = value.substring(0, idx).trim();
        String s2 = value.substring(idx).trim();
        String[] be = s1.split("[.][.]");
        Integer begin = Integer.valueOf(be[0]);
        Integer end = Integer.valueOf(be[1]);
        if (end >= begin) {
            for (int i = begin; i <= end; i++ ) {
                String r = s2.replaceAll("\\$\\{i\\}", i+"");
                list.add(r);
            }
        } else {
            for (int i = begin; i >= end; i-- ) {
                String r = s2.replaceAll("\\$\\{i\\}", i+"");
                list.add(r);
            }
        }
        return list;
    }

    public static List string2list2(String value, int idx) {
        List list = new ArrayList();
        String s1 = value.substring(0, idx).trim();
        String s2 = value.substring(idx).trim();
        String[] be = s1.split("[.][.]");
        char begin = be[0].charAt(0);
        char end = be[1].charAt(0);
        if (end >= begin) {
            for (char i = begin; i <= end; i++ ) {
                String r = s2.replaceAll("\\$\\{i\\}", i+"");
                list.add(r);
            }
        } else {
            for (char i = begin; i >= end; i-- ) {
                String r = s2.replaceAll("\\$\\{i\\}", i+"");
                list.add(r);
            }
        }
        return list;
    }    

    private static final DecimalFormat formatN2 = new DecimalFormat("00");
    private static final DecimalFormat formatN4 = new DecimalFormat("0000");
    private static final Pattern DPATTERN1 =  Pattern.compile("[0-9]+\\.\\.[0-9]+");
    private static final Pattern DPATTERN2 =  Pattern.compile("[a-zA-Z]\\.\\.[a-zA-Z]");

    
    public static Map replaceByArray (Map map) throws Exception {
        for (Object varName : map.keySet()) {
            Object value = map.get(varName);
            if (value instanceof Map) {
                replaceByArray ((Map)value);
            } else if (value instanceof List) {
                replaceByArray ((List)value);
            } else if (value instanceof String) {
                Matcher m = DPATTERN1.matcher((String)value);
                if (m.find()) {
                    List list = string2list1((String)value, m.end());
                    map.put(varName, list);
                }
                m = DPATTERN2.matcher((String)value);
                if (m.find()) {
                    List list = string2list2((String)value, m.end());
                    map.put(varName, list);
                }
            }
        }
        return map;
    }
    
    public static void replaceByArray (List list) throws Exception {
        //ArrayList newList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            if (value instanceof Map) {
                replaceByArray ((Map)value);
            } else if (value instanceof List) {
                replaceByArray ((List)value);
            } else if (value instanceof String) {
                Matcher m = DPATTERN1.matcher((String)value);
                if (m.find()) {
                    List newList = string2list1((String)value, m.end());
                    list.remove(i);
                    list.addAll(i, newList);
                    i = i - 1 +  newList.size();
                }
                m = DPATTERN2.matcher((String)value);
                if (m.find()) {
                    List newList = string2list2((String)value, m.end());
                    list.remove(i);
                    list.addAll(i, newList);
                    i = i - 1 + list.size();
                }
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        /*
        System.out.println(correctDateStr("2013-7-23 8:12:5"));
        System.out.println(correctDateStr("a2013-7-23 8:12:5"));
        System.out.println(correctDateStr("a2013-7-23 8:12:5b"));
         System.out.println(correctDateStr("a2013-7-23 8:12:53b"));
        SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        Date d = format.parse(correctDateStr("05/Aug/2016:03:00:28 +0800"));
        System.out.println(d);
*/
    }

}

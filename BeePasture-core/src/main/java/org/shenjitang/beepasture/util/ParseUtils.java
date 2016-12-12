/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.util;

import java.util.ArrayList;
import java.util.List;

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
        if (page instanceof List) {
            return (List) page;
        } else {
            List pages = new ArrayList();
            pages.add(page);
            return pages;
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.util.Map;

/**
 *
 * @author xiaolie
 */
public class ResourceUtils {
    public static String get(Map map, String key, String def) {
        String value = (String)map.get(key);
        if (value == null) {
            value = def;
        }
        return value;
    }
    
    public static String substringHead(String str, String chars) {
        int idx = str.indexOf(chars);
        if (idx >= 0) {
            return str.substring(0, idx);
        } else {
            return str;
        }
    }
    
    public static String substringTail(String str, String chars) {
        int idx = str.lastIndexOf(chars);
        if (idx >= 0) {
            return str.substring(idx + 1);
        } else {
            return str;
        }
    }
    
    public static String getMiddle(String str, String beginChar, String endChar) {
        int idx1 = str.indexOf(beginChar);
        if (idx1< 0) {
            return null;
        }
        String sub1 = str.substring(idx1 + 1);
        int idx2 = sub1.indexOf(endChar);
        if (idx2 < 0) {
            return null;
        }
        return sub1.substring(0, idx2);
    }
    

}

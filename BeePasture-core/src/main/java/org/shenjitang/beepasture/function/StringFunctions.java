/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

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
}

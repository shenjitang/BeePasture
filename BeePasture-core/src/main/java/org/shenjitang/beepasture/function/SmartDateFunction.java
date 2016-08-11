/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 *
 * @author xiaolie
 */
public class SmartDateFunction  implements Function {

    @Override
	public Date call(Object[] paras, Context ctx) {
        Long nowTime = System.currentTimeMillis();
        String str = null;
        if (paras.length < 1) {
            throw new RuntimeException("smartDate Error,Args (String datestr, Sting format) ");
        } else if (paras.length == 1) {
            str = paras[0].toString();
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
        return null;
	}
    
}

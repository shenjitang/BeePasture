/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.util.Date;
import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 *
 * @author xiaolie
 */
public class DateAddFunction  implements Function {

    @Override
	public Date call(Object[] paras, Context ctx) {
        Long nowTime = System.currentTimeMillis();
        Integer dayoffset = 0;
        if (paras.length < 1) {
            throw new RuntimeException("dateadd Error,Args (Integer dayoffset, Sting format)  or (Date date, Integer dayoffset, String format) ");
        } else if (paras.length == 1) {
            dayoffset = Integer.valueOf(paras[0].toString());
        } else if (paras.length == 2) {
            if (paras[0] instanceof Date) {
                nowTime = ((Date)paras[0]).getTime();
            } else {
                nowTime = Long.valueOf(paras[0].toString());
            }
            dayoffset = Integer.valueOf(paras[1].toString());
        } else {
            throw new RuntimeException("dateadd Error,Args (Integer dayoffset, Sting format)  or (Date date, Integer dayoffset, String format) ");
        }
        Long day = nowTime + dayoffset * 24 * 3600000L;
        return new Date(day);
	}
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.study;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author xiaolie
 */
public class TestDateFormat {
    public static void main(String[] args) throws ParseException {
        test1();
    }
    
    public static void test1() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d");
        Date d = format.parse("2017-1-8");
        System.out.println(format.format(d));
        d = format.parse("2017-11-12");
        System.out.println(format.format(d));
        d = format.parse("2017-11-7");
        System.out.println(format.format(d));
        
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.commons.csv;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 *
 * @author xiaolie
 */
public class CSVUtils {
    public String getCSVWithHeads(List list) throws Exception {
        Appendable out = null;
        String[] heads = getHeads(list.get(0));
        //Object[] records = list.toArray();
        StringBuilder sb = new StringBuilder();
        sb.append(CSVFormat.DEFAULT.format(heads));
        for (Object obj : list) {
            sb.append('\n').append(CSVFormat.DEFAULT.format(getValues(heads, obj)));
        }
        return sb.toString();
    }
    
    public String[] getHeads(Object record) {
        if (record instanceof Map) {
            Map map = (Map)record;
            Object[] heads = map.keySet().toArray();
            String[] strHeads = new String[heads.length];
            for (int i = 0; i < heads.length; i++) {
                strHeads[i] = heads[i].toString();
            }
            return strHeads;
        } else {
            PropertyDescriptor[] heads = PropertyUtils.getPropertyDescriptors(record);
            String[] strHeads = new String[heads.length - 1];
            int i = 0;
            for (PropertyDescriptor head : heads) {
                if (!"class".equalsIgnoreCase(head.getName())) {
                    strHeads[i++] = head.getName();
                }
            }
            return strHeads;
        }
    }
    
    public Object[] getValues(String[] heads, Object record) throws Exception {
        Object[] values = new Object[heads.length];
        if (record instanceof Map) {
            Map map = (Map)record;
            for (int i = 0; i < heads.length; i++) {
                values[i] = map.get(heads[i]);
            }
        } else {
            for (int i = 0; i < heads.length; i++) {
                values[i] = PropertyUtils.getProperty(record, heads[i]);
            }
        }
        return values;
    }
    
    
    public static void main(String[] args) throws Exception {
        List list = new ArrayList();
        for (int i = 0; i < 10; i++) {
            Map map = new HashMap();
            map.put("name", "tom" + i);
            map.put("age", 10+i);
            map.put("female", (i % 2 == 0));
            list.add(map);
        }
        CSVUtils utils = new CSVUtils();
        String str = utils.getCSVWithHeads(list);
        System.out.println(str);
        /*
        list = new ArrayList();
        for (int i = 0; i < 10; i++) {
            org.shenjitang.webgather.study.City city = new org.shenjitang.webgather.study.City();
            city.setName("haha" + i);
            city.setTitle("城市" + i);
            list.add(city);
        }
        str = utils.getCSVWithHeads(list);
        System.out.println(str);
                */
    }
}

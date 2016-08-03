/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.StringTemplateResourceLoader;


/**
 *
 * @author xiaolie
 */
public class ScriptTemplateExecuter {
    private static final Log LOGGER = LogFactory.getLog(ScriptTemplateExecuter.class);
    private final StringTemplateResourceLoader resourceLoader = new StringTemplateResourceLoader();		
    private String it;
    public ScriptTemplateExecuter() {
        
    }
    
    public ScriptTemplateExecuter(String it) {
        this.it = it;
    }
    
    public void setIt(String it) {
        this.it = it;
    }
    

    public String expressCalcu(String str, Map params) {
        try {
            Configuration cfg = Configuration.defaultConfiguration();
            GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);

            Long time = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            if (!params.containsKey("time")) {
                params.put("time", time);
            }
            gt.registerFunctionPackage("sys", System.class);
            gt.registerFunctionPackage("str", StringFunctions.class);
            gt.registerFunction("dateAdd", new DateAddFunction());
            //gt.registerFunctionPackage("it", it);
            for (Object key : params.keySet()) {
                Object o = params.get(key);
                if (o != null) {
                    if (o instanceof List && ((List)o).size() == 1) {
                        gt.registerFunctionPackage((String)key, ((List)o).get(0));
                    } else {
                        gt.registerFunctionPackage((String)key, o);
                    }
                }
            }
            Template t = gt.getTemplate(str);
            t.binding(params);
            return t.render();
        } catch (Exception e) {
            LOGGER.warn(str, e);
            return str;
        }
    }
    
    public String expressCalcu(String str, Object it, Map<String, Object> inParams) throws Exception {
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        Map<String, Object> params = new HashMap();
        if (inParams != null) {
            params.putAll(inParams);
        }
        if (it == null) {
            it = "";
        }
        params.put("it", it);
        
        Long time = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        if (!params.containsKey("time")) {
            params.put("time", time);
        }
        gt.registerFunctionPackage("sys", System.class);
        gt.registerFunctionPackage("str", StringFunctions.class);
        gt.registerFunction("dateAdd", new DateAddFunction());
        //gt.registerFunctionPackage("it", it);
        for (String key : params.keySet()) {
            Object o = params.get(key);
            if (o != null) {
                gt.registerFunctionPackage(key, o);
            }
        }
        Template t = gt.getTemplate(str);
        t.binding(params);
        return t.render();
    }
    
    public String expressCalcu(String str, Object it, Object page, Object thisVar, Map<String, Object> inParams) throws Exception {
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        Map<String, Object> params = new HashMap();
        if (inParams != null) {
            params.putAll(inParams);
        }
        if (it == null) {
            it = "";
        }
        params.put("it", it);
        params.put("_page", page);
        params.put("_this",thisVar);
        Long time = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        if (!params.containsKey("time")) {
            params.put("time", time);
        }
        gt.registerFunctionPackage("sys", System.class);
        gt.registerFunctionPackage("str", StringFunctions.class);
        gt.registerFunction("dateAdd", new DateAddFunction());
        //gt.registerFunctionPackage("it", it);
        for (String key : params.keySet()) {
            Object o = params.get(key);
            if (o != null) {
                if (o instanceof List && ((List)o).size() == 1) {
                    gt.registerFunctionPackage(key, ((List)o).get(0));
                } else {
                    gt.registerFunctionPackage(key, o);
                }
            }
        }
        Template t = gt.getTemplate(str);
        t.binding(params);
        return t.render();
    }    
}

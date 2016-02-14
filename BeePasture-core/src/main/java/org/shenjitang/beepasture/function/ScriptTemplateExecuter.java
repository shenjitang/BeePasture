/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.ho.yaml.Yaml;


/**
 *
 * @author xiaolie
 */
public class ScriptTemplateExecuter {
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
    

    public String expressCalcu(String str, Map params) throws Exception {
        Long time = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        if (params == null) {
            params = new HashMap();
        }
        if (!params.containsKey("time")) {
            params.put("time", time);
        }
        if (!params.containsKey("it")) {
            params.put("it", it);
        }
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        gt.registerFunctionPackage("sys", System.class);
        gt.registerFunctionPackage("str", StringFunctions.class);
        gt.registerFunctionPackage("yaml", Yaml.class);
        Template t = gt.getTemplate(str);
        t.binding(params);
        return t.render();
    }
    
    public String expressCalcu(String str, String it, Map params) throws Exception {
        if (params == null) {
            params = new HashMap();
        }
        params.put("it", it);
        
        Long time = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        if (!params.containsKey("time")) {
            params.put("time", time);
        }
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        gt.registerFunctionPackage("sys", System.class);
        gt.registerFunctionPackage("str", StringFunctions.class);
        if (it == null) {
            it = "";
        }
        gt.registerFunctionPackage("it", it);
        Template t = gt.getTemplate(str);
        t.binding(params);
        return t.render();
    }
}

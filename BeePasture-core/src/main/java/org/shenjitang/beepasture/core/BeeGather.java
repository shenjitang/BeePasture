/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.ho.yaml.Yaml;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.component.Component;
import org.shenjitang.beepasture.http.HttpTools;
import org.shenjitang.beepasture.http.PageAnalyzer;
import org.shenjitang.beepasture.resource.ResourceMng;

/**
 *
 * @author xiaolie
 */
public class BeeGather {
    private final HtmlCleaner cleaner = new  HtmlCleaner();  
    private HttpTools httpTools;
    private final ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    private PageAnalyzer pageAnalyzer;
    private Map vars;
    private List gatherStepList;
    private Map persistStep; 
    private Map resources;
    private String gather;
    private final ResourceMng resourceMng = new ResourceMng();
    private static final Log LOGGER = LogFactory.getLog(BeeGather.class);
    

    public String getGather() {
        return gather;
    }

    public void setGather(String gather) {
        this.gather = gather;
    }

    public BeeGather() {
        httpTools = new HttpTools();
        pageAnalyzer = new PageAnalyzer();
    }

    public BeeGather(String yamlString) {
        this.gather = yamlString;
        httpTools = new HttpTools();
        pageAnalyzer = new PageAnalyzer();
    }

     public void init() throws Exception {
        Map route = (Map)Yaml.load(gather);
        LOGGER.debug(route);
        resources = (Map)route.get("resource");
        if (resources != null) {
            resourceMng.init(resources);
        } else {
            resources = new HashMap();
        }
        vars = (Map)route.get("var");
        if (vars == null) {
            vars = new HashMap();
            route.put("var", vars);
        }
        initVars(vars, resources);
        gatherStepList = (List)route.get("gather");
        persistStep = (Map)route.get("persist");        
    }
     
    public boolean containsResource(String name) {
        return resources.containsKey(name);
    }
     
    
    protected List getUrlsFromStepUrl(String url) {
        List urls = null;
        if (vars.containsKey(url)) {
            urls = (List)vars.get(url);
        } else {
            urls = new ArrayList();
            urls.add(url);
        }
        return urls;
    }
    
    protected List loadResource(String url, Map step) throws Exception {
        String scheme = resourceMng.getResourceScheme(url);
        Map param = resourceMng.getResourceParam(url);
        Component component = ResourceMng.createComponent(scheme, resourceMng.getResource(url), param);
        Map loadParam = (Map) step.get("param");
        Object value = component.loadResource(loadParam);
        List list = new ArrayList();
        if (value instanceof Collection) {
            list.addAll((Collection) value);
        } else {
            list.add(value);
        }
        return list;
    }
    
    public Map doGather() throws Exception {
        if (gatherStepList == null) {
            return vars;
        }
        for (Object stepObj : gatherStepList) {
            Map step = (Map) stepObj;
            GatherStep gatherStep = new GatherStep(step, this);
            gatherStep.execute();
            gatherStep.sleep();
        }
        return vars;
    }
    
    protected List getVar(String varName) {
        List var = (List) vars.get(varName);
        if (var == null) {
            var = new ArrayList();
            vars.put(varName, var);
        }
        return var;
    }
    
    protected String getScript(Map step) {
        String templete = (String) step.get("templete");
        if (StringUtils.isBlank(templete)) {
            templete = (String) step.get("template");
        }
        if (StringUtils.isBlank(templete)) {
            templete = (String) step.get("script");
        }
        return templete;
    }
    
    protected void saveTo(Map persistMap) throws Exception {
        if (persistMap != null) {
            persistMap.keySet().stream().forEach((key) -> {
                try {
                    String varName = (String) key;
                    String topVarName = varName.split("[.]")[0];
                    
                    Object objPersist = persistMap.get(key);
                    if (objPersist instanceof String) {
                        String resourceStr = (String)persistMap.get(key);
                        resourceStr = template.expressCalcu(resourceStr, null);
                        if (resourceStr.startsWith("file:\\\\")) {
                            resourceStr = "file://" + resourceStr.substring(7);
                        }
                        if (resourceStr.contains(":")) {
                            Map params = new HashMap();
                            params.put("url", resourceStr);
                            persist(resourceStr, params, varName, vars.get(topVarName));
                        } else {
                            if (resources.containsKey(resourceStr)) {
                                Map resource = (Map)resources.get(resourceStr);
                                persist(resourceStr, resource, varName, vars.get(topVarName));
                            } else {
                                throw new RuntimeException("不支持的persist cmd: " + resourceStr);
                            }
                        }
                    } else if (objPersist instanceof Map) {
                        Map resourceMap = (Map)objPersist;
                        String resourceName = (String)resourceMap.get("resource");
                        Map resource = (Map)resources.get(resourceName);
                        resource.putAll(resourceMap);
                        persist(resourceName, resource, varName, vars.get(topVarName));
                    } else {
                        throw new RuntimeException("不支持的persist: " + objPersist.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    } 
    
    
    public void persist(String resourceName, Map params, String varName, Object obj) throws Exception {
        String urlStr = (String)params.get("url");
        URI uri = URI.create(urlStr);
        String scheme = uri.getScheme();
        Component persistable = ResourceMng.createComponent(scheme, resourceMng.getResource(resourceName), params);
//        Component persistable = (Component)Class.forName(ResourceMng.COMPONENT_PACKAGE + "." + StringUtils.capitalize(scheme) + "Component").newInstance();
//        persistable.setResource(resourceMng.getResource(resourceName));
        persistable.persist(uri, varName, obj);
    }
    
    public void saveTo() throws Exception {
        saveTo(persistStep);
    }
    

 
    public Map getVars() {
        return vars;
    }
    
    private DecimalFormat formatN2 = new DecimalFormat("00");
    private DecimalFormat formatN4 = new DecimalFormat("0000");
    private Pattern DPATTERN1 =  Pattern.compile("[0-9]+\\.\\.[0-9]+");
    private Pattern DPATTERN2 =  Pattern.compile("[a-zA-Z]\\.\\.[a-zA-Z]");

    private Map initVars(Map vars, Map resources) throws Exception{
        replaceByArray(vars);
        return vars;
    }
    
    private void replaceByArray (Map map) throws Exception {
        for (Object varName : map.keySet()) {
            Object value = vars.get(varName);
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
    }
    
    private void replaceByArray (List list) throws Exception {
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

    private List string2list1(String value, int idx) throws Exception {
        List list = new ArrayList();
        String s1 = value.substring(0, idx).trim();
        String s2 = value.substring(idx).trim();
        String[] be = s1.split("[.][.]");
        Integer begin = Integer.valueOf(be[0]);
        Integer end = Integer.valueOf(be[1]);
        for (int i = begin; i <= end; i++ ) {
            Map map = new HashMap();
            map.put("i", i);
            String r = template.expressCalcu(s2, map);
            list.add(r);
        }
        return list;
    }

    private List string2list2(String value, int idx) throws Exception {
        List list = new ArrayList();
        String s1 = value.substring(0, idx).trim();
        String s2 = value.substring(idx).trim();
        String[] be = s1.split("[.][.]");
        char begin = be[0].charAt(0);
        char end = be[1].charAt(0);
        for (char i = begin; i <= end; i++ ) {
            Map map = new HashMap();
            map.put("i", Character.toString(i));
            String r = template.expressCalcu(s2, map);
            list.add(r);
        }
        return list;
    }
    
}

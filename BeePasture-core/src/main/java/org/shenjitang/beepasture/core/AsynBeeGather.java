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
import java.util.concurrent.LinkedTransferQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class AsynBeeGather {
    private final HtmlCleaner cleaner = new  HtmlCleaner();  
    private HttpTools httpTools;
    private PageAnalyzer pageAnalyzer;
    private Map vars;
    private Map queues;
    private Map<String, LinkedTransferQueue> queueMap = new HashMap();
    private List gatherStepList;
    private Map persistStep; 
    private Map resources;
    private String gather;
    private final ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    private final ResourceMng resourceMng = new ResourceMng();
    private final Log log = LogFactory.getLog(AsynBeeGather.class);
    

    public String getGather() {
        return gather;
    }

    public void setGather(String gather) {
        this.gather = gather;
    }

    public AsynBeeGather() {
        httpTools = new HttpTools();
        pageAnalyzer = new PageAnalyzer();
    }

    public AsynBeeGather(String yamlString) {
        this.gather = yamlString;
        httpTools = new HttpTools();
        pageAnalyzer = new PageAnalyzer();
    }

     public void init() throws Exception {
        Map route = (Map)Yaml.load(gather);
        log.debug(route);
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
        queues = (Map)route.get("queue");
        if (queues == null) {
            queues = new HashMap();
            route.put("queue", vars);
        }
        initVars(queues, resources);
        for (Object queueName : queues.keySet()) {
            queueMap.put((String)queueName, new LinkedTransferQueue());
        }
        gatherStepList = (List)route.get("gather");
        persistStep = (Map)route.get("persist");
        //启动异步采集线程
        if (gatherStepList != null) {
            for (Object stepObj : gatherStepList) {
                Map step = (Map) stepObj;
                String queueName = (String) step.get("url");
                if (queues.containsKey(queueName)) { //启动异步采集线程
                    LinkedTransferQueue que = queueMap.get(queueName);
                    GatherJob gatherJob = new GatherJob(step, que, this);
                    Thread th = new Thread((Runnable)gatherJob, "gather-" + queueName);
                    th.start();
                }
            }
        }
        //启动异步保存线程
        if (persistStep != null) {
            for (Object queueName : persistStep.keySet()) {
                if (queues.containsKey(queueName)) { //启动异步保存线程
                    LinkedTransferQueue que = queueMap.get(queueName);
                    PersistJob persistJob = new PersistJob(persistStep.get(queueName), que, this);
                    Thread th = new Thread((Runnable)persistJob, "persist-" + queueName);
                    th.start();
                }
                
            }
        }
        
    }

    public Map doGather() throws Exception {
        if (gatherStepList == null) {
            return vars;
        }
        for (Object stepObj : gatherStepList) {
            Map step = (Map) stepObj;
            String rurl = (String) step.get("url");
            Map heads = (Map) step.get("heads");
            String xpath = (String) step.get("xpath");
            List urls = null;
            if (vars.containsKey(rurl)) {
                urls = (List)vars.get(rurl);
            } else if (resources.containsKey(rurl)) {
                String scheme = resourceMng.getResourceScheme(rurl);
                Map param = resourceMng.getResourceParam(rurl);
                Component component = ResourceMng.createComponent(scheme, resourceMng.getResource(rurl), param);
                Map loadParam = (Map)step.get("param");
                Object value = component.loadResource(loadParam);
                Map saveTo = (Map) step.get("save");
                String to = (String) saveTo.get("to");
                Boolean append = (Boolean)saveTo.get("append");
                if (append == null) {
                    append = true;
                }
                if (!vars.containsKey(to) || !append) {
                    vars.put(to, new ArrayList());
                }
                List toList = (List) vars.get(to);
                if (value instanceof Collection) {
                    toList.addAll((Collection)value);
                } else {
                    toList.add(value);
                }
                continue;
            } else if (queues.containsKey(rurl)) {
                continue;
            } else {
                urls = new ArrayList();
                urls.add(rurl);
            }
            Object oSleep = step.get("sleep");
            Long sleep = null;
            if (oSleep != null) {
                sleep = Long.valueOf(oSleep.toString());
            }
            Object olimit = step.get("limit");
            Long limit = null;
            if (olimit != null) {
                limit = Long.valueOf(olimit.toString());
            }
            String encoding = (String)step.get("encoding");
            
            Boolean html = (Boolean)step.get("html");
            if (html == null) {
                html = true;
            }
            
            Map saveTo = (Map) step.get("save");
            String to = (String) saveTo.get("to");
            Boolean append = (Boolean)saveTo.get("append");
            if (append == null) {
                append = true;
            }
            if (!vars.containsKey(to) || !append) {
                vars.put(to, new ArrayList());
            }
            List resultList = new ArrayList();
            int count = 0;
            for (Object ourl : urls) {
                try {
                    //Map params = new HashMap();
                    //params.put("time", System.currentTimeMillis());
                    String url = template.expressCalcu((String)ourl, null);
                    //String url = (String)ourl;
                    String page = httpTools.doGet(url, heads, encoding);
                    if (StringUtils.isBlank(xpath)) { //如果没有xpath，整个页面放入变量
                        resultList.add(page);
                    } else {
                        List<String> pages = new ArrayList();
                        pages.add(page);
                        String[] pathList = xpath.split(";");
                        if (pathList.length > 1) { //多步骤
                            pages = narrowdown(Arrays.copyOf(pathList, pathList.length - 1), pages);
                            xpath = pathList[pathList.length - 1];
                        }
                        for (String p : pages) {
                            if (xpath.startsWith("json(")) {
                                String jsonPath = xpath.substring(5, xpath.length() - 1);
                                Object o = JsonPath.read(p, jsonPath);
                                if (o instanceof List) {
                                    resultList.addAll((List)o);
                                } else {
                                    resultList.add(o);
                                }
                            } else if (xpath.startsWith("express(")) {
                                String express = xpath.substring(8, xpath.length() - 1);
                                Map ps = new HashMap();
                                ps.put("time", System.currentTimeMillis());
                                ps.put("page", p);
                                String r = template.expressCalcu(express, ps);
                                resultList.add(r);
                            } else {
                                TagNode node = pageAnalyzer.toTagNode(p);
                                //String text = node.getText().toString();
                                //List list1 = node.getAllElementsList(true);
                                Map propertyMap = (Map) saveTo.get("property");
                                List list = getList(node, xpath, propertyMap);
                                resultList.addAll(list);
                            }
                        }
                    }
                    if (sleep != null) {
                        Thread.sleep(sleep);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (limit != null && ++count >= limit) {
                    break;
                }
            }
            List toList = (List) vars.get(to);
            String templete = (String)step.get("templete");
            if (StringUtils.isBlank(templete)) {
                toList.addAll(resultList);
            } else {
                for (Object item : resultList) {
                    try {
                        Map map = new HashMap();
                        map.put("page", item);
                        String s = template.expressCalcu(templete, map);
                        toList.add(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            //保存
            //Map persistMap = (Map) step.get("persist");
            //saveTo(persistMap);
        }
        return vars;
    }
    
    protected void saveTo(Map persistMap) throws Exception {
        if (persistMap != null) {
            for (Object key : persistMap.keySet())  {
                if (queues.containsKey(key)) {
                    continue;
                }
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
            }
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
    

    private List<String> narrowdown(String[] pathList, List<String> pages) throws Exception {
        for (String path : pathList) {
            pages = narrowdownOnce(path, pages);
        }
        return pages;
    }
    
    private List<String> narrowdownOnce(String path, List<String> pages) throws Exception {
        List<String> rlist = new ArrayList();
        for (String page : pages) {
            List list = narrowdownOnceOnePage(path, page);
            rlist.addAll(list);
        }
        return rlist;
    }

    private List<String> narrowdownOnceOnePage(String xpath, String page) throws Exception {
        List<String> rlist = new ArrayList();
        if (xpath.startsWith("json(")) {
            String jsonPath = xpath.substring(5, xpath.length() - 1);
            String s = JsonPath.read(page, jsonPath);
            rlist.add(s);
            return rlist;
        } else if (xpath.startsWith("express(")) {
            String express = xpath.substring(8, xpath.length() - 1);
            Map ps = new HashMap();
            ps.put("time", System.currentTimeMillis());
            ps.put("page", page);
            String r = template.expressCalcu(express, ps);
            rlist.add(r);
            return rlist;
        } else {
            TagNode node = pageAnalyzer.toTagNode(page);
            return pageAnalyzer.getList(node, xpath);
        }
    }
    
    public Map getVars() {
        return vars;
    }

    public void setHttpTools(HttpTools httpTools) {
        this.httpTools = httpTools;
    }

    public void setPageAnalyzer(PageAnalyzer pageAnalyzer) {
        this.pageAnalyzer = pageAnalyzer;
    }
    
    public List getList(TagNode node, String xpath, Map<String, String> properMap) throws Exception {
        List returnList = new ArrayList();
        List list = pageAnalyzer.getList(node, xpath);
        for (Object item : list) {
            if (properMap == null || properMap.isEmpty()) {
                returnList.add(item);
            } else {
                Map map = new HashMap();
                for (String key : properMap.keySet()) {
                    try {
                        String path = null;
                        String script = null;
                        Object opath = properMap.get(key);
                        if (opath instanceof Map) {
                            path = (String)((Map)opath).get("xpath");
                            script = (String)((Map)opath).get("script");
                            if (StringUtils.isBlank(script)) {
                                script = (String)((Map)opath).get("templete");
                            }
                        } else {
                            path = opath.toString();
                        }
                        TagNode tn = null;
                        if (item instanceof TagNode) {
                            tn = (TagNode)item;
                        } else {
                            tn = pageAnalyzer.toTagNode(item.toString());
                        }
                        String value = pageAnalyzer.getText(tn, path);
                        if (StringUtils.isNoneBlank(script)) {
                            value = template.expressCalcu(script, value, null);
                        }
                        //System.out.println("key:" + key + "    path=" + path + "    value=" + value);
                        map.put(key, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                returnList.add(map);
            }
        }
        return returnList;
    }
    
    
    public List getAList(TagNode node, String xpath) throws Exception {
        Map<String, String> properMap = new HashMap();
        properMap.put("name", "//text()");
        properMap.put("url", "@href");
        List list = getList(node, xpath, properMap);
        return list;
    }

    
    public static void main(String[] args) throws Exception {
        /*
        String str = "<%var a=1,b=2; %> a+b=${a+b}   aaa  ${sys.currentTimeMillis()}";
        StringTemplateResourceLoader resourceLoader = new StringTemplateResourceLoader();		
		Configuration cfg = Configuration.defaultConfiguration();
		GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        gt.registerFunctionPackage("sys", System.class);
		Template t = gt.getTemplate(str);
        
		//t.binding("time", time);		
		String result = t.render();
        System.out.println(result);
                */
        AsynBeeGather gather = new AsynBeeGather();
        Map map = new HashMap();
        map.put("abc", "123*456");
        System.out.println(gather.template.expressCalcu("${date(),dateFormat=\"yyyy-MM-dd\"} *** ${str.substring(abc, 0, str.indexOf(abc, \"*\"))}", map));
        
    }

    private Map nameValueToMap(List<NameValuePair> params) {
        Map map = new HashMap();
        for (NameValuePair nvp : params) {
            map.put(nvp.getName(), nvp.getValue());
        }
        return map;
    }
    
    private DecimalFormat formatN2 = new DecimalFormat("00");
    private DecimalFormat formatN4 = new DecimalFormat("0000");
    private Pattern DPATTERN1 =  Pattern.compile("[0-9]+\\.\\.[0-9]+");
    private Pattern DPATTERN2 =  Pattern.compile("[a-zA-Z]\\.\\.[a-zA-Z]");

    private Map initVars(Map vars, Map resources) throws Exception{
//        Map varMap = new HashMap();
//        for (Object varName : vars.keySet()) {
//            Object value = vars.get(varName);
//            //如果是指定的资源，就要装载资源
//            if (value instanceof Map) {
//                Map map = (Map)value;
//                String resName = (String)map.get("resource");
//                if (StringUtils.isNotBlank(resName)) {
//                    Map resource = (Map)resources.get(resName);
//                    Object resourceValue = resourceMng.loadResource(resource);
//                    varMap.put(varName, resourceValue);
//                }
//            }
//        }
//        vars.putAll(varMap);
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import com.alibaba.fastjson.JSON;
import com.jayway.jsonpath.JsonPath;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlcleaner.TagNode;
import org.shenjitang.beepasture.debug.GatherDebug;
import org.shenjitang.beepasture.function.JavaScriptExecuter;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.http.PageAnalyzer;
import org.shenjitang.beepasture.resource.BeeResource;
import org.shenjitang.beepasture.resource.ResourceMng;
import org.shenjitang.beepasture.resource.util.ResourceUtils;
import org.shenjitang.commons.csv.CSVUtils;

/**
 *
 * @author xiaolie
 */
public class GatherStep {

    public static Log MAIN_LOGGER = LogFactory.getLog("org.shenjitang.beepasture.core.Main");
    protected final Map step;
    protected final BeeGather beeGather;
    protected final ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    protected final PageAnalyzer pageAnalyzer;
    protected static final Log LOGGER = LogFactory.getLog(GatherStep.class);
    protected List withVar;
    protected Object withVarCurrent;
    protected Long count = 0L;
    protected final String rurl; //yamel中url的值（脚本中的源句）
    protected final Long limit;
    protected final Map save;
    protected final Object xpath;
    protected Map heads;
    protected final Map templateParamMap = new HashMap();
    protected final String script; //yamel中script或template的值（脚本中的源句）

    public GatherStep(Map step) {
        pageAnalyzer = new PageAnalyzer();
        this.step = step;
        this.beeGather = BeeGather.getInstance();
        String withVarName = (String) getValue(step, "with", (String) null);
        if (StringUtils.isNotBlank(withVarName)) {
            if (beeGather.getResourceMng().getResource(withVarName) == null) {
                withVar = (List) beeGather.getVars().get(withVarName);
            }
        }
        rurl = (String) step.get("url");
        limit = GatherStep.getLongValue(step, "limit");
        save = (Map) step.get("save");
        xpath = step.get("xpath");
        heads = (Map) step.get("head");
        if (heads == null) {
            heads = (Map) step.get("heads");
        }
        script = beeGather.getScript(step);
    }

    public void execute() throws Exception {
        List urls;
        if (withVar != null) {
            urls = withVar;
        } else {
            urls = getUrlsFromStepUrl(rurl, step); 
        }
        for (Object ourl : urls) {
            if (step.containsKey("iterator")) {
               onceFlow(ourl); 
            } else {
                onceGather(ourl);
            }
            if (limit != null && ++count >= limit) {
                break;
            }
        }
    }
    
    protected List getUrlsFromStepUrl(String url, Map step) throws Exception {
        List urls = null;
        if (beeGather.getVars().containsKey(url)) {
            urls = (List)beeGather.getVar(url);//   vars.get(url);
        } else {
            urls = new ArrayList();
            urls.add(url);
        }
        return urls;
    }
   
    protected Map getLoadParam() {
        //资源装载的params里的参数，如果是对vars的引用，就要替换成vars立的值。
        Map loadParam = new HashMap();
        Map map = step.get("param") == null ? step : (Map) step.get("param");
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (beeGather.getVars().containsKey(value)) {
                loadParam.put(key, beeGather.getVar((String) value));
            } else {
                loadParam.put(key, value);
            }
        }
        return loadParam;
    }
    
    protected Object loadResource(String url) throws Exception {
        GatherDebug.debug(this, "加载资源：" + url);
        BeeResource beeResource = beeGather.getResourceMng().getResource(url);
        return beeResource.loadResource(getLoadParam());
    }
    
    public boolean needExpressCalcu(Object str) {
        return str instanceof String && str.toString().contains("${");
    }
    
    /**
     * 對url中取得的內容做xpath處理。
     * @param page
     * @return 
     */
    public List doXpath(List page) {
        if (xpath == null){
            return page;
        }
        if (xpath instanceof List) {
            for (Object x : (List)xpath) {
                page = doXpath(page, (String)x);
            }
        } else {
            if (StringUtils.isBlank((String)xpath)) {
                return page;
            }
            page = doXpath(page, (String)xpath);
        }
        return page;
    }
    
    public List doXpath(List page, String xpath) {    
        List pageList = new ArrayList();
        for (Object p : (List)page) {
            if (p instanceof TagNode) {
                try {
                    pageList.addAll(doXpath((TagNode)p, xpath));
                } catch (Exception e) {
                    LOGGER.warn("xpath:" + xpath + "  page:" + p.toString(), e);
                }
            } else {
                pageList.addAll(doXpath(p.toString(), xpath));
            }
        }
        return pageList;
    }
    
    public List doXpath(String page, String xpath) {    
        List rlist = new ArrayList();
        if (xpath.startsWith("json(")) {
            String jsonPath = xpath.substring(5, xpath.length() - 1);
            Object s = JsonPath.read(page, jsonPath);
            if (s instanceof List) {
                rlist.addAll((List)s);
            } else {
                rlist.add(s);
            }
            return rlist;
        } else {
            try {
                TagNode node = pageAnalyzer.toTagNode(page);
                List pages = pageAnalyzer.getList(node, xpath);
                if (pages == null || pages.isEmpty()) {
                    LOGGER.warn("xpath不能抓到合适的内容！ xpath:" + xpath);
                }
                return pages;
            } catch (Exception e) {
                LOGGER.warn("xpath:" + xpath + "  page:" + page, e);
                return new ArrayList();
            }
        }
    }   
    
    private List doXpath(TagNode page, String xpath) {    
        List rlist = new ArrayList();
        if (xpath.startsWith("json(")) {
            String jsonPath = xpath.substring(5, xpath.length() - 1);
            Object s = JsonPath.read(page.toString(), jsonPath);
            if (s instanceof List) {
                rlist.addAll((List)s);
            } else {
                rlist.add(s);
            }
            return rlist;
        } else {
            try {
                List pages = pageAnalyzer.getList(page, xpath);
                if (pages == null || pages.isEmpty()) {
                    LOGGER.warn("xpath不能抓到合适的内容！ xpath:" + xpath);
                }
                return pages;
            } catch (Exception e) {
                LOGGER.warn("xpath:" + xpath + "  page:" + page, e);
                return new ArrayList();
            }
        }
    }  
    
    public List doJsonpath(List page, String jsonPath) {    
        List pageList = new ArrayList();
        for (Object p : (List)page) {
            pageList.addAll(doJsonpath(p.toString(), jsonPath));
        }
        return pageList;
    }
    
    public List doJsonpath(String page, String jsonPath) {    
        List rlist = new ArrayList();
        Object s = JsonPath.read(page, jsonPath);
        if (s instanceof List) {
            rlist.addAll((List) s);
        } else {
            rlist.add(s);
        }
        return rlist;
    }   
        
    public static List toList(Object page) {
        if (page instanceof List) {
            return (List) page;
        } else {
            List pages = new ArrayList();
            pages.add(page);
            return pages;
        }
    }
    
    public List doJavaScript(List pages, String ascript) {
        if (StringUtils.isBlank(ascript)) {
            return pages;
        }
        List list = new ArrayList();
        templateParamMap.putAll(beeGather.getVars());
        for (Object page : pages) {
            try {
                //templateParamMap.put("_page", page);
                if (page instanceof TagNode) {
                    page = ((TagNode)page).getText();
                }
                templateParamMap.put("it", page);
                Object res = JavaScriptExecuter.exec(ascript, templateParamMap);
                list.add(res);
            } catch (Exception e) {
                LOGGER.warn("template:" + ascript + "  page:" + page, e);
            }
        }
        return list;
    }
    
    /**
     * 對從url中取得的內容做script處理
     * @param pages
     * @return 
     */
    public List doScript(List pages, String ascript) {
        if (StringUtils.isBlank(ascript)) {
            return pages;
        }
        List list = new ArrayList();
        templateParamMap.putAll(beeGather.getVars());
        for (Object page : pages) {
            try {
                //templateParamMap.put("_page", page);
                if (page instanceof TagNode) {
                    page = ((TagNode)page).getText();
                }
                templateParamMap.put("it", page);
                String res = template.expressCalcu(ascript, templateParamMap);
                list.add(res);
            } catch (Exception e) {
                LOGGER.warn("template:" + ascript + "  page:" + page, e);
            }
        }
        return list;
    }
    
    /**
     * 對從url中取得的內容做正則處理
     * @param pages
     * @param regex
     * @return 
     */
    public List doRegex(List pages, String regex) {
        if (StringUtils.isNotBlank(regex)) {
            List list = new ArrayList();
            templateParamMap.putAll(beeGather.getVars());
            for (Object page : pages) {
                String v = doRegex(page.toString(), regex);
                if (v != null) {
                    list.add(v);
                }
            }
            return list;
        } else {
            return pages;
        }
    }
    
    public String doRegex(String page, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    public List doFilter(List obj, String filterStr) {
        if (StringUtils.isBlank(filterStr)) {
            return obj;
        }
        List resList = new ArrayList();
        if (obj instanceof List) {
            for (Object page : (List)obj) {
                if (doFilterOnce("script", filterStr, page)) {
                    resList.add(page);
                }
            }
        } else {
            if (doFilterOnce("script", filterStr, obj)) {
                resList.add(obj);
            }
        }
        return resList;
    }
    
    protected List extract(Map extract, List pages) {
        for (Object key: extract.keySet()) {
            if ("filter".equalsIgnoreCase(key.toString())) {
                return (List)doFilter(pages, (String)extract.get(key));
//                List resList = new ArrayList();
//                for (Object page : pages) {
//                    if (doFilterOnce((String)extract.get(key), page)) {
//                        resList.add(page);
//                    }
//                }
//                return resList;
            } else if ("xpath".equalsIgnoreCase(key.toString())) {
                return doXpath(pages, (String)extract.get(key));
            } else if ("jsonpath".equalsIgnoreCase(key.toString())) {
                return doJsonpath(pages, (String)extract.get(key));
            } else if ("javascript".equalsIgnoreCase(key.toString())) {
                return doJavaScript(pages,(String)extract.get(key));
            } else if ("script".equalsIgnoreCase(key.toString())) {
                return doScript(pages,(String)extract.get(key));
            } else if ("regex".equalsIgnoreCase(key.toString())) {
                return doRegex(pages, (String)extract.get(key));
            } else if ("marshal".equalsIgnoreCase(key.toString())) {
                return doMarshal(pages, (Map)extract.get("marshal"));
            } else if ("unmarshal".equalsIgnoreCase(key.toString())) {
                return doUnmarshal(pages, (Map)extract.get("unmarshal"));
            }
        }
        return pages;
    }
    
    protected Object propertyExtract(Map extract, Object it) {
        try {
            for (Object key: extract.keySet()) {
                String express = (String)extract.get(key);
                if ("xpath".equalsIgnoreCase(key.toString())) {
                    TagNode node = null;
                    if (it instanceof TagNode) {
                        node = (TagNode)it;
                    } else {
                        node = pageAnalyzer.toTagNode(it.toString());
                    }
                    return pageAnalyzer.getText(node, express);
                } else if ("jsonpath".equalsIgnoreCase(key.toString())) {
                    return JsonPath.read(it, express);
                } else if ("javascript".equalsIgnoreCase(key.toString())) {
                    templateParamMap.put("it", it);
                    return JavaScriptExecuter.exec(express, templateParamMap);
                } else if ("script".equalsIgnoreCase(key.toString())) {
                    templateParamMap.put("it", it);
                    //templateParamMap.put("_page", it);
                    //templateParamMap.put("_this", ourl);
                    return template.expressCalcu(express, templateParamMap);
                } else if ("regex".equalsIgnoreCase(key.toString())) {
                    Pattern pattern = Pattern.compile(express);
                    Matcher matcher = pattern.matcher(it.toString());
                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn(it, e);
        }
        return "";
    }
    
    /**
     * 做一次gather.
     * @param ourl 将rurl转换成需要处理的列表后的一条url。
     */
    public void onceGather(Object ourl) {
        templateParamMap.putAll(beeGather.getVars());
        withVarCurrent = ourl;
        if (withVar != null) {
            ourl = ((Map) ourl).get(rurl);
            step.put("withVarCurrent", withVarCurrent);
        }
        try {
            Object page = ourl;
            Boolean direct = ResourceUtils.get(step, "direct", false);
            if (!direct) {
                if (needExpressCalcu(ourl)) {
                    ourl = template.expressCalcu((String) ourl, beeGather.getVars());
                } else if (ourl instanceof File) {
                    ourl = "file://" + ((File)ourl).getAbsolutePath();
                }
                templateParamMap.put("_this", ourl);
                if (ourl instanceof String) {
                    if (beeGather.containsResource((String)ourl) || ResourceMng.maybeResource(ourl.toString())) {
                        try {
                            page = loadResource((String)ourl);
                        } catch (Exception e) {
                            LOGGER.warn("unknown resource:" + ourl, e);
                        }
                    }
                }
            }
            templateParamMap.put("_page", page);
            List pages = toList(page);
            try {
                List extractList = (List)step.get("extract");
                if (extractList != null) {
                    for (Object extract : extractList) {
                        pages = extract((Map)extract, pages);
                        GatherDebug.debug(this, "执行完语句：" + JSON.toJSONString(extract));
                    }
                }
            } catch (ClassCastException e) {
                throw new RuntimeException("关键字extract的值必须是数组，不可以是map或别的类型！", e);
            }
            pages = doFilter(pages, (String) step.get("filter"));
            pages = doXpath(pages);
            pages = doRegex(pages, (String) step.get("regex"));
            pages = doScript(pages, script);
            pages = doJavaScript(pages, (String) step.get("javascript"));
            pages = doMarshal(pages, (Map)step.get("marshal"));
            pages = doUnmarshal(pages, (Map)step.get("unmarshal"));
            pages = setProperties(pages, ourl);
            save(pages, ourl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void onceFlow(Object ourl) {
        try {
            if (needExpressCalcu(ourl)) {
                ourl = template.expressCalcu((String) ourl, beeGather.getVars());
            } else if (ourl instanceof File) {
                ourl = "file://" + ((File) ourl).getAbsolutePath();
            }
            templateParamMap.put("_this", ourl);
            if (ourl instanceof String) {
                String url = (String)ourl;
                if (beeGather.containsResource(url) || ResourceMng.maybeResource(url)) {
                    try {
                        Map loadParam = getLoadParam();
                        BeeResource beeResource = beeGather.getResourceMng().getResource(url);
                        Iterator ite = beeResource.iterate(loadParam);
                        while(ite.hasNext()) {
                            Object page = ite.next();
                            templateParamMap.put("_page", page);
                            List pages = toList(page);
                            List extractList = (List)step.get("extract");
                            if (extractList != null) {
                                for (Object extract : extractList) {
                                    pages = extract((Map)extract, pages);
                                    GatherDebug.debug(this, "执行完语句：" + JSON.toJSONString(extract));
                                }
                            }
                            pages = doFilter(pages, (String) step.get("filter"));
                            pages = doXpath(pages);
                            pages = doRegex(pages, (String) step.get("regex"));
                            pages = doScript(pages, script);
                            pages = doJavaScript(pages, (String) step.get("javascript"));
                            pages = doMarshal(pages, (Map)step.get("marshal"));
                            pages = doUnmarshal(pages, (Map)step.get("unmarshal"));
                            pages = setProperties(pages, ourl);
                            save(pages, ourl);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("unknown resource:" + ourl, e);
                    }
                } else {
                    LOGGER.warn("url->flow the url mast be resource! not cuttent this url:" + ourl);
                }
            } else {
                LOGGER.warn("url->flow the url mast be resource! not cuttent this url:" + ourl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    

    public Object doScript(Object it, Object page, Object ourl) throws Exception {
        if (it == null) {
            return it;
        }
        if (StringUtils.isBlank(it.toString())) {
            return it;
        }
        String regex = (String)step.get("regex");
        if (StringUtils.isNotBlank(regex)) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(it.toString());
            if (matcher.find()) {
                it = matcher.group();
            }
        }
        if (StringUtils.isNotBlank(script)) {
            return template.expressCalcu(script, it, page, ourl, beeGather.getVars());
        } else {
            return it;
        }
    }
    
    public Map saveStr2map(Object srcTo) {
        Map toMap = new HashMap();
        toMap.putAll(save);
        if (srcTo == null && !toMap.containsKey("resource")) {
            toMap.put("resource", "camel");
            //toMap.putAll(save);
        } else if (srcTo instanceof String) {
            toMap.put("to", srcTo);
            //toMap.putAll(save);
        } else if (srcTo instanceof Map) {
            if (((Map)srcTo).containsKey("endpoint")) {
                ((Map)srcTo).put("resource", "camel");
            }
            toMap.putAll((Map)srcTo);
        }
        return toMap;
        
//        Object to = save.get("to");
//        if (to == null) {
//            toMap.put("to", "camel");
//        } else if (to instanceof String) {
//            String toStr = (String)save.get("to");
//            if (StringUtils.isNotBlank(toStr)) {
//                toMap.put("to", toStr);
//            } else {
//                toMap.put("to", "camel");
//            }
//        } else {
//            return (Map)srcTo;
//        }
//        if (srcTo instanceof Map) {
//            toMap.putAll(srcTo);
//        }
//        return toMap;
    }
    
    public void save(List pages, Object ourl) {
        Object to = save.get("to");
        if (to instanceof List) {
            for (Object saveDef : (List)to) {
                Map toMap = saveStr2map(saveDef);
                save(toMap, pages, ourl);
            }
        } else {
            Map toMap = saveStr2map(to);
            save(toMap, pages, ourl);
        }
    }
    
    protected Boolean doFilterOnce(String key, String filterExpress, Object page) {
        if ("script".equalsIgnoreCase(key)) {
            String res = doScript(filterExpress, page);
            return Boolean.valueOf(res);
        } else {
            throw new RuntimeException("不支持的filter检查方式：" + key);
        }
    }
    
    protected void save(Map saveDefMap, List pages, Object ourl) {
        for (Object page : pages) {
            removeProperties(page);
            String filterExpress = (String)saveDefMap.get("filter");
            if (StringUtils.isNotBlank(filterExpress) && !doFilterOnce("script", filterExpress, page)) {
                continue;
            }
            String varName = (String)saveDefMap.get("var");
            if (StringUtils.isNotBlank(varName)) {
                saveToVar(varName, page, ourl);
            } else {
                String resourceName = (String)saveDefMap.get("resource");
                if (StringUtils.isNotBlank(resourceName)) {
                    resourceName = doScript(resourceName, page);
                    saveToResource(resourceName, page, ourl, saveDefMap);
                } else {
                    String name = (String)saveDefMap.get("to");
                    if (StringUtils.isNotBlank(name)) {
                        smartSaveTo(name, page, ourl, saveDefMap);
                    } else if (saveDefMap.containsKey("endpoint")) {
                        saveToResource("camel", page, ourl, saveDefMap);                        
                    }
                }
            }
        }
    }
    
    protected void saveToVar(String varName, Object page, Object ourl) {
        varName = doScript(varName, page);
        if ("_this".equalsIgnoreCase(varName)) {
            ((Map) withVarCurrent).putAll((Map) page);
        } else {
            List toList = beeGather.getVar(varName);
            toList.add(page);
        }
    }
    
    protected void saveToResource(String name, Object page, Object ourl, Map saveDefMap) {
        BeeResource resource = beeGather.getResourceMng().getResource(name);
        if (resource != null) {
            beeGather.getVars().remove(name);
            resource.persist("it", page, saveDefMap);
        } else {
           throw new RuntimeException("can not find resource: " + name);
        }
    }
    
    protected void smartSaveTo(String name, Object page, Object ourl, Map saveDefMap) {
        BeeResource resource = beeGather.getResourceMng().getResource(name);
        if (resource != null) {
            beeGather.getVars().remove(name);
            resource.persist(null, page, saveDefMap);
        } else {
           saveToVar(name, page, ourl);
        }
    }
    
    protected void removeProperties(Object value) {
        List removePropertyList = (List) save.get("removeProperty");
        if (removePropertyList != null) {
            if (removePropertyList.isEmpty()) { //如果 removePropety: [] 表示删除property中没有提到的
                Map propMap = (Map)save.get("property");
                if (value instanceof Map) {
                    for (Object key : ((Map) value).keySet()) {
                        if (!propMap.containsKey(key)) {
                            removePropertyList.add(key);
                        }
                    }
                }
            }
            if (value instanceof Map) {
                for (Object key : removePropertyList) {
                    ((Map) value).remove(key);
                }
            }
        }
    }
    
    public String doScript(String script, Object it) {
        templateParamMap.put("it", it);
        if (needExpressCalcu(script)) {
            return template.expressCalcu(script, templateParamMap);
        }
        return script;
    }

    final protected <T> T getValue(Map map, String key, T defaultValue) {
        T value = (T) map.get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    protected static Long getLongValue(Map map, String key) {
        Object oValue = map.get(key);
        if (oValue != null) {
            return Long.valueOf(oValue.toString());
        } else {
            return null;
        }
    }

    protected void sleep() {
        Object oSleep = step.get("sleep");
        if (oSleep != null) {
            Long sleep = Long.valueOf(oSleep.toString());
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                LOGGER.warn("sleep", e);
            }
        }
    }
    
    protected List doUnmarshal(List its, Map unmarshalMap) {
//        Map unmarshalMap = (Map)save.get("unmarshal");
        if (unmarshalMap != null) {
            List returnList = new ArrayList();
            for (Object it : its ) {
                returnList.add(unmarshal(it, unmarshalMap));
            }
            return returnList;
        }        
        return its;
    }

    protected List doMarshal(List its, Map marshalMap) {
//        Map marshalMap = (Map)save.get("unmarshal");
        if (marshalMap != null) {
            List returnList = new ArrayList();
            for (Object it : its ) {
                try {
                    returnList.add(marshal(it, marshalMap));
                } catch (Exception e) {
                    LOGGER.warn("marshal fail", e);
                }
            }
            return returnList;
        }        
        return its;
    }

    protected List setProperties(List its, Object ourl) {
        Map propertyMap = (Map) save.get("property");
        if (propertyMap == null || propertyMap.isEmpty()) {
            return its;
        } 
        List returnList = new ArrayList();
        for (Object it : its ) {
            returnList.add(setPageProperties(it, ourl, propertyMap));
        }
        return returnList;
    }
    
    protected Map setPageProperties(Object it, Object ourl, Map propertyMap) {
        Map result;
       if (Map.class.isAssignableFrom(it.getClass())) {
            result = (Map) it;
        } else {
            result = new HashMap();
        }
        for (Object key : propertyMap.keySet()) {
            Object ov = null;
            try {
                Object propertyPropDef = propertyMap.get(key);

                String type = null;
                String propScript = null;
    //            String scope = null;
                if (propertyPropDef instanceof String) {
                    Map m1 = new HashMap();
                    if (((String)propertyPropDef).contains("/")) {
                        m1.put("xpath", propertyPropDef);
                    } else if (((String)propertyPropDef).startsWith("$.") || ((String)propertyPropDef).startsWith("$[")){
                        m1.put("jsonpath", propertyPropDef);
                    } else {
                        m1.put("script", propertyPropDef);
                    }
                    propertyPropDef = m1;
                }
                Map propValue = (Map)propertyPropDef;
                type = (String) propValue.get("type");

                List extractList = (List)propValue.get("extract");
                if (extractList != null) {
                    if ("_page".equalsIgnoreCase((String)propValue.get("with"))) {
                        ov = templateParamMap.get("_page");
                    } else if ("_this".equalsIgnoreCase((String)propValue.get("with"))) {
                        ov = templateParamMap.get("_this");
                    } else {
                        ov = it;
                    }
                    for (Object extract : extractList) {
                        ov = propertyExtract((Map)extract, ov);
                        templateParamMap.put("it", ov);
                        GatherDebug.debug(this, "执行完语句：" + JSON.toJSONString(extract));
                    }
                } else {
                    propScript = beeGather.getScript(propValue);
                    Object aim = it;
                    if ("_page".equalsIgnoreCase((String)propValue.get("with"))) {
                        aim = templateParamMap.get("_page");
                    } else if ("_this".equalsIgnoreCase((String)propValue.get("with"))) {
                        aim = templateParamMap.get("_this");
                    }
                    if (aim instanceof String) {
                        ov = xpathPropertyObj((String) aim, propValue);
                    } else if (aim instanceof TagNode) {
                        ov = xpathPropertyObj((TagNode) aim, propValue);
                    } else if (aim instanceof Map) {
                        ov = ((Map) aim).get(key);
                    }
        //                ov = template.expressCalcu(script, thisValue, result);
                    if (ov == null && aim instanceof String) {
                        ov = aim;
                    }
                    if (StringUtils.isNotBlank(propScript)) {
                        templateParamMap.put("it", ov);
                        //templateParamMap.put("_page", it);
                        //templateParamMap.put("_this", ourl);
                        ov = template.expressCalcu(propScript, templateParamMap);
                    }
                    String javaScriptExpress = (String)propValue.get("javascript");
                    if (StringUtils.isNotBlank(javaScriptExpress)) {
                        templateParamMap.put("it", ov);
                        ov = JavaScriptExecuter.exec(javaScriptExpress, templateParamMap);
                    }
                }
                if (StringUtils.isNotBlank(type) && ov != null) {
                    try {
                        if ("date".equalsIgnoreCase(type)) {
                            String format = getValue(propValue, "format", "yyyy-MM-dd HH:mm:ss");
                            String locate = (String)(propValue).get("locate");
                            SimpleDateFormat sdf = null;
                            if (StringUtils.isBlank(locate) && ov != null) {
                                if (ov.toString().contains("MMM")) {
                                    locate = "ENGLISH";
                                }
                            }
                            if (StringUtils.isBlank(locate)) {
                                sdf = new SimpleDateFormat(format);
                            } else {
                                sdf = new SimpleDateFormat(format, Locale.forLanguageTag(locate));
//                                if ("ENGLISH".equalsIgnoreCase(locate)) {
//                                    sdf = new SimpleDateFormat(format, Locale.ENGLISH);
//                                } else if ("CHINESE".equalsIgnoreCase(locate)) {
//                                    sdf = new SimpleDateFormat(format, Locale.CHINESE);
//                                } else if () {
//                                    sdf = new SimpleDateFormat(format, Locale.);
//                                }
                            }
                            if (StringUtils.isNotBlank(ov.toString())) {
                                ov = sdf.parse(correctDateStr(ov.toString()));
                            } else {
                                ov = null;
                            }
                        } else if ("String[]".equalsIgnoreCase(type)) {
                            String split = getValue(propValue, "split", ",");
                            ov = ((String) ov).split(split);
                        } else if ("bool".equalsIgnoreCase(type) || "Boolean".equalsIgnoreCase(type)) {
                            ov = Boolean.valueOf(ov.toString());
                        } else if ("int".equalsIgnoreCase(type) || "Integer".equalsIgnoreCase(type)) {
                                ov = Integer.valueOf(ov.toString());
                        } else if ("long".equalsIgnoreCase(type)) {
                                ov = Long.valueOf(ov.toString());
                        } else if ("double".equalsIgnoreCase(type)) {
                                ov = Double.valueOf(ov.toString());
                        } else if ("float".equalsIgnoreCase(type)) {
                                ov = Float.valueOf(ov.toString());
                        } else if ("number".equalsIgnoreCase(type)) {
                                String format = getValue(propValue, "format", null);
                                java.text.DecimalFormat df = new java.text.DecimalFormat(format);
                                try {
                                    ov = df.parse(ov.toString());
                                } catch (java.text.ParseException e) {
                                    LOGGER.warn("非法字符不能转Number类型 :" + ov.toString());
                                    ov = null;
                                }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("page:" + it, e);
                        ov = null;
                    }
                }
                result.put(key, ov);
            } catch (Exception e) {
                LOGGER.warn("提取字段：" + key + " 出现异常", e);
            }
            GatherDebug.debug(this, "提取字段：" + key + " = " + ov);
        }
        return result;
    }
    
    public Object xpathPropertyObj(String page, Object propertyParam) {
            String value = null;
            try {
                String path = null;
                String regex = null;
                if (propertyParam instanceof Map) {
                    path = (String) ((Map) propertyParam).get("xpath");
                    regex = (String) ((Map) propertyParam).get("regex");
                } else {
                    path = propertyParam.toString();
                }
                if (StringUtils.isBlank(path)) {
                    value = page;
                } else if (path.startsWith("json(")) {
                    String jsonPath = path.substring(5, path.length() - 1);
                    value = JsonPath.read(page, jsonPath);
                } else {
                    TagNode tn = pageAnalyzer.toTagNode((String) page);
                    value = pageAnalyzer.getText(tn, path);
                }
                
                if (StringUtils.isNotBlank(regex)) {
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(value);
                    if (matcher.find()) {
                        value = matcher.group();
                    }
                }
                return value;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
    }    
    
    public Object xpathPropertyObj(TagNode tn, Object propertyParam) {
            String value = null;
            try {
                String path = null;
                String regex = null;
                if (propertyParam instanceof Map) {
                    path = (String) ((Map) propertyParam).get("xpath");
                    regex = (String) ((Map) propertyParam).get("regex");
                } else {
                    path = propertyParam.toString();
                }
                if (StringUtils.isBlank(path)) {
                    path = ".";
                }
                value = pageAnalyzer.getText(tn, path);
                if (StringUtils.isNotBlank(regex)) {
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(value);
                    if (matcher.find()) {
                        value = matcher.group();
                    }
                }                
                return value;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
    }      

    public Map getTemplateParamMap() {
        return templateParamMap;
    }

    public static String correctDateStr(String str) {
        StringBuilder sb = new StringBuilder();
        int numCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') { //是数字
                numCount++;
            } else { //是字符
                if (numCount == 1) {
                    sb.insert(sb.length() - 1, '0');
                }
                numCount = 0;
            }
            sb.append(c);
        }
        if (numCount == 1) {
            sb.insert(sb.length() - 1, '0');
        }
        return sb.toString();
    }
    
    public static void main(String[] args) throws Exception {
        /*
        System.out.println(correctDateStr("2013-7-23 8:12:5"));
        System.out.println(correctDateStr("a2013-7-23 8:12:5"));
        System.out.println(correctDateStr("a2013-7-23 8:12:5b"));
         System.out.println(correctDateStr("a2013-7-23 8:12:53b"));
        SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        Date d = format.parse(correctDateStr("05/Aug/2016:03:00:28 +0800"));
        System.out.println(d);
*/
    }

    private Map unmarshal(Object page, Map get) {
        String type = (String) get.get("type");
        if ("csv".equalsIgnoreCase(type)) {
            String split = ResourceUtils.get(get, "split", ",");
            Map map = new HashMap();
            String[] ps = page.toString().split(split);
            for (int i = 0; i < ps.length; i++) {
                map.put("f" + i, ps[i]);
            }
            return map;
        } else {
            throw new RuntimeException("not support unmarshal type: " + type);
        }
    }

    private String marshal(Object page, Map get) throws Exception {
        String type = (String) get.get("type");
        if ("json".equalsIgnoreCase(type)) {
            return JSON.toJSONString(page);
        } else if ("csv".equalsIgnoreCase(type)) {
            CSVFormat format = CSVFormat.DEFAULT;
            if (get.containsKey("delimiter")) {
                format = CSVFormat.newFormat(((String)get.get("delimiter")).trim().charAt(0));
            }
            List headList = (List)get.get("head");
            Object[] values = CSVUtils.getValues(headList, page);
            return format.format(values);
        } else {
            throw new RuntimeException("not support m"
                    + "arshal type: " + type);
        }
    }
}

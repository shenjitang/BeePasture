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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlcleaner.TagNode;
import org.shenjitang.beepasture.debug.GatherDebug;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.http.PageAnalyzer;
import org.shenjitang.beepasture.resource.BeeResource;
import org.shenjitang.beepasture.resource.util.ResourceUtils;

/**
 *
 * @author xiaolie
 */
public class GatherStep {

    public static Log MAIN_LOGGER = LogFactory.getLog("org.shenjitang.beepasture.core.Main");
    private final Map step;
    private final BeeGather beeGather;
    private final ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    private final PageAnalyzer pageAnalyzer;
    private static final Log LOGGER = LogFactory.getLog(GatherStep.class);
    private List withVar;
    private Object withVarCurrent;
    private Long count = 0L;
    private final String rurl; //yamel中url的值（脚本中的源句）
    private final Long limit;
    private final Map saveTo;
    private final Object xpath;
    private Map heads;
    private final Map templateParamMap = new HashMap();
    private String script; //yamel中script或template的值（脚本中的源句）

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
        saveTo = (Map) step.get("save");
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
            urls = getUrlsFromStepUrl(rurl, step); //如果是资源，也是从这里加载了
        }
        for (Object ourl : urls) {
            onceGather(ourl);
            if (limit != null && ++count >= limit) {
                break;
            }
        }
    }
    
    protected List getUrlsFromStepUrl(String url, Map step) throws Exception {
        List urls = null;
        if (beeGather.getVars().containsKey(url)) {
            urls = (List)beeGather.getVar(url);//   vars.get(url);
        } else if (beeGather.containsResource(url)) {
            urls = loadResource(url, step);
        } else {
            urls = new ArrayList();
            urls.add(url);
        }
        return urls;
    }
    
    protected List loadResource(String url, Map step) throws Exception {
        GatherDebug.debug(this, "加载资源：" + url);
        BeeResource beeResource = beeGather.getResourceMng().getResource(url);
        //资源装载的params里的参数，如果是对vars的引用，就要替换成vars立的值。
        Map loadParam = new HashMap();
        Map map = null;
        if (step.get("param") != null) {
            map = (Map) step.get("param");
        } else {
            map = step;
        }
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (beeGather.getVars().containsKey(value)) {
                loadParam.put(key, beeGather.getVar((String) value));
            } else {
                loadParam.put(key, value);
            }
        }
        Object value = beeResource.loadResource(loadParam);
        List list = new ArrayList();
        if (value instanceof Collection) {
            list.addAll((Collection) value);
        } else {
            list.add(value);
        }
        return list;
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
    
    protected List extract(Map extract, List pages) {
        for (Object key: extract.keySet()) {
            if ("xpath".equalsIgnoreCase(key.toString())) {
                return doXpath(pages, (String)extract.get(key));
            } else if ("jsonpath".equalsIgnoreCase(key.toString())) {
                return doJsonpath(pages, (String)extract.get(key));
            } else if ("script".equalsIgnoreCase(key.toString())) {
                return doScript(pages,(String)extract.get(key));
            } else if ("regex".equalsIgnoreCase(key.toString())) {
                return doRegex(pages, (String)extract.get(key));
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
        withVarCurrent = ourl;
        if (withVar != null) {
            ourl = ((Map) ourl).get(rurl);
            step.put("withVarCurrent", withVarCurrent);
        }
        try {
            Object page = ourl;
            Boolean direct = Boolean.valueOf(ResourceUtils.get(step, "direct", "false"));
            if (!direct) {
                if (needExpressCalcu(ourl)) {
                    ourl = template.expressCalcu((String) ourl, beeGather.getVars());
                } else if (ourl instanceof File) {
                    ourl = "file://" + ((File)ourl).getAbsolutePath();
                }
                templateParamMap.put("_this", ourl);
                if (ourl instanceof String && ourl.toString().toLowerCase().contains(":")) {
                    try {
                        GatherDebug.debug(this, "准备加载资源：" + ourl);
                        BeeResource resource = beeGather.getResourceMng().getResource((String)ourl);
                        if (resource != null) {
                            page = resource.loadResource(step);
                            templateParamMap.put("_page", page);
                            GatherDebug.debug(this, "加载资源：" + ourl);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("unknown resource:" + ourl, e);
                    }
                }
            }
            templateParamMap.put("_page", page);
            List pages = toList(page);
            List extractList = (List)step.get("extract");
            if (extractList != null) {
                for (Object extract : extractList) {
                    pages = extract((Map)extract, pages);
                    GatherDebug.debug(this, "执行完语句：" + JSON.toJSONString(extract));
                }
            }
            pages = doXpath(pages);
            pages = doRegex(pages, (String) step.get("regex"));
            pages = doScript(pages, script);
            pages = setProperties(pages, ourl);
            save(pages, ourl);
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

    public void filterAddOneVar(String name, Object value) {
        count++;
        List removePropertyList = (List) saveTo.get("removeProperty");
        if (removePropertyList != null) {
            if (removePropertyList.isEmpty()) { //如果 removePropety: [] 表示删除property中没有提到的
                Map propMap = (Map)saveTo.get("property");
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
        BeeResource resource = beeGather.getResourceMng().getResource(name);
        if (resource != null) {
            beeGather.getVars().remove(name);
            resource.persist(null, value, saveTo);
        } else if ("_this".equalsIgnoreCase(name)) {
            ((Map)withVarCurrent).putAll((Map)value);
        } else {
            List toList = beeGather.getVar(name);
            toList.add(value);
        }
    }
    
    public void save(List pages, Object ourl) {
        if (saveTo != null) {
            String name = (String) saveTo.get("to");
            if (StringUtils.isBlank(name)) {
                return;
            }
            for (Object page : pages) {
                templateParamMap.put("it", page);
                if (needExpressCalcu(name)) {
                    name = template.expressCalcu(name, templateParamMap);
                }
//                List toList = beeGather.getVar(name);
//                if (withVar != null) {
//                    if (ourl instanceof Map && !((Map) ourl).containsKey(name)) {
//                        ((Map) ourl).put(name, toList);
//                    }
//                }
                filterAddOneVar(name, page);
            }
        }
    }

    protected <T> T getValue(Map map, String key, T defaultValue) {
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


    protected List setProperties(List its, Object ourl) {
        Map propertyMap = (Map) saveTo.get("property");
        if (propertyMap == null || propertyMap.isEmpty()) {
            return its;
        } 
        List returnList = new ArrayList();
        for (Object it : its ) {
            Map unmarshalMap = (Map)saveTo.get("unmarshal");
            if (unmarshalMap != null) {
                it = unmarshal(it, unmarshalMap);
            }
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
                Object propValue = propertyMap.get(key);

                String type = null;
                String propScript = null;
    //            String scope = null;
                if (propValue instanceof String) {
                    Map m1 = new HashMap();
                    if (((String)propValue).contains("/")) {
                        m1.put("xpath", propValue);
                    } else if (((String)propValue).startsWith("$.") || ((String)propValue).startsWith("$[")){
                        m1.put("jsonpath", propValue);
                    } else {
                        m1.put("script", propValue);
                    }
                    propValue = m1;
                }

                    type = (String) ((Map) propValue).get("type");

                List extractList = (List)((Map) propValue).get("extract");
                if (extractList != null) {
                    ov = it;
                    for (Object extract : extractList) {
                        ov = propertyExtract((Map)extract, ov);
                        templateParamMap.put("it", ov);
                        GatherDebug.debug(this, "执行完语句：" + JSON.toJSONString(extract));
                    }
                } else {
                    propScript = beeGather.getScript((Map) propValue);
                    Object aim = it;
                    if (aim instanceof String) {
                        ov = xpathPropertyObj((String) aim, propValue);
                    } else if (aim instanceof TagNode) {
                        ov = xpathPropertyObj((TagNode) aim, propValue);
                    } else if (aim instanceof Map) {
                        ov = ((Map) aim).get(key);
                    }
        //                ov = template.expressCalcu(script, thisValue, result);
                    if (ov == null) {
                        ov = aim;
                    }
                    if (StringUtils.isNotBlank(propScript)) {
                        templateParamMap.put("it", ov);
                        //templateParamMap.put("_page", it);
                        //templateParamMap.put("_this", ourl);
                        ov = template.expressCalcu(propScript, templateParamMap);
                    }
                }
                if (StringUtils.isNotBlank(type) && ov != null) {
                    try {
                        if ("date".equalsIgnoreCase(type)) {
                            String format = getValue((Map) propValue, "format", "yyyy-MM-dd HH:mm:ss");
                            String locate = (String)((Map) propValue).get("locate");
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
                            String split = getValue((Map) propValue, "split", ",");
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
*/
        SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        Date d = format.parse(correctDateStr("05/Aug/2016:03:00:28 +0800"));
        System.out.println(d);
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
}

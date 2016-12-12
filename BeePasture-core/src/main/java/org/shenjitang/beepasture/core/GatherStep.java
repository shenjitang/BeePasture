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
import org.ho.yaml.Yaml;
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
    protected Map step;
    protected final Map rStep; //yaml里写的原始的step。
    protected final BeeGather beeGather;
    protected final ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    protected final PageAnalyzer pageAnalyzer;
    protected static final Log LOGGER = LogFactory.getLog(GatherStep.class);
    protected List withVar;
    protected Object withVarCurrent;
    protected Long count = 0L;
    protected String rurl; //yaml中url的值（脚本中的源句）
    protected final Long limit;
    protected final Map save;
    protected Object xpath;
    protected final Object oXpath;
    protected Map heads;
    protected final Map templateParamMap = new HashMap();
    protected final String script; //yamel中script或template的值（脚本中的源句）
    public static volatile long activeTime = System.currentTimeMillis();

    public GatherStep(Map step) {
        pageAnalyzer = new PageAnalyzer();
        this.rStep = step;
        this.beeGather = BeeGather.getInstance();
        String withVarName = (String) getValue(step, "with", (String) null);
        if (StringUtils.isNotBlank(withVarName)) {
            if (beeGather.getResourceMng().getResource(withVarName) == null) {
                withVar = (List) beeGather.getVars().get(withVarName);
            }
        }
        rurl = (String) rStep.get("url");
        limit = GatherStep.getLongValue(rStep, "limit");
        save = (Map) rStep.get("save");
        oXpath = rStep.get("xpath");
        xpath = rStep.get("xpath");
        heads = (Map) rStep.get("head");
        if (heads == null) {
            heads = (Map) rStep.get("heads");
        }
        script = beeGather.getScript(rStep);
    }
    
    public String changeValueFromObj(String value, Object obj) {
        return obj instanceof Map && ((Map)obj).containsKey(value) ? (String)((Map)obj).get(value) : value;
    }

    public void execute() throws Exception {
        rurl = maybeScript(rurl) ? doScript(rurl): rurl;
        List urls;
        if (withVar != null) {
            urls = withVar;
        } else {
            urls = getUrlsFromStepUrl(rurl, rStep); 
        }
        for (Object ourl : urls) {
            activeTime = System.currentTimeMillis();
            withVarCurrent = ourl;
            templateParamMap.put("_this", ourl);
            if (withVar != null) {
                templateParamMap.put("_with", ourl);
                ourl = ((Map) ourl).get(rurl);            
            }
            templateParamMap.put("it", ourl);
            step = (Map)cloneMap(rStep);
            beforeGatherExpressCalcu("download", "sql");
            if (withVar != null) {
                step.put("withVarCurrent", withVarCurrent);
            }            
            if (rStep.containsKey("iterator")) {
                onceFlow(ourl); 
            } else {
                onceGather(ourl);
                sleep();
            }
            if (limit != null && ++count >= limit) {
                break;
            }
            activeTime = System.currentTimeMillis();
        }
    }

    /**
     * 做一次gather.
     * @param ourl 将rurl转换成需要处理的列表后的一条url，如果有withVar就是withVar的第一条。
     */
    public void onceGather(Object ourl) throws Exception {

        try {
            Object page = ourl;
            Boolean direct = ResourceUtils.get(step, "direct", false);
            if (!direct) {
                if (maybeScript(ourl)) {
                    ourl = template.expressCalcu((String) ourl, beeGather.getVars());
                } else if (ourl instanceof File) {
                    ourl = "file://" + ((File)ourl).getAbsolutePath();
                }
                //templateParamMap.put("_this", ourl);
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
            pages = doFilter(pages, step.get("filter"));
            pages = doXpath(pages);
            pages = doRegex(pages, step.get("regex"));
            pages = doScript(pages, changeValueFromObj(script, ourl));
            pages = doJavaScript(pages, (String) step.get("javascript"));
            pages = doMarshal(pages, (Map)step.get("marshal"));
            pages = doUnmarshal(pages, (Map)step.get("unmarshal"));
            //pages = setProperties(pages, ourl);
            save(pages, ourl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void onceFlow(Object ourl) {
        try {
            if (maybeScript(ourl)) {
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
                        if (ite == null) {
                            MAIN_LOGGER.warn("skip " + url + "    cause: not find resource");
                            return;
                        }
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
                            pages = doFilter(pages, step.get("filter"));
                            pages = doXpath(pages);
                            pages = doRegex(pages, step.get("regex"));
                            pages = doScript(pages, changeValueFromObj(script, ourl));
                            pages = doJavaScript(pages, (String) step.get("javascript"));
                            pages = doMarshal(pages, (Map)step.get("marshal"));
                            pages = doUnmarshal(pages, (Map)step.get("unmarshal"));
                            //pages = setProperties(pages, ourl);
                            save(pages, ourl);
                        }
                        beeResource.afterIterate();
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
    
    public boolean maybeScript(Object str) {
        return str != null && str instanceof String && str.toString().contains("${");
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
        //templateParamMap.putAll(beeGather.getVars());
        for (Object page : pages) {
            try {
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
        //templateParamMap.putAll(beeGather.getVars());
        for (Object page : pages) {
            try {
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
    public List doRegex(List pages, Object regex) {
        if (regex != null) {
            List list = new ArrayList();
            //templateParamMap.putAll(beeGather.getVars());
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
    
    public String doRegex(String page, Object regex) {
        if (regex instanceof String) {
            return doRegex(page, (String) regex);
        } else {
            String express = (String) ((Map) regex).get("express");
            Integer group = Integer.valueOf(((Map) regex).get("group").toString());
            return doRegex(page, express, group);
        }
    }

    public String doRegex(String page, String regex, Integer groupIndex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(groupIndex);
        }
        return null;
    }
    
    public String doRegex(String page, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    public List doFilter(List obj, Object filter) {
        if (filter == null) {
            return obj;
        }
        List resList = new ArrayList();
        if (filter instanceof String) {
            for (Object page : (List) obj) {
                if (doFilterOnce("script", (String)filter, page)) {
                    resList.add(page);
                    LOGGER.debug("keep item: " + page.toString());
                } else {
                    LOGGER.info("skip item: " + page.toString());
                }
            }
        } else {
            for (Object page : (List) obj) { //对每一个_item做filter
                Map map = (Map)filter;
                boolean keep = true;
                ONE: for (Object key : map.keySet()) {
                    Object value = map.get(key);
                    if (value instanceof String) {
                        keep &= doFilterOnce((String)key, (String)value, page);
                    } else if (value instanceof List) {
                        for (Object item : (List)value) {
                            if (item instanceof String ) {
                                keep = doFilterOnce((String)key, (String)item, page);
                                if (!keep) {
                                    break ONE;
                                }
                            } else {
                                String ope = (String)((Map)item).keySet().iterator().next();
                                String express = (String)((Map)item).get(ope);
                                keep = doFilterOnce((String)key, express, page);
                                if ("not".equalsIgnoreCase(ope) || "mustnot".equalsIgnoreCase(ope)) { //mast not
                                    keep = !keep;
                                    if (!keep) {
                                        break ONE;
                                    }
                                } else if ("should".equalsIgnoreCase(ope)){
                                    if (keep) {
                                        break ONE;
                                    }
                                } else if ("shouldnot".equalsIgnoreCase(ope)){
                                    keep = !keep;
                                    if (keep) {
                                        break ONE;
                                    }
                                } else { //master
                                    if (!keep) {
                                        break ONE;
                                    }
                                }
                            }
                            String ope = (String)((Map)item).keySet().iterator().next();
                        }
                    } else {
                        String ope = (String)((Map)value).keySet().iterator().next();
                        String express = (String)((Map)value).get(ope);
                        keep = doFilterOnce((String)key, express, page);
                        if ("not".equalsIgnoreCase(ope) || "mustnot".equalsIgnoreCase(ope)) { //mast not
                            keep = !keep;
                            if (!keep) {
                                break;
                            }
                        } else if ("should".equalsIgnoreCase(ope)){
                            if (keep) {
                                break;
                            }
                        } else if ("shouldnot".equalsIgnoreCase(ope)){
                            keep = !keep;
                            if (keep) {
                                break;
                            }
                        } else { //master
                            if (!keep) {
                                break;
                            }
                        }
                    }
                }
                if (keep) {
                    LOGGER.debug("keep item: " + page.toString());
                    resList.add(page);
                } else {
                    LOGGER.info("skip item: " + page.toString());
                }
            }
        }
        return resList;
    }
    
    protected Boolean doFilterOnce(String key, String filterExpress, Object page) {
        if ("script".equalsIgnoreCase(key)) {
            String res = doScript(filterExpress, page);
            return Boolean.valueOf(res);
        } else if ("regex".equalsIgnoreCase(key)) {
            Pattern pattern = Pattern.compile(filterExpress);
            Matcher matcher = pattern.matcher(page.toString());
            return matcher.find();
        } else {
            throw new RuntimeException("不支持的filter检查方式：" + key);
        }
    }
    
//    public List doFilterRegex(List obj, String regex) {
//        List resList = new ArrayList();
//        Pattern pattern = Pattern.compile(regex);
//        for (Object page : (List) obj) {
//            Matcher matcher = pattern.matcher(page.toString());
//            if (matcher.find()) {
//                resList.add(page);
//                //System.out.print("+");
//            } else {
//                //System.out.print("-");
//            }
//        }
//        return resList;
//    }
//    
//    public List doFilterScript(List obj, String filterStr) {
//        if (StringUtils.isBlank(filterStr)) {
//            return obj;
//        }
//        List resList = new ArrayList();
//        if (obj instanceof List) {
//            for (Object page : (List)obj) {
//                if (doFilterOnce("script", filterStr, page)) {
//                    resList.add(page);
//                }
//            }
//        } else {
//            if (doFilterOnce("script", filterStr, obj)) {
//                resList.add(obj);
//            }
//        }
//        return resList;
//    }
//    
    protected List extract(Map extract, List pages) {
        for (Object key: extract.keySet()) {
            if ("filter".equalsIgnoreCase(key.toString())) {
                return (List)doFilter(pages, extract.get(key));
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
                return doRegex(pages, extract.get(key));
            } else if ("marshal".equalsIgnoreCase(key.toString())) {
                return doMarshal(pages, (Map)extract.get("marshal"));
            } else if ("unmarshal".equalsIgnoreCase(key.toString())) {
                return doUnmarshal(pages, (Map)extract.get("unmarshal"));
            }
        }
        return pages;
    }
    
    protected Object propertyExtract(Map extract, Object it, Map result) {
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
                    templateParamMap.put("_item", result);
                    return JavaScriptExecuter.exec(express, templateParamMap);
                } else if ("script".equalsIgnoreCase(key.toString())) {
                    templateParamMap.put("it", it);
                    templateParamMap.put("_item", result);
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
     * 这个方法有问题，只要调用一遍step里的内容就改掉了，以后再也不会重新计算了。
     * @param keys 
     */
    protected void beforeGatherExpressCalcu(String ... keys) {
        for (String key : keys) {
            doExpressCalcu(step, key);
        }
    }
    
    protected void doExpressCalcu(Map parent,String key) {
        Object obj = parent.get(key);
        if (obj == null) {
            return;
        }
        if (obj instanceof Map) {
            for (Object k : ((Map)obj).keySet()) {
                doExpressCalcu((Map)obj, (String)k);
            }
        } else if (obj instanceof List) {
            for (int i = 0; i < ((List)obj).size(); i++) {
                Object o = ((List)obj).get(i);
                if (o instanceof String) {
                    if (maybeScript((String)o)) {
                        ((List)obj).remove(i);
                        String v = doScript((String)obj);
                        ((List)obj).add(i, v);
                    }
                } else if (o instanceof Map) {
                    for (Object k: ((Map)o).keySet()) {
                        doExpressCalcu((Map)o, (String)k);
                    }
                } else {
                    throw new RuntimeException("yaml is not good format! array's child can not array. key=" + key);    
                }
            }
        } else if (obj instanceof String) { //string
            if (maybeScript((String)obj)) {
                String v = doScript((String)obj);
                parent.put(key, v);
            }
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
        if (save == null) {
            return;
        }
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
    
    protected void save(Map saveDefMap, List pages, Object ourl) {
        pages = setProperties(pages, ourl, (Map)saveDefMap.get("property"));
        String varName = doScript((String)saveDefMap.get("var"));
        String resourceName = doScript((String)saveDefMap.get("resource"));
        String endpoint = doScript((String)saveDefMap.get("endpoint"));
        String toName = doScript((String)saveDefMap.get("to"));
        String filterExpress = (String)saveDefMap.get("filter");
        BeeResource resource = null;
        List var = null;
        if (StringUtils.isNotBlank(varName)) {
            var = beeGather.getVar(varName);
        } else if (StringUtils.isNotBlank(toName)){
            resource = beeGather.getResourceMng().getResource(toName);
            if (resource == null) {
                var = beeGather.getVar(toName);
            }
        }
        if (StringUtils.isNotBlank(resourceName)) {
            resource = beeGather.getResourceMng().getResource(resourceName);
        }
        for (Object page : pages) {
            removeProperties(page);
            if (StringUtils.isNotBlank(filterExpress) && !doFilterOnce("script", filterExpress, page)) {
                continue;
            }
            if (var != null) {
                var.add(page);
            }
            if (resource != null) {
                saveToResource(resourceName, page, ourl, saveDefMap);
            }
            if (StringUtils.isNotBlank(endpoint)) {
                saveToResource("camel", page, ourl, saveDefMap);
            }
        }
        
//        for (Object page : pages) {
//            removeProperties(page);
//            if (StringUtils.isNotBlank(filterExpress) && !doFilterOnce("script", filterExpress, page)) {
//                continue;
//            }
//            if (StringUtils.isNotBlank(varName)) {
//                smartSaveTo(varName, page, ourl, saveDefMap);
//            } else {
//                if (StringUtils.isNotBlank(resourceName)) {
//                    resourceName = doScript(resourceName, page);
//                    saveToResource(resourceName, page, ourl, saveDefMap);
//                } else {
//                    if (StringUtils.isNotBlank(toName)) {
//                        smartSaveTo(toName, page, ourl, saveDefMap);
//                    } else if (saveDefMap.containsKey("endpoint")) {
//                        saveToResource("camel", page, ourl, saveDefMap);                        
//                    }
//                }
//            }
//        }
    }
    
    protected void saveToVar(String varName, Object page, Object ourl) {
        //varName = doScript(varName, page);
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

    public String doScript(String script) {
        if (maybeScript(script)) {
            return template.expressCalcu(script, templateParamMap);
        }
        return script;
    }
    
    public String doScript(String script, Object it) {
        templateParamMap.put("it", it);
        if (maybeScript(script)) {
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
        Long sleep = 0L;
        Object oSleep = step.get("sleep");
        if (oSleep != null) {
            if (oSleep instanceof String) {
                String sSleep = ((String)oSleep).trim().toLowerCase();
                if (sSleep.endsWith("ms")) {
                    sleep = Long.valueOf(sSleep.substring(0, sSleep.indexOf("ms")));
                } else if (sSleep.endsWith("s")) {
                    sleep = Long.valueOf(sSleep.substring(0, sSleep.indexOf("s")));
                    sleep = sleep*1000L;
                } else if (sSleep.endsWith("m")) {
                    sleep = Long.valueOf(sSleep.substring(0, sSleep.indexOf("m")));
                    sleep = sleep*60L*1000L;
                }
            } else {
                sleep = Long.valueOf(oSleep.toString());
            }
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
                    LOGGER.warn("marshal fail it:" + it.toString(), e);
                }
            }
            return returnList;
        }        
        return its;
    }

    protected List setProperties(List its, Object ourl, Map propertyMap) {
        //Map propertyMap = (Map) save.get("property");
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
        Map sobj = new HashMap();
       if (Map.class.isAssignableFrom(it.getClass())) {
            result = (Map) it;
            sobj.putAll((Map) it);
        } else {
            result = new HashMap();
            result.put("_", it);
            sobj.put("_", it);
        }
       
        for (Object key : propertyMap.keySet()) {
            Object ov = null;
            try {
                Object propertyPropDef = propertyMap.get(key);

                String type = null;
                String propScript = null;
    //            String scope = null;
                if (propertyPropDef instanceof String) {
                    String def = (String)propertyPropDef;
                    Map m1 = new HashMap();
                    if ("_this".equalsIgnoreCase(def)) {
                        result.put(key, templateParamMap.get("_this"));
                        continue;
                    } else if ("_page".equalsIgnoreCase(def)) {
                        result.put(key, templateParamMap.get("_page"));
                        continue;
                    } else if ("_item".equalsIgnoreCase(def)) {
                        result.put(key, it);
                        continue;
                    } else if (withVarCurrent != null && withVarCurrent instanceof Map && ((Map)withVarCurrent).containsKey(def)) {
                        result.put(key, ((Map)withVarCurrent).get(def));
                        continue;
                    } else if (def.contains("/")) {
                        m1.put("xpath", def);
                    } else if (def.startsWith("$.") || def.startsWith("$[")){
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
                    //ov 是 it,如果it是map，ov是map.get(key)
                    for (Object extract : extractList) {
                        if (ov instanceof Map) {
                            ov = ((Map)ov).get(key);
                        }                        
                        ov = propertyExtract((Map)extract, ov, sobj);
                        //templateParamMap.put("it", ov);
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
//                    if (ov == null && aim instanceof String) {
//                        ov = aim;
//                    }
                    if (StringUtils.isNotBlank(propScript)) {
                        templateParamMap.put("it", ov);
                        templateParamMap.put("_item", sobj.get("_"));
                        ov = template.expressCalcu(propScript, templateParamMap);
                    }
                    String javaScriptExpress = (String)propValue.get("javascript");
                    if (StringUtils.isNotBlank(javaScriptExpress)) {
                        templateParamMap.put("it", ov);
                        templateParamMap.put("_item", sobj.get("_"));
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
                Object regex = null;
                if (propertyParam instanceof Map) {
                    path = (String) ((Map) propertyParam).get("xpath");
                    regex = ((Map) propertyParam).get("regex");
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
                
                if (regex != null) {
                    value = doRegex(value, regex);
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
        List heads = (List)get.get("head");
        if ("csv".equalsIgnoreCase(type)) {
            String split = ResourceUtils.get(get, "split", ",");
            Map map = new HashMap();
            String[] ps = page.toString().split(split);
            for (int i = 0; i < ps.length; i++) {
                if (heads == null && i >= heads.size()) {
                    map.put("f" + i, ps[i]);
                } else {
                    map.put(heads.get(i), ps[i]);
                }
            }
            return map;
        } else {
            throw new RuntimeException("not support unmarshal type: " + type);
        }
    }

    private String marshal(Object page, Map get) throws Exception {
        String result = null;
        String type = (String) get.get("type");
        if ("json".equalsIgnoreCase(type)) {
            result = JSON.toJSONString(page);
        } else if ("csv".equalsIgnoreCase(type)) {
            CSVFormat format = CSVFormat.DEFAULT;
            if (get.containsKey("delimiter")) {
                format = CSVFormat.newFormat(((String)get.get("delimiter")).trim().charAt(0));
            }
            List headList = (List)get.get("head");
            Object[] values = CSVUtils.getValues(headList, page);
            result = format.format(values);
        } else {
            throw new RuntimeException("not support m"
                    + "arshal type: " + type);
        }
        if (get.containsKey("newline")) {
            if ("head".equalsIgnoreCase((String)get.get("newline"))) {
                result = "\n" + result;
            } else {
                result += "\n";
            }
        }
        return result;
    }

    private Map cloneMap(Map rStep) {
        return (Map)Yaml.load(Yaml.dump(rStep));
    }
}

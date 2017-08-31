/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.htmlcleaner.XPather;
import org.shenjitang.beepasture.debug.DebugLevel;
import org.shenjitang.beepasture.debug.GatherDebug;
import org.shenjitang.beepasture.function.JavaScriptExecuter;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.http.PageAnalyzer;
import org.shenjitang.beepasture.resource.BeeResource;
import org.shenjitang.beepasture.resource.ResourceMng;
import org.shenjitang.beepasture.resource.util.ResourceUtils;
import org.shenjitang.beepasture.util.ParseUtils;
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
    protected Map withVarCurrent;
    protected Long count = 0L;
    protected String rurl; //yaml中url的值（脚本中的源句）
    protected Object ourl; //yaml中url进过计算得到的最终的url，可能是http:这样的字符串，也肯能是一个resource.
    protected final Long limit;
    protected final Map save;
    protected Object xpath;
    protected Map heads;
    protected Boolean free;
    protected final Map templateParamMap = new HashMap();
    protected final String script; //yamel中script或template的值（脚本中的源句）
    protected Integer id;
    public static volatile long activeTime = System.currentTimeMillis();

    public GatherStep(Map step, Integer id) {
        this.id = id;
        pageAnalyzer = new PageAnalyzer();
        this.rStep = step;
        this.beeGather = BeeGather.getInstance();
        String withVarName = (String) getValue(step, "with", (String) null);
        if (StringUtils.isNotBlank(withVarName)) {
            if (beeGather.getResourceMng().getResource(withVarName, false) == null) {
                withVar = (List) beeGather.getVars().get(withVarName);
            }
        }
        rurl = (String) rStep.get("url");
        free = rStep.get("free") != null;
        limit = GatherStep.getLongValue(rStep, "limit");
        save = (Map) rStep.get("save");
        xpath = rStep.get("xpath");
        heads = (Map) rStep.get("head");
        if (heads == null) {
            heads = (Map) rStep.get("heads");
        }
        script = beeGather.getScript(rStep);
    }

    public Integer getId() {
        return id;
    }
    
    public String changeValueFromObj(String value, Object obj) {
        return obj instanceof Map && ((Map)obj).containsKey(value) ? (String)((Map)obj).get(value) : value;
    }

    public void execute() throws Exception {
        rurl = ParseUtils.maybeScript(rurl) ? doScript(rurl): rurl;
        List urls = withVar == null? getUrlsFromStepUrl(rurl, rStep) : withVar;        
        for (int i = 0; i < urls.size(); i++) {
            ourl = urls.get(i);
            activeTime = System.currentTimeMillis();
            if (ourl instanceof Map) {
                withVarCurrent = (Map)ourl;
            }
            templateParamMap.put("_this", ourl);
            if (withVar != null) {
                ourl = ((Map) ourl).get(rurl);            
            }
            templateParamMap.put("it", ourl);
            cloneStep();
            if (withVar != null) {
                step.put("withVarCurrent", withVarCurrent);
            }         
            GatherDebug.debug(this, DebugLevel.GATHER, "开始执行gather：" + id);
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
            if (free) {
                urls.remove(i);
                i--;
            }
        }
    }
    
    public void cloneStep() {
            step = (Map)cloneMap(rStep);
            beforeGatherExpressCalcu("download", "sql");
    }

    /**
     * 做一次gather.
     * @param ourl 将rurl转换成需要处理的列表后的一条url，如果有withVar就是withVar的第一条。
     */
    public void onceGather(Object ourl) throws Exception {
        GatherDebug.debug(this, DebugLevel.STEP, "开始执行step：" + rurl);
        try {
            Object page = ourl;
            Boolean direct = ResourceUtils.get(step, "direct", false);
            if (!direct) {
                if (ParseUtils.maybeScript(ourl)) {
                    ourl = template.expressCalcu((String) ourl, beeGather.getVars());
                } else if (ourl instanceof File) {
                    ourl = "file://" + ((File)ourl).getAbsolutePath();
                }
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
            List pages = ParseUtils.toList(page);
            try {
                List extractList = (List)step.get("extract");
                if (extractList != null) {
                    for (Object extract : extractList) {
                        pages = extract((Map)extract, pages);
//                        GatherDebug.debug(this, "执行完语句：" + JSON.toJSONString(extract));
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
            save(pages, ourl);
        } catch (Exception e) {
            LOGGER.warn("", e);
        }
    }
    
    public void onceFlow(Object ourl) {
        try {
            if (ParseUtils.maybeScript(ourl)) {
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
                        BeeResource beeResource = beeGather.getResourceMng().getResource(url, false);
                        Iterator ite = beeResource.iterate(loadParam);
                        if (ite == null) {
                            MAIN_LOGGER.warn("skip " + url + "    cause: not find resource");
                            return;
                        }
                        while(ite.hasNext()) {
                            Object page = ite.next();
                            templateParamMap.put("_page", page);
                            List pages = ParseUtils.toList(page);
                            List extractList = (List)step.get("extract");
                            if (extractList != null) {
                                for (Object extract : extractList) {
                                    pages = extract((Map)extract, pages);
//                                    GatherDebug.debug(this, "执行完语句：" + JSON.toJSONString(extract));
                                }
                            }
                            pages = doFilter(pages, step.get("filter"));
                            pages = doXpath(pages);
                            pages = doRegex(pages, step.get("regex"));
                            pages = doScript(pages, changeValueFromObj(script, ourl));
                            pages = doJavaScript(pages, (String) step.get("javascript"));
                            pages = doMarshal(pages, (Map)step.get("marshal"));
                            pages = doUnmarshal(pages, (Map)step.get("unmarshal"));
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
        return beeGather.getVars().containsKey(url) ? 
                (List)beeGather.getVar(url) : 
                Lists.newArrayList(url);
    }
   
    protected Map getLoadParam() {
        //资源装载的params里的参数，如果是对vars的引用，就要替换成vars立的值。
//        Object p = step.get("param");
//        if (p == null) {
//            return new HashMap();
//        } else {
//            return (Map)p;
//        }
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
        GatherDebug.debug(this, DebugLevel.STATEMENT, "加载资源：" + url);
        BeeResource beeResource = beeGather.getResourceMng().getResource(url, false);
        return beeResource.loadResource(getLoadParam());
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
        GatherDebug.debug(this, DebugLevel.STATEMENT, "xpath: " + xpath);
        Object _this = templateParamMap.get("_this");
        xpath = changeValueFromObj(xpath, _this);
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
        } else if (xpath.startsWith("innerHtml(")){
            try {
                String path = xpath.substring(10, xpath.length() - 1);
                TagNode node = pageAnalyzer.toTagNode(page);
                List pages = pageAnalyzer.getList(node, path, true);
                if (pages == null || pages.isEmpty()) {
                    LOGGER.warn("xpath不能抓到合适的内容！ xpath:" + xpath);
                }
                return pages;
            } catch (Exception e) {
                LOGGER.warn("xpath:" + xpath + "  page:" + page, e);
                return new ArrayList();
            }
        } else if (xpath.startsWith("remove(")){
            try {
                String path = xpath.substring(7, xpath.length() - 1);
                TagNode node = pageAnalyzer.toTagNode(page);
                XPather xPather = new XPather(path);
                Object[] objs = xPather.evaluateAgainstNode(node);
                List resList = new ArrayList();
                for (Object o : objs) {
                    if (o instanceof TagNode) {
                        node.removeChild(o);
                    }
                }
                List pages = new ArrayList();
                pages.add(node);
                return pages;
            } catch (Exception e) {
                LOGGER.warn("xpath:" + xpath + "  page:" + page, e);
                return new ArrayList();
            }
        } else {
            try {
                TagNode node = pageAnalyzer.toTagNode(page);
                List pages = pageAnalyzer.getList(node, xpath, false);
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
        } else if (xpath.startsWith("innerHtml(")){
            try {
                String path = xpath.substring(10, xpath.length() - 1);
                List pages = pageAnalyzer.getList(page, path, true);
                if (pages == null || pages.isEmpty()) {
                    LOGGER.warn("xpath不能抓到合适的内容！ xpath:" + xpath);
                }
                return pages;
            } catch (Exception e) {
                LOGGER.warn("xpath:" + xpath + "  page:" + page, e);
                return new ArrayList();
            }
        } else if (xpath.startsWith("remove(")){
            try {
                String path = xpath.substring(7, xpath.length() - 1);
                removeChild(page, path);
                List pages = new ArrayList();
                pages.add(page);
                return pages;
            } catch (Exception e) {
                LOGGER.warn("xpath:" + xpath + "  page:" + page, e);
                return new ArrayList();
            }
        } else {
            try {
                List pages = pageAnalyzer.getList(page, xpath, false);
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
                if (res instanceof List) {
                    list.addAll((List)res);
                } else {
                    list.add(res);
                }
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
        for (Object page : pages) {
            try {
                if (page instanceof TagNode) {
                    page = ((TagNode)page).getText();
                }
                templateParamMap.put("it", page);
                GatherDebug.debug(this, DebugLevel.STATEMENT, "script: " + ascript);
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
        GatherDebug.debug(this, DebugLevel.STATEMENT, "regex: " + regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(groupIndex);
        }
        return null;
    }
    
    public String doRegex(String page, String regex) {
        GatherDebug.debug(this, DebugLevel.STATEMENT, "regex: " + regex);
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
                    if (withVar != null) {
                        try {
                            ((List)withVar).remove(withVarCurrent);
                        } catch (Exception e) {
                            LOGGER.warn("这里必须是list。不是list，肯定有问题，需要检查。", e);
                        }
                    }
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
                    if (withVar != null) {
                        try {
                            ((List)withVar).remove(withVarCurrent);
                        } catch (Exception e) {
                            LOGGER.warn("这里必须是list。不是list，肯定有问题，需要检查。", e);
                        }
                    }
                    LOGGER.info("skip item: " + page.toString());
                }
            }
        }
        return resList;
    }
    
    protected Boolean doFilterOnce(String key, String filterExpress, Object page) {
        GatherDebug.debug(this, DebugLevel.STATEMENT, "filter " + key + ": " + filterExpress);
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
    
    protected List extract(Map extract, List pages) {
        for (Object key: extract.keySet()) {
            if ("filter".equalsIgnoreCase(key.toString())) {
                return (List)doFilter(pages, extract.get(key));
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
            } else if ("dataimage".equalsIgnoreCase(key.toString())) {
                return dataimageAll(pages);
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
                    return template.expressCalcu(express, templateParamMap);
                } else if ("regex".equalsIgnoreCase(key.toString())) {
                    Pattern pattern = Pattern.compile(express);
                    Matcher matcher = pattern.matcher(it.toString());
                    if (matcher.find()) {
                        return matcher.group();
                    }
                } else if ("dataimage".equalsIgnoreCase(key.toString())) {
                    return dataimage(it);
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
                    if (ParseUtils.maybeScript((String)o)) {
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
            if (ParseUtils.maybeScript((String)obj)) {
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
            GatherDebug.debug(this, DebugLevel.STATEMENT, "script: " + script);
            return template.expressCalcu(script, it, page, ourl, beeGather.getVars());
        } else {
            return it;
        }
    }
    
    public Map saveStr2map(Object srcTo) {
        Map toMap = new HashMap();
        toMap.putAll(save);
        toMap.remove("to");
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
        if (pages.isEmpty()) return;
        pages = setProperties(pages, ourl, (Map)saveDefMap.get("property"));
        String varName = doScript((String)saveDefMap.get("var"));
        String resourceName = doScript((String)saveDefMap.get("resource"));
        String endpoint = doScript((String)saveDefMap.get("endpoint"));
        String toName = null;
        Object to = saveDefMap.get("to");
        if (to != null) {
            if (to instanceof String) {
                toName = doScript((String)to);
            } else if (to instanceof Map) {
                String k = (String)((Map)to).keySet().iterator().next();
                if ("resource".equalsIgnoreCase(k)) {
                    resourceName = doScript((String)((Map)to).get(k));
                } else if ("var".equalsIgnoreCase(k)) {
                    varName = doScript((String)((Map)to).get(k));
                } else {
                    throw new RuntimeException ("save: to: key: value. key is wrong, mast be resource or var");
                }
            } else {
                throw new RuntimeException ("save key's value mast be string or map!");
            }
        }
        
        String filterExpress = (String)saveDefMap.get("filter");
        if ("_this".equalsIgnoreCase(varName) || "_this".equalsIgnoreCase(toName)) {
            Map page = (Map)pages.get(pages.size() - 1);
            withVarCurrent.putAll(page);
            return;
        }
        BeeResource resource = null;
        List var = null;
        if (StringUtils.isNotBlank(varName)) {
            var = beeGather.getVar(varName);
        } else if (StringUtils.isNotBlank(toName)){
            resource = beeGather.getResourceMng().getResource(toName, false);
            if (resource == null) {
                var = beeGather.getVar(toName);
            }
        }
        if (StringUtils.isNotBlank(resourceName)) {
            resource = beeGather.getResourceMng().getResource(resourceName, false);
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
            } else if (StringUtils.isNotBlank(endpoint)) {
                saveToResource("camel", page, ourl, saveDefMap);
            }
        }       
    }
    
    protected void saveToVar(String varName, Object page, Object ourl) {
        //varName = doScript(varName, page);
        if ("_this".equalsIgnoreCase(varName)) {
            withVarCurrent.putAll((Map) page);
        } else {
            List toList = beeGather.getVar(varName);
            toList.add(page);
        }
    }
    
    protected void saveToResource(String name, Object page, Object ourl, Map saveDefMap) {
        BeeResource resource = beeGather.getResourceMng().getResource(name, false);
        if (resource != null) {
            beeGather.getVars().remove(name);
            resource.persist("it", page, saveDefMap);
        } else {
           throw new RuntimeException("can not find resource: " + name);
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
        if (ParseUtils.maybeScript(script)) {
            GatherDebug.debug(this, DebugLevel.STATEMENT, "script: " + script);
            return template.expressCalcu(script, templateParamMap);
        }
        return script;
    }
    
    public String doScript(String script, Object it) {
        templateParamMap.put("it", it);
        if (ParseUtils.maybeScript(script)) {
            GatherDebug.debug(this, DebugLevel.STATEMENT, "script: " + script);
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
        }
        templateParamMap.put("_item", sobj);
       
        for (Object key : propertyMap.keySet()) {
            Object ov = null;
            try {
                Object propertyPropDef = propertyMap.get(key);
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
                    } else if ("it".equalsIgnoreCase(def)) {
                        result.put(key, it);
                        continue;
                    } else if (withVarCurrent != null && withVarCurrent.containsKey(def)) {
                        result.put(key, withVarCurrent.get(def));
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
                String type = (String) propValue.get("type");
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
                        ov = propertyExtract((Map)extract, ov);
                        //templateParamMap.put("it", ov);
                        GatherDebug.debug(this, DebugLevel.STATEMENT, "执行完语句：" + JSON.toJSONString(extract));
                    }
                } else {
                    String propScript = beeGather.getScript(propValue);
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
                            if ("millisecond".equalsIgnoreCase(format) || "ms".equalsIgnoreCase(format)) {
                                Long ms = Long.valueOf(ov.toString());
                                ov = new Date();
                                ((Date)ov).setTime(ms);
                            } else {
                                String locate = (String)(propValue).get("locate");
                                SimpleDateFormat sdf = null;
                                if (StringUtils.isBlank(locate)) {
                                    if (ov.toString().contains("MMM")) {
                                        locate = "ENGLISH";
                                    }
                                }
                                if (StringUtils.isBlank(locate)) {
                                    sdf = new SimpleDateFormat(format);
                                } else {
                                    sdf = new SimpleDateFormat(format, Locale.forLanguageTag(locate));
                                }
                                if (StringUtils.isNotBlank(ov.toString())) {
                                    ov = sdf.parse(ParseUtils.correctDateStr(ov.toString()));
                                } else {
                                    ov = null;
                                }
                            }
                        } else if ("String[]".equalsIgnoreCase(type)) {
                            String split = getValue(propValue, "split", ",");
                            ov = ((String) ov).split(split);
                        } else if ("bool".equalsIgnoreCase(type) || "Boolean".equalsIgnoreCase(type)) {
                            ov = Boolean.valueOf(ov.toString());
                        } else if ("int".equalsIgnoreCase(type) || "Integer".equalsIgnoreCase(type)) {
                                ov = Integer.valueOf(ov.toString());
                        } else if ("long".equalsIgnoreCase(type)) {
                            if (ov instanceof Date) {
                                ov = ((Date)ov).getTime();
                            } else {
                                ov = Long.valueOf(ov.toString());
                            }
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
            GatherDebug.debug(this, DebugLevel.STATEMENT, "提取完字段：" + key + " = " + ov);
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



    private Map unmarshal(Object page, Map get) {
        String type = (String) get.get("type");
        GatherDebug.debug(this, DebugLevel.STATEMENT, "unmarshal " + type);
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
        GatherDebug.debug(this, DebugLevel.STATEMENT, "marshal " + type);
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

    private List dataimageAll(List pages) {
        List list = new ArrayList();
        for (Object page : pages) {
            list.add(dataimage(page));
        }
        return list;
    }
    
    private TagNode dataimage(Object page) {
            try {
                if (page instanceof TagNode) {
                    return dataimage((TagNode)page);
                } else {
                    return dataimage(page.toString());
                }
            } catch (Exception e) {
                LOGGER.warn("dataimage", e);
            }
            return null;
    }
    
    private TagNode dataimage(TagNode tagNode) throws Exception {
        TagNode[] allNode = tagNode.getAllElements(true);
        for (TagNode node : allNode) {
            if ("img".equalsIgnoreCase(node.getName())) {
                String imgUrl = node.getAttributeByName("src");
                if (StringUtils.isNotBlank(imgUrl)) {
                    if (!imgUrl.toLowerCase().startsWith("http:")) {
                        imgUrl = StringUtils.substringBeforeLast(ourl.toString(), "/") + "/" + imgUrl;
                    }
                    LOGGER.info("image:" + imgUrl);
                    BeeResource beeResource = beeGather.getResourceMng().getResource(imgUrl, false);
                    String str = (String)beeResource.loadResource(ImmutableMap.of("dataimage", Boolean.TRUE));
                    node.removeAttribute("src");
                    node.addAttribute("src", str);
                }
            }
        }
        return tagNode;
    }
    
    private TagNode dataimage(String str) throws Exception {
//        if (!str.startsWith("<")) {
//            str = "<div id=\"auto-add\">" + str + "</div>";
//        }
        TagNode node = pageAnalyzer.toTagNode(str);
        return dataimage(node);
//        throw new RuntimeException("data-image 目前只能处理TagNode");
    }

    private void removeChild(TagNode page, String tagNode) {
        for (TagNode cp : page.getChildTags()) {
            if (cp.getName().equals(tagNode)) {
                page.removeChild(cp);
            }
            if (cp.getChildTags().length > 0) {
                removeChild(cp, tagNode);
            }
        }
        
    }
}

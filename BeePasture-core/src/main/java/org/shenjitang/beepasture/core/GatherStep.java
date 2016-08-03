/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import com.jayway.jsonpath.JsonPath;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlcleaner.TagNode;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.http.HttpTools;
import org.shenjitang.beepasture.http.PageAnalyzer;
import org.shenjitang.beepasture.resource.BeeResource;

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
            withVar = (List) beeGather.getVar(withVarName);
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
            urls = beeGather.getUrlsFromStepUrl(rurl, step); //如果是资源，也是从这里加载了
        }
        for (Object ourl : urls) {
            onceGather(ourl);
            if (limit != null && ++count >= limit) {
                break;
            }
        }
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
    
    private List doXpath(List page, String xpath) {    
        List pageList = new ArrayList();
        for (Object p : (List)page) {
            if (p instanceof TagNode) {
                try {
                    pageList.addAll(pageAnalyzer.getList((TagNode)p, xpath));
                } catch (Exception e) {
                    LOGGER.warn("xpath:" + xpath + "  page:" + p.toString(), e);
                }
            } else {
                pageList.addAll(doXpath(p.toString(), xpath));
            }
        }
        return pageList;
    }
    
    private List doXpath(String page, String xpath) {    
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
                return pageAnalyzer.getList(node, xpath);
            } catch (Exception e) {
                LOGGER.warn("xpath:" + xpath + "  page:" + page, e);
                return new ArrayList();
            }
        }
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
    public List doScript(List pages) {
        if (StringUtils.isBlank(script)) {
            return pages;
        }
        List list = new ArrayList();
        templateParamMap.putAll(beeGather.getVars());
        for (Object page : pages) {
            try {
                //templateParamMap.put("_page", page);
                templateParamMap.put("it", page);
                String res = template.expressCalcu(script, templateParamMap);
                list.add(res);
            } catch (Exception e) {
                LOGGER.warn("template:" + script + "  page:" + page, e);
            }
        }
        return list;
    }
    
    /**
     * 對從url中取得的內容做正則處理
     * @param pages
     * @return 
     */
    public List doRegex(List pages) {
        String regex = (String) step.get("regex");
        if (StringUtils.isNotBlank(regex)) {
            List list = new ArrayList();
            templateParamMap.putAll(beeGather.getVars());
            for (Object page : pages) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(page.toString());
                if (matcher.find()) {
                    String value = matcher.group();
                    list.add(value);
                }
            }
            return list;
        } else {
            return pages;
        }
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
            if (needExpressCalcu(ourl)) {
                ourl = template.expressCalcu((String) ourl, beeGather.getVars());
            } else if (ourl instanceof File) {
                ourl = "file://" + ((File)ourl).getAbsolutePath();
            }
            templateParamMap.put("_this", ourl);
            if (ourl instanceof String && ourl.toString().toLowerCase().contains(":")) {
                try {
                    BeeResource resource = beeGather.getResourceMng().getResource((String)ourl);
                    if (resource != null) {
                        page = resource.loadResource(step);
                    }
                } catch (Exception e) {
                    LOGGER.warn("unknown resource:" + ourl, e);
                }
            }
            templateParamMap.put("_page", page);
            List pages = toList(page);
            pages = doXpath(pages);
            pages = doRegex(pages);
            pages = doScript(pages);
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
        List removePropertyList = (List) saveTo.get("removePropety");
        if (removePropertyList != null) {
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
                List toList = beeGather.getVar(name);
                if (withVar != null) {
                    if (ourl instanceof Map && !((Map) ourl).containsKey(name)) {
                        ((Map) ourl).put(name, toList);
                    }
                }
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
            Object propValue = propertyMap.get(key);
            
            String type = null;
            String propScript = null;
            String scope = null;
            if (propValue instanceof Map) {
                propScript = beeGather.getScript((Map) propValue);
                type = (String) ((Map) propValue).get("type");
                scope =  (String) ((Map) propValue).get("scope");
            }
            
            Object aim = it;
            if (StringUtils.isNotBlank(scope)) {
                aim = templateParamMap.get(scope.trim());
                if (aim == null) {
                    aim = it;
                }
            }
            
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
            if (StringUtils.isNotBlank(type) && ov != null) {
                try {
                    if ("date".equalsIgnoreCase(type)) {
                        String format = getValue((Map) propValue, "format", "yyyy-MM-dd HH:mm:ss");
                        SimpleDateFormat sdf = new SimpleDateFormat(format);
                        if (StringUtils.isNotBlank(ov.toString())) {
                            ov = sdf.parse(ov.toString());
                        } else {
                            ov = null;
                        }
                    } else if ("String[]".equalsIgnoreCase(type)) {
                        String split = getValue((Map) propValue, "split", ",");
                        ov = ((String) ov).split(split);
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
    
}

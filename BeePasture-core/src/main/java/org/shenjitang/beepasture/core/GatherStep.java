/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import com.jayway.jsonpath.JsonPath;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.htmlcleaner.TagNode;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.http.HttpTools;
import org.shenjitang.beepasture.http.PageAnalyzer;

/**
 *
 * @author xiaolie
 */
public class GatherStep {

    public static Log MAIN_LOGGER = LogFactory.getLog("org.shenjitang.beepasture.core.Main");
    private final Map step;
    private final BeeGather beeGather;
    private final ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    private final HttpTools httpTools;
    private final PageAnalyzer pageAnalyzer;
    private static final Log LOGGER = LogFactory.getLog(GatherStep.class);
    private List withVar;
    private final String rurl;
    private final Long limit;
    private final Map saveTo;
    private String toVar = null;
    private final Map download;
    private final Boolean direct;
    private final String xpath;
    private final String charset;
    private Map heads;

    public GatherStep(Map step, BeeGather beeGather) {
        httpTools = new HttpTools();
        pageAnalyzer = new PageAnalyzer();
        this.step = step;
        this.beeGather = beeGather;
        String withVarName = (String) getValue(step, "with", (String) null);
        if (StringUtils.isNotBlank(withVarName)) {
            withVar = (List) beeGather.getVar(withVarName);
        }
        rurl = (String) step.get("url");
        limit = getLongValue(step, "limit");
        saveTo = (Map) step.get("save");
        if (saveTo != null) {
            toVar = (String) saveTo.get("to");
        }
        download = (Map) step.get("download");
        direct = (Boolean) step.get("direct");
        xpath = (String) step.get("xpath");
        charset = (String) step.get("charset");
        heads = (Map) step.get("head");
        if (heads == null) {
            heads = (Map) step.get("heads");
        }
    }

    public void execute() throws Exception {
        List urls;
        if (withVar != null) {
            urls = withVar;
        } else {
            urls = beeGather.getUrlsFromStepUrl(rurl, step);
        }
        int count = 0;
        for (Object ourl : urls) {
            onceGather(ourl);
            if (limit != null && ++count >= limit) {
                break;
            }
        }
    }
    
    protected void onceGather(Object ourl) {
        try {
            String url = null;
            if (withVar != null) {
                url = (String) ((Map) ourl).get(rurl);
            } else if (ourl instanceof String) {
                url = template.expressCalcu((String) ourl, null);
            } else if (ourl instanceof File) {
                url = ((File)ourl).getAbsolutePath();
            } else {
                url = ourl.toString();
            }
            String page = null;
            if (direct != null && direct) {
                page = url;
            } else if (ourl instanceof File) {
                String encoding = StringUtils.isBlank(charset) ? "gbk" : charset;
                page = FileUtils.readFileToString((File) ourl, encoding);
            } else if (download != null) {// download to file
                String fileName = null;
                if (withVar != null) {
                    fileName = (String) ((Map) ourl).get(download.get("to"));
                }
                if (fileName == null) {
                    fileName = (String) download.get("to");
                }
                System.out.println("****************1downlod to :" + fileName);
                fileName = template.expressCalcu(fileName, url, null);
                System.out.println("****************2downlod to :" + fileName);
                httpTools.downloadFile(url, fileName);
                String filenameToVar = (String) download.get("filename");
                if (StringUtils.isNotBlank(filenameToVar)) {
                    if (withVar != null) {
                        ((Map) ourl).put(filenameToVar, fileName);
                    } else {
                        beeGather.getVar(filenameToVar).add(fileName);
                    }
                }
                if (saveTo != null) {
                    try {
                        String format = (String) saveTo.get("format");
                        if (format != null && format.trim().equalsIgnoreCase("text")) {
                            String encod = getValue(saveTo, "encoding", StringUtils.isBlank(charset) ? "gbk" : charset);
                            page = readTextFile(fileName, encod);
                        } else {
                            page = parseFile2Text(fileName);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("parse file:" + fileName, e);
                    }
                }
            } else if (url.trim().toLowerCase().startsWith("http:") || url.trim().toLowerCase().startsWith("https:")) {
                String postBody = (String) step.get("post");
                if (StringUtils.isNotBlank(postBody)) {
                    page = httpTools.doPost(url, postBody, heads);
                } else {
                    //if ("gzip".equalsIgnoreCase(contentEncoding)) {
                    //    page = httpTools.doGZipGet(url);
                    //} else {
                    page = httpTools.doGet(url, heads, charset);
                    //}
                }
            } else {
                page = url;
            }
            String tempXpath = xpath;
            if (page != null) {
                if (StringUtils.isBlank(tempXpath)) { //如果没有xpath，整个页面放入变量
                    toVar(toVar, doScript(page), ourl);
                } else {
                    List<String> pages = new ArrayList();
                    pages.add(page);
                    String[] pathList = tempXpath.split(";");
                    if (pathList.length > 1) { //多步骤
                        pages = narrowdown(Arrays.copyOf(pathList, pathList.length - 1), pages);
                        tempXpath = pathList[pathList.length - 1];
                    }
                    for (String p : pages) {
                        toVar(toVar, doXpath(p, tempXpath, (Map) saveTo.get("property")), ourl);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object doScript(Object page) throws Exception {
        String templete = beeGather.getScript(step);
        if (StringUtils.isNotBlank(templete)) {
            return template.expressCalcu(templete, page, null);
        } else {
            return page;
        }
    }

    public void toVar(String name, Object value, Object object) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        if (withVar != null) {
            if (value instanceof List && ((List) value).size() > 0) {
                ((Map) object).put(name, (List) ((List) value).get(0));
            } else {
                ((Map) object).put(name, value);
            }
        } else {
            Object resource = beeGather.getResourceMng().getResource(name);
            if (resource == null) {
                List toList = beeGather.getVar(name);
                if (value instanceof List) {
                    toList.addAll((List) value);
                } else {
                    toList.add(value);
                }
            } else {
                //to resource
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

    protected Long getLongValue(Map map, String key) {
        Object oValue = map.get(key);
        if (oValue != null) {
            return Long.valueOf(oValue.toString());
        } else {
            return null;
        }
    }

//    protected void download(String urlStr, Map step, Long limit) throws Exception {
//        List urls = beeGather.getUrlsFromStepUrl(urlStr);
//        Map saveTo = (Map) step.get("save");
//        String file = (String) step.get("filename");
//        String filenameto = (String) step.get("filenameto");
//        List toFileNameList = null;
//        if (StringUtils.isNotBlank(filenameto)) {
//            toFileNameList = beeGather.getVar(filenameto);
//        }
//        int count = 0;
//        for (Object ourl : urls) {
//            if (++count > limit) {
//                break;
//            }
//            try {
//                String url = template.expressCalcu((String) ourl, null);
//                String fileName = template.expressCalcu(file, url, null);
//                httpTools.downloadFile(url, fileName);
//                if (toFileNameList != null) {
//                    toFileNameList.add(fileName);
//                }
//            } catch (Exception e) {
//                LOGGER.warn("download " + urlStr, e);
//            }
//        }
//    }

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

    protected List doXpath(String page, String xpath, Map propertyMap) throws Exception {
        Object obj = null;
        if (xpath.startsWith("json(")) {
            String jsonPath = xpath.substring(5, xpath.length() - 1);
            Object js = JsonPath.read(page, jsonPath);
            List list = new ArrayList();
            if (js != null) {
                if (js instanceof JSONArray) {
                    list.addAll((JSONArray)js);
                } else {
                    list.add(js);
                }
            }
            obj = list;
        } else if (xpath.startsWith("express(")) {
            String express = xpath.substring(8, xpath.length() - 1);
            Map ps = new HashMap();
            ps.put("time", System.currentTimeMillis());
            ps.put("page", page);
            obj = template.expressCalcu(express, ps);
        } else { //xpath
            TagNode node = pageAnalyzer.toTagNode(page);
            obj = pageAnalyzer.getList(node, xpath);
        }
        List resultList = new ArrayList();
        if (obj instanceof List) {
            for (Object o : (List) obj) {
                resultList.add(setProperties(page, o, propertyMap));
            }
        } else {
            resultList.add(setProperties(page, obj, propertyMap));
        }
        return resultList;
    }

    protected Object setProperties(String page, Object value, Map<String, Object> propertyMap) throws Exception {
        if (propertyMap == null || propertyMap.isEmpty()) {
            return doScript(value);
        }
        Map result;
        if (Map.class.isAssignableFrom(value.getClass())) {
            result = (Map) value;
        } else {
            result = new HashMap();
        }
        for (String key : propertyMap.keySet()) {
            Object ov = null;
            Object propValue = propertyMap.get(key);
            if (propValue instanceof Map) {
                String scope = (String) ((Map)propValue).get("scope");
                if (StringUtils.isNotBlank(scope) && "global".equalsIgnoreCase(scope.trim())) {
                    value = page;
                }
            }
            if (value instanceof String) {
                ov = xpathPropertyObj((String) value, propValue);
            } else if (value instanceof TagNode) {
                ov = xpathPropertyObj((TagNode) value, propValue);
            } else if (value instanceof Map) {
                String script = null;
                String type = null;
                if (propValue instanceof Map) {
                    script = beeGather.getScript((Map) propValue);
                    type = (String) ((Map) propValue).get("type");
                } else if (propValue instanceof String) {
                    script = (String) propValue;
                }
                ov = result.get(key);
                if (ov != null) {
                    if (StringUtils.isNotBlank(script)) {
                        ov = template.expressCalcu(script, result.get(key), result);
                    }
                }
            }
            String type = null;
            if (propValue instanceof Map) {
                type = (String) ((Map) propValue).get("type");
            }
            if (StringUtils.isNotBlank(type) && ov != null) {
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
                    try {
                        ov = Integer.valueOf(ov.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        ov = null;
                    }
                } else if ("long".equalsIgnoreCase(type)) {
                    try {
                        ov = Long.valueOf(ov.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        ov = null;
                    }
                } else if ("double".equalsIgnoreCase(type)) {
                    try {
                        ov = Double.valueOf(ov.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        ov = null;
                    }
                } else if ("float".equalsIgnoreCase(type)) {
                    try {
                        ov = Float.valueOf(ov.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        ov = null;
                    }
                }
            }
            result.put(key, ov);

        }

        return result;
    }

    public Object xpathPropertyObj(String page, Object propertyParam) throws Exception {
            String value = null;
            try {
                String path = null;
                String script = null;
                if (propertyParam instanceof Map) {
                    path = (String) ((Map) propertyParam).get("xpath");
                    script = beeGather.getScript((Map) propertyParam);
                } else {
                    path = propertyParam.toString();
                }
                
                if (path.startsWith("json(")) {
                    String jsonPath = path.substring(5, path.length() - 1);
                    value = JsonPath.read(page, jsonPath);
                } else {
                    TagNode tn = pageAnalyzer.toTagNode((String) page);
                    value = pageAnalyzer.getText(tn, path);
                }
                if (StringUtils.isNoneBlank(script)) {
                    value = template.expressCalcu(script, value, null);
                }
                return value;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
    }    
    
    public Object xpathPropertyObj(TagNode tn, Object propertyParam) throws Exception {
            String value = null;
            try {
                String path = null;
                String script = null;
                if (propertyParam instanceof Map) {
                    path = (String) ((Map) propertyParam).get("xpath");
                    script = beeGather.getScript((Map) propertyParam);
                } else {
                    path = propertyParam.toString();
                }
                
                value = pageAnalyzer.getText(tn, path);
                if (StringUtils.isNoneBlank(script)) {
                    value = template.expressCalcu(script, value, null);
                }
                return value;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
    }      
    
    public Map xpathObj(TagNode tn, Map<String, Object> properMap) throws Exception {
        Map map = new HashMap();
        for (String key : properMap.keySet()) {
            try {
                String path = null;
                String script = null;
                Object opath = properMap.get(key);
                if (opath instanceof Map) {
                    path = (String) ((Map) opath).get("xpath");
                    script = beeGather.getScript((Map) opath);
                } else {
                    path = opath.toString();
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
        return map;
    }

    private String parseFile2Text(String fileName) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        InputStream stream = new FileInputStream(new File(fileName));
        parser.parse(stream, handler, metadata);
        return handler.toString();
    }

    private String readTextFile(String fileName, String encoding) throws IOException {
        return FileUtils.readFileToString(new File(fileName), encoding);
    }

}

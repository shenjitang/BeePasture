/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.StringUtils;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.http.HttpService;
import org.shenjitang.beepasture.http.HttpTools;
import org.shenjitang.beepasture.http.OkHttpTools;


/**
 *
 * @author xiaolie
 */
public class HttpResource extends BeeResource implements Runnable {
    protected static final Log LOGGER = LogFactory.getLog(HttpResource.class);
    //private ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    //protected HttpService httpTools;
    protected long count;
    protected long RENEWHTTPTOOLS_COUNT = 100L;
    protected Thread httpThread;
    protected Map loadParam;
    protected volatile boolean startRun = false; 
    protected Object result;
    protected long timeout = 60000L;
    protected static long threadCount = 0;
    protected boolean ssl = false;
    
    public HttpResource() {
        //httpTools = new OkHttpTools(false);
    }
    
    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
    }
    
    @Override
    protected void _persist(GatherStep gatherStep, String varName, Object obj, Map params) {
        System.out.println(varName);
        String url = (String)params.get("to");
//        String method = (String)params.get("method");
        Map heads = (Map)params.get("head");
        try {
//            if ("post".equalsIgnoreCase(method)) {
                HttpService httpTools = new HttpTools(ssl);
                String page = httpTools.doPost((String) url, obj.toString(), heads, null);
//            } else {               
//            }
        } catch (Exception e) {
            LOGGER.warn(url, e);
        }
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        this.loadParam = loadParam;
        startThread();
        return waiteForResult(timeout);
        
    }

    private Object loadResource() throws Exception {
        ++count;
        HttpService httpTools = new HttpTools(ssl);//OkHttpTools(ssl);
//        if (++count % RENEWHTTPTOOLS_COUNT == 0L) {
//            LOGGER.info("renew httptool count=" + count);
//            httpTools = new HttpTools();
//        }
        Map withVarCurrent = (Map)loadParam.get("withVarCurrent");
        String charset = (String)loadParam.get("charset");
        Map heads = (Map) loadParam.get("head");
        if (heads == null) {
            heads = (Map) loadParam.get("heads");
        }
        Map download = (Map) loadParam.get("download");
        if (download != null) {
            String fileName = getFileName(withVarCurrent, loadParam, download);
            httpTools.downloadFile(url.toString(), fileName);
            String filenameToVar = (String) download.get("filename");
            if (StringUtils.isNotBlank(filenameToVar)) { 
                if (withVarCurrent != null) {
                    withVarCurrent.put(filenameToVar, fileName);
                } else {
                    BeeGather.getInstance().getVar(filenameToVar).add(fileName);
                }
            }
            String fileUrl = "file://" + fileName;
            FileResource fileResource = new FileResource();
            fileResource.init(fileUrl, null);
            Map saveMap = (Map)loadParam.get("save");
            if (saveMap != null) {
                return fileResource.loadResource(null, saveMap);
            }
            return fileUrl;
        }
        if (loadParam.containsKey("dataimage")) {
            return httpTools.dataImage(url);
        }
        String page = null;
        String postBody = (String) loadParam.get("post");
        if (org.apache.commons.lang3.StringUtils.isNotBlank(postBody)) {
            LOGGER.info("=> POST " + url);
            page = httpTools.doPost((String) url, postBody, heads, null);
            LOGGER.debug("POST " + url + "\n" + page);
        } else {
            LOGGER.info("=> GET " + url);
            page = httpTools.doGet((String) url, heads, charset);
            LOGGER.debug("GET " + url + " finish  " + (page == null ? "null" : (page.length() > 50 ? page.substring(0, 40) + "   ......" : page)));
        }
        return page;
    }
    
    protected String getFileName(Map withVarCurrent, Map loadParam, Map download) {
        String fileName = null;
        String to = (String)download.get("to");
        if (withVarCurrent != null) {
            fileName = (String)withVarCurrent.get(to);
        }
        if (StringUtils.isBlank(fileName)) {
            fileName = to;
        }
        //fileName = template.expressCalcu(fileName, url, null);
        return fileName;
    }

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run() {
            try {
                result = loadResource();
            } catch (Throwable th) {
                LOGGER.warn("", th);
            } finally {
                startRun = false;
                synchronized(this) {
                    this.notify();
                }
            }
    }

    private Object waiteForResult(Long timeout) {
        long beginTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - beginTime < timeout) {
            if (startRun) {
                synchronized (this) {
                    try {
                        this.wait(1000);
                    } catch (InterruptedException e) {
                        LOGGER.info("waiteForResult interrupted! " + e.toString());
                    }
                }
            } else {
                return result;
            }
        }
        LOGGER.warn("http get timeout!!!, interrupt thread: " + httpThread.getName());
        try {
            httpThread.interrupt();
        } catch (Exception e) {
            LOGGER.warn("httpThread.interrupt", e);
        }
        try {    Thread.sleep(1000L);} catch (InterruptedException i){}
        //startThread();
        //startRun = false;
        //httpTools = new HttpTools();
        return null;
    }

    private void startThread() {
        startRun = true;
        httpThread = new Thread(this, "httpResource-thread-" + threadCount++);
        httpThread.setDaemon(true);
        httpThread.start();
    }
}

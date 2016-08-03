/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.StringUtils;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.http.HttpTools;


/**
 *
 * @author xiaolie
 */
public class HttpResource extends BeeResource {
    private static final Log LOGGER = LogFactory.getLog(HttpResource.class);
    //private ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    private final HttpTools httpTools;
    
    public HttpResource() {
        httpTools = new HttpTools();
    }
    
    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
    }
    
    @Override
    public void persist(String varName, Object obj, Map params) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        Object withVarCurrent = loadParam.get("withVarCurrent");
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
                if (withVarCurrent != null && withVarCurrent instanceof Map) {
                    ((Map) withVarCurrent).put(filenameToVar, fileName);
                } else {
                    BeeGather.getInstance().getVar(filenameToVar).add(fileName);
                }
            }
            String fileUrl = "file://" + fileName;
            FileResource fileResource = new FileResource();
            fileResource.init(fileUrl, null);
            Map saveMap = (Map)loadParam.get("save");
            if (saveMap != null) {
                return fileResource.loadResource(saveMap);
            }
            return fileUrl;
        } else {
            String page = null;
            String postBody = (String) loadParam.get("post");
            if (org.apache.commons.lang3.StringUtils.isNotBlank(postBody)) {
                page = httpTools.doPost((String) url, postBody, heads);
                LOGGER.info("POST " + url + "\n" + page);
            } else {
                page = httpTools.doGet((String) url, heads, charset);
                LOGGER.info("GET " + url + "\n" + page);
            }
            return page;
        }

    }
    
    protected String getFileName(Object withVarCurrent, Map loadParam, Map download) {
        String fileName = null;
        String to = (String)download.get("to");
        if (withVarCurrent != null && withVarCurrent instanceof Map) {
            fileName = (String)((Map)withVarCurrent).get(to);
        }
        if (StringUtils.isBlank(fileName)) {
            fileName = to;
        }
        //fileName = template.expressCalcu(fileName, url, null);
        return fileName;
    }
    
}

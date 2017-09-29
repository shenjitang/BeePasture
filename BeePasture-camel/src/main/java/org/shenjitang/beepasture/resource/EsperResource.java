/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.epl.EsperProcess;

/**
 *
 * @author xiaolie
 */
public class EsperResource extends BeeResource {
    private static final Log LOGGER = LogFactory.getLog(EsperResource.class);
    
    private Map esperDefineMap;
    private EsperProcess esperProcess;
    //private CamelContextResource camelContextResource;

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        
        //camelContextResource = BeeGather.getInstance().getResourceMng().getResource(name);
        String part = uri.getSchemeSpecificPart();
        LOGGER.debug("******part: " + part);
        if (StringUtils.isNotBlank(part) && (!"null".equalsIgnoreCase(part))) {
            String fileEncoding = System.getProperty("fileEncoding", "utf8");
            String yamlContent = FileUtils.readFileToString(new File(part), fileEncoding);
            esperDefineMap = (Map)Yaml.load(yamlContent);
        } else {
            esperDefineMap = param;
        }
        esperProcess = new EsperProcess();
        esperProcess.start(esperDefineMap);
    }
    
    @Override
    protected void _persist(GatherStep gatherStep, String varName, Object obj, Map params) {
        try {
            esperProcess.streamIn((Map)obj, params);
        } catch (Exception e) {
            LOGGER.warn("persist :" + obj, e);
        }
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

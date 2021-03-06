/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.resource.BeeResource;
import org.shenjitang.beepasture.resource.ResourceMng;
import org.shenjitang.beepasture.util.ParseUtils;

/**
 *
 * @author xiaolie
 */
public class BeeGather {
    public static Log MAIN_LOGGER = LogFactory.getLog("org.shenjitang.beepasture.core.Main");
    protected final ScriptTemplateExecuter template = new ScriptTemplateExecuter();
    protected Map program;
    protected Map vars;
    protected List gatherStepList;
    protected Map persistStep; 
    protected Map resources;
    protected List resourcesList;
    protected String gather;
    protected final ResourceMng resourceMng = new ResourceMng();
    protected static final Log LOGGER = LogFactory.getLog(BeeGather.class);
    protected static BeeGather instance;
//    protected PageAnalyzer pageAnalyzer = new PageAnalyzer();

    public Map getProgram() {
        return program;
    }
    

    public String getGather() {
        return gather;
    }
    
    public static BeeGather getInstance() {
        return instance;
    }

    public BeeGather(String yamlString) {
        this.gather = yamlString;
        program = (Map)Yaml.load(gather);
        Map sysProperties = (Map)program.get("properties");
        if (sysProperties != null) {
            sysProperties.forEach((k, v) -> {
                System.setProperty((String)k, (String)v);
            });
        }
        instance = this;
        LOGGER.debug(program);
    }

     public void init() throws Exception {
        loadResources();
        if (resources != null) {
            if (resourcesList == null) {
                resourceMng.init(resources);
            } else {
                resourceMng.init(resourcesList);
            }
        } else {
            resources = new HashMap();
        }
        vars = (Map)program.get("var");
        if (vars == null) {
            vars = new HashMap();
            program.put("var", vars);
        }
        initVars(vars, resources);
        gatherStepList = (List)program.get("gather");
        persistStep = (Map)program.get("persist");  
    }
     
    public boolean containsResource(String name) {
        return resources.containsKey(name);
    }
    
    public Map doGather() throws Exception {
        if (gatherStepList == null) {
            return vars;
        }
        int i = 0;
        for (Object stepObj : gatherStepList) {
            Map step = (Map) stepObj;
            MAIN_LOGGER.info("enter " + step.get("url"));
            GatherStep gatherStep = new GatherStep(step, i++);
            gatherStep.execute();
            MAIN_LOGGER.info("level " + step.get("url"));
//            gatherStep.sleep();
        }
        return vars;
    }
    
    public List getVar(String varName) {
        List var = (List) vars.get(varName);
        if (var == null) {
            var = new ArrayList();
            vars.put(varName, var);
        }
        return var;
    }
    
    protected String getScript(Map step) {
        String templete = (String) step.get("templete");
        if (StringUtils.isBlank(templete)) {
            templete = (String) step.get("template");
        }
        if (StringUtils.isBlank(templete)) {
            templete = (String) step.get("script");
        }
        return templete;
    }
    
    protected void saveTo(Map persistMap) throws Exception {
        if (persistMap != null) {
            for (Object key : persistMap.keySet()) {
                try {
                    String varName = (String) key;
                    Object objPersist = persistMap.get(key);
                    saveVar(varName, objPersist);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    } 
    
    public void saveVar(String varName, Object objPersist) throws Exception {
        String topVarName = varName.split("[.]")[0];
        if (objPersist instanceof String) {
            String resourceStr = template.expressCalcu((String) objPersist, new HashMap());
            if (resourceStr.startsWith("file:\\\\")) {
                resourceStr = "file://" + resourceStr.substring(7);
            }
            if (resourceStr.contains(":")) {
                Map params = new HashMap();
                params.put("url", resourceStr);
                persist(resourceStr, params, varName, vars.get(topVarName));
            } else if (resources.containsKey(resourceStr)) {
                Map resource = (Map) resources.get(resourceStr);
                persist(resourceStr, resource, varName, vars.get(topVarName));
            } else {
                throw new RuntimeException("不支持的persist cmd: " + resourceStr);
            }
        } else if (objPersist instanceof Map) {
            Map resourceMap = (Map) objPersist;
            String resourceName = (String) resourceMap.get("resource");
            Map resource = (Map) resources.get(resourceName);
            Map mergeReourceMap = new HashMap();
            mergeReourceMap.putAll(resource);
            mergeReourceMap.putAll(resourceMap);
            persist(resourceName, mergeReourceMap, varName, vars.get(topVarName));
        } else if (objPersist instanceof List) {
            for (Object saveStep : (List)objPersist) {
                saveVar(varName, saveStep);
            }
        } else {
            throw new RuntimeException("不支持的persist: " + objPersist.toString());
        }
    }
    
    
    public void persist(String resourceName, Map params, String varName, Object obj) throws Exception {
        BeeResource beeResource = resourceMng.getResource(resourceName);
        beeResource.persist(null, varName, obj, params);
    }
    
    public void saveTo() throws Exception {
        saveTo(persistStep);
    }

    public ResourceMng getResourceMng() {
        return resourceMng;
    }
    
    protected void loadResources() {
        Object res = program.get("resource");
        if (res instanceof List) {
            resourcesList = (List)res;
            resources = new HashMap();
            for (Object one : resourcesList) {
                resources.put(((Map)one).get("name"), one);
            }
        } else if(res instanceof Map) { 
            resources = (Map)res;
        }
    }
 
    public Map getVars() {
        return vars;
    }
    
    protected Map initVars(Map vars, Map resources) throws Exception{
        return ParseUtils.replaceByArray(vars);
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.core.GatherStep;

/**
 *
 * @author xiaolie
 */
public abstract class BeeResource {
    protected final Log LOGGER = LogFactory.getLog(this.getClass());
    protected BeeResource saveToResource;
    protected Map saveDefMap;
    protected String flowOutEndpoint = null;
    protected String name;
    protected String url;
    protected URI uri;
    protected Map params = new HashMap();
    protected Map uriParams = new HashMap();
    
    public BeeResource() {
    }

    public void init(String url, Map param) throws Exception {
        if (url != null) {
            this.url = url;
            try {
                this.uri = URI.create(url);
                
                String query  = uri.getQuery();
                if (org.codehaus.plexus.util.StringUtils.isNotBlank(query)) {
                    List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
                    if (queryPair != null) {
                        for (NameValuePair nvp : queryPair) {
                            params.put(nvp.getName(), nvp.getValue());
                            uriParams.put(nvp.getName(), nvp.getValue());
                        }
                    }
                }
                
            } catch (IllegalArgumentException e) {
                LOGGER.warn("", e);
            }
        }
        if (param != null) {
            this.params.putAll(param);
        }
        saveDefMap = (Map)params.get("save");
        if (saveDefMap != null) {
            String to = (String)saveDefMap.get("to");
            saveToResource = BeeGather.getInstance().getResourceMng().getResource(to);
        }
    }
    
    abstract public void persist(GatherStep gatherStep, String varName, Object obj, Map params);
    abstract public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception;
    abstract public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception;
    
    public void afterIterate() {
        
    }

    public Map getParams() {
        return params;
    }

    public void setParams(Map params) {
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlowOutEndpoint() {
        return flowOutEndpoint;
    }

    public void setFlowOutEndpoint(String flowOutEndpoint) {
        this.flowOutEndpoint = flowOutEndpoint;
        if (StringUtils.isNotBlank(flowOutEndpoint)) {
            if (saveDefMap == null) {
                saveDefMap = new HashMap();
            }
            if (StringUtils.isBlank((String)saveDefMap.get("endpoint"))) {
                saveDefMap.put("endpoint", flowOutEndpoint);
            }
            saveToResource = BeeGather.getInstance().getResourceMng().getResource((String)saveDefMap.get("to"));
        }
    }
    
    protected void flowOut(GatherStep gatherStep, Object result, List<String> headList) {
        if (saveToResource != null) {
            saveDefMap.put("header", headList);
            saveToResource.persist(gatherStep, null, result, saveDefMap);
        }
    }
    
    public static List getList(Map map, Object key) {
        Object value = map.get(key);
        if (value == null) {
            return new ArrayList();
        } else if (value instanceof List) {
            return (List)value;
        } else {
            List list = new ArrayList();
            list.add(value);
            return list;
        }
    }

    protected String getParam(Map localParam, String paramName, String def) {
        String value = (String)localParam.get(paramName);
        if (StringUtils.isBlank(value)) {
            value = (String)params.get(paramName);
            if (StringUtils.isBlank(value)) {
                value = def;
            }
        }
        return value;
    }
    
    protected Object getValue(String varName,  Object obj) {
        if (StringUtils.isNotBlank(varName)) {
            String topVarName = varName.split("[.]")[0];
            String tailVarName = null;
            if (topVarName.length() < varName.length()) {
                tailVarName = varName.substring(topVarName.length() + 1);
                if (obj instanceof List) {
                    if (((List)obj).size() == 1) {
                        obj = ((List)obj).get(0);
                    }
                }
                if (obj instanceof Map) {
                    obj = ((Map)obj).get(tailVarName);
                } else {
                    try {
                        obj = PropertyUtils.getProperty(obj, tailVarName);
                    } catch (Exception e) {
                        LOGGER.warn("persist:" + varName, e);
                    }
                }
            }
        }    
        return obj;
    }


    public String getUrl() {
        return url;
    }

    public URI getUri() {
        return uri;
    }
}

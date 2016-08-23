/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shenjitang.beepasture.core.BeeGather;

/**
 *
 * @author xiaolie
 */
public abstract class BeeResource {
    protected final Log LOGGER = LogFactory.getLog(this.getClass());
    public BeeResource() {
    }
    
    protected String name;
    protected String url;
    protected URI uri;
    protected Map params = new HashMap();
    public void init(String url, Map param) throws Exception {
        this.url = url;
        this.uri = URI.create(url);
        if (param != null) {
            this.params.putAll(param);
        }
    }
    abstract public void persist(String varName, Object obj, Map params);
    abstract public Object loadResource(Map loadParam) throws Exception;

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

}

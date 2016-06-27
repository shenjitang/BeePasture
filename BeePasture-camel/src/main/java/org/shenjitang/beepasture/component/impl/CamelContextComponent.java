/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.component.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.shenjitang.beepasture.component.Component;
import org.shenjitang.beepasture.resource.CamelContextResource;

/**
 *
 * @author xiaolie
 */
public class CamelContextComponent implements Component {
    private CamelContextResource resource;
    private Map param;

    public CamelContextComponent() {
    }
    

    @Override
    public void persist(URI uri, String varName, Object obj) throws Exception {
        String to = (String)param.get("to");
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                try {
                    resource.getCamelProducer().requestBody(to, item);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setResource(Object resource) {
        this.resource = (CamelContextResource)resource;
    }

    @Override
    public void setParams(Map params) {
        this.param = params;
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

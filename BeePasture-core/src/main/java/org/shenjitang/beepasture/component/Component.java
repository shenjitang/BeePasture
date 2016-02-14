/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.component;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 *
 * @author xiaolie
 */
public interface Component {
    public void persist(URI uri, String varName, Object obj) throws Exception;
    public void setResource(Object resource);
    public void setParams(Map params);
    public Object loadResource(Map loadParam) throws Exception;
}

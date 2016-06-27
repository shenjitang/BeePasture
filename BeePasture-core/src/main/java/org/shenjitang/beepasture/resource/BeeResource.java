/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.util.Map;

/**
 *
 * @author xiaolie
 */
public interface BeeResource {
    public void init(URI uri, Map param) throws Exception;
}

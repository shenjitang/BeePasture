/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author xiaolie
 */
public class HttpServiceMng {
    static final Map<String ,HttpService> httpServiceMap = new HashMap();
    public static HttpService get(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        Boolean ssl = "https".equalsIgnoreCase(scheme);
        String key = scheme + ":" + host;
        if (httpServiceMap.containsKey(key)) {
            return httpServiceMap.get(key);
        } else {
            HttpService service = new OkHttpTools(ssl);
            httpServiceMap.put(key, service);
            return service;
        }
    } 
}

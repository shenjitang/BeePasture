/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author xiaolie
 */
public class HttpServiceMng {
    protected static final Log LOGGER = LogFactory.getLog(HttpServiceMng.class);
    static final Map<String ,HttpService> httpServiceMap = new HashMap();
    public static HttpService get(URI uri) {
        String httpToolsClassName = System.getProperty("HttpTools", "org.shenjitang.beepasture.http.OkHttpTools");
        if (!httpToolsClassName.contains(".")) {
            httpToolsClassName = "org.shenjitang.beepasture.http." + httpToolsClassName;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        Boolean ssl = "https".equalsIgnoreCase(scheme);
        String key = scheme + ":" + host;
        if (httpServiceMap.containsKey(key)) {
            return httpServiceMap.get(key);
        } else {
            try {
                Class clazz = Class.forName(httpToolsClassName);
                Constructor constructor = clazz.getConstructor(Boolean.class);
                HttpService service = (HttpService)constructor.newInstance(ssl);
                LOGGER.info("Load HttpService => " + httpToolsClassName);
                httpServiceMap.put(key, service);
                return service;
            } catch (Exception ex) {
                LOGGER.error(httpToolsClassName, ex);
                System.exit(-440);
            }
        }
        return null;
    } 
}

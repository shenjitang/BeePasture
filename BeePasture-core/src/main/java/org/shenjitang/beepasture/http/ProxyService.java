/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.ho.yaml.Yaml;

/**
 *
 * @author xiaolie
 */
public class ProxyService {
    private static ProxyService instance;
    
    private List httpProxyList = new ArrayList();
    private List httpsProxyList = new ArrayList();
    private String configFilename = "proxys.yaml";
    private Random random = new Random();

    private ProxyService() {
        loadProxyList();
    }
    
    public static ProxyService getInstance() {
        if (instance == null) {
            instance = new ProxyService();
        }
        return instance;
    }
    
    public void loadProxyList() {
        File file = new File(configFilename);
        if (file.exists()) {
            try {
                List proxyList = (List)Yaml.load(file);
                for (Object proxy : proxyList) {
                    Map map = (Map)proxy;
                    if ("HTTPS".equalsIgnoreCase((String)map.get("protocol"))) {
                        httpsProxyList.add(proxy);
                        httpProxyList.add(proxy);
                    } else if ("HTTP".equalsIgnoreCase((String)map.get("protocol"))) {
                        httpProxyList.add(proxy);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public Map getProxy(boolean ssl) {
        return ssl?getHttpsProxy():getHttpProxy();
    }
    
    public Map getHttpProxy() {
        if (httpProxyList.isEmpty()) {
            return null;
        }
        int size = httpProxyList.size();
        int n = random.nextInt(size);
        return (Map)httpProxyList.get(n);
    }
    
    public Map getHttpsProxy() {
        if (httpsProxyList.isEmpty()) {
            return null;
        }
        int size = httpsProxyList.size();
        int n = random.nextInt(size);
        return (Map)httpsProxyList.get(n);
    }
    
}

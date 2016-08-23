/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;


/**
 *
 * @author xiaolie
 */
public class ResourceMng {
    private Map resourceDefine;
    private final Map<String, BeeResource> resourceMap = new HashMap();
    

    public ResourceMng() {
    }
    
    public BeeResource getResource(String name) {
        try {
            BeeResource beeResurce = resourceMap.get(name);
            if (beeResurce == null) {
                if (name.contains(":")) {
                    Map params = new HashMap();
                    params.put("url", name);
                    beeResurce = initOneResource(name, params);
                } else {
                    //throw new RuntimeException("不认识的资源或url: " + name);
                    return null;
                }
            }
            return beeResurce;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
//    public String getResourceScheme(String resourceName) {
//        String url = (String)((Map)resourceDefine.get(resourceName)).get("url");
//        URI uri = URI.create(url);
//        return uri.getScheme();
//    }
    
    public Map getResourceParam(String resourceName) {
        return (Map)resourceDefine.get(resourceName);
    }
    
    public void initCamelResource(String name, Map params) {
        String url = (String) params.get("url");
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if ("camelContext".equalsIgnoreCase(scheme)) {
            initOneResource(name, params);
        }
    }
    
    public void initOtherResource(String name, Map params) {
        String url = (String) params.get("url");
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"camelContext".equalsIgnoreCase(scheme)) {
            initOneResource(name, params);
        }
    }

    public BeeResource initOneResource(String name, Map params) {
        String url = (String) params.get("url");
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        try {
            String resourceClassname = "org.shenjitang.beepasture.resource." + StringUtils.capitalize(scheme) + "Resource";
            BeeResource resource = (BeeResource) Class.forName(resourceClassname).newInstance();
            resource.setName(name);
            resource.init(url, params);
            resourceMap.put(name, resource);
            return resource;
        } catch (Exception e) {
            throw new RuntimeException("Don't know resource :" + url, e);
        }
    }

    public void init(Map resources) {
        this.resourceDefine = resources;
        try {
            // init camel resource
            for (Object key : resources.keySet() ) {
                initCamelResource((String)key, (Map)resources.get(key));
            }
            // init other resource
            for (Object key : resources.keySet() ) {
                initOtherResource((String)key, (Map)resources.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    


//    public Object loadResource(Map resource) throws Exception {
//        Map params = new HashMap();
//        String urlStr = (String)resource.get("url");
//        URI uri = URI.create(urlStr);
//        
//        String fileName = uri.getAuthority() + uri.getPath();
//        String query  = uri.getQuery();
//        List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
//        if (queryPair != null) {
//            for (NameValuePair nvp : queryPair) {
//                params.put(nvp.getName(), nvp.getValue());
//            }
//        }
//
//        String encoding = ResourceUtils.get(params, "encoding", "GBK");
//        String format = ResourceUtils.get(params, "format", "yaml");
//        if ("plantext".equalsIgnoreCase(format) || "planttext".equalsIgnoreCase(format)) {
//            return FileUtils.readFileToString(new File(fileName), encoding);
//        } else if ("line".equalsIgnoreCase(format)) {
//            return FileUtils.readLines(new File(fileName), encoding);
//        } else { //default = yaml
//            return Yaml.load(FileUtils.readFileToString(new File(fileName), encoding));
//        }
//    }
}

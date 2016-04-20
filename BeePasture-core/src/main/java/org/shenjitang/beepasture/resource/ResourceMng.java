/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.elasticsearch.client.Client;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.component.Component;
import org.shenjitang.beepasture.component.ComponentUtils;


/**
 *
 * @author xiaolie
 */
public class ResourceMng {
    private Map resourceDefine;
    private Map<String, Object> resourceMap = new HashMap();
    public static String COMPONENT_PACKAGE = "org.shenjitang.beepasture.component.impl";

    public ResourceMng() {
    }
    
    public Object getResource(String name) {
        return resourceMap.get(name);
    }
    
    public String getResourceScheme(String resourceName) {
        String url = (String)((Map)resourceDefine.get(resourceName)).get("url");
        URI uri = URI.create(url);
        return uri.getScheme();
    }
    
    public Map getResourceParam(String resourceName) {
        return (Map)resourceDefine.get(resourceName);
    }

    public void init(Map resources) {
        this.resourceDefine = resources;
        try {
            for (Object key : resources.keySet() ) {
                String name = (String)key;
                Map params = (Map)resources.get(key);
                String url = (String)params.get("url");
                URI uri = URI.create(url);
                String scheme = uri.getScheme();
                if (scheme.equalsIgnoreCase("jdbc")) {
                    Properties props = new Properties();
                    props.put("driverClassName", getDriverClassName(url));
                    for (Object k : params.keySet()) {
                        props.put(k, params.get(k));
                    }

                    DataSource ds = BasicDataSourceFactory.createDataSource(props);
                    resourceMap.put(name, ds);
                } else if (scheme.equalsIgnoreCase("file")) {
                    String fileName = uri.getAuthority() + uri.getPath();
                    File file = new File(fileName);
                    resourceMap.put(name, file);
                } else if (scheme.equalsIgnoreCase("mongodb")) {
                    MongoDbResource monodbResource = new MongoDbResource(url);
                    resourceMap.put(name, monodbResource);
                } else if (scheme.equalsIgnoreCase("elasticsearch")) {
                    ElasticsearchResource resource = new ElasticsearchResource(uri);
                    resourceMap.put(name, resource);
                } else {
//                    String resourceClassname = "org.shenjitang.beepasture.resource." + StringUtils.capitalize(scheme) + "Resource";
//                    Object resource = Class.forName(resourceClassname).newInstance();
                    throw new RuntimeException("Don't know resource :" + url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

    public String getDriverClassName(String url) {
        if (url.contains("jdbc:jtds:sqlserver")) {
            return "net.sourceforge.jtds.jdbc.Driver";
        } else if (url.contains("jdbc:sqlserver")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        } else if (url.contains("jdbc:oracle:thin")) {
            return "oracle.jdbc.OracleDriver";
        } else if (url.contains("jdbc:mysql:")) {
            return "com.mysql.jdbc.Driver";
        } else {
            return null;
        }
    }
    
    public static Component createComponent(String scheme, Object resource, Map params) throws Exception {
        Component component = (Component)Class.forName(COMPONENT_PACKAGE + "." + StringUtils.capitalize(scheme) + "Component").newInstance();
        component.setResource(resource);
        component.setParams(params);
        return component;
    }

    public Object loadResource(Map resource) throws Exception {
        Map params = new HashMap();
        String urlStr = (String)resource.get("url");
        URI uri = URI.create(urlStr);
        
        String fileName = uri.getAuthority() + uri.getPath();
        String query  = uri.getQuery();
        List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
        if (queryPair != null) {
            for (NameValuePair nvp : queryPair) {
                params.put(nvp.getName(), nvp.getValue());
            }
        }

        String encoding = ComponentUtils.get(params, "encoding", "GBK");
        String format = ComponentUtils.get(params, "format", "yaml");
        if ("plantext".equalsIgnoreCase(format) || "planttext".equalsIgnoreCase(format)) {
            return FileUtils.readFileToString(new File(fileName), encoding);
        } else if ("line".equalsIgnoreCase(format)) {
            return FileUtils.readLines(new File(fileName), encoding);
        } else { //default = yaml
            return Yaml.load(FileUtils.readFileToString(new File(fileName), encoding));
        }
    }
}

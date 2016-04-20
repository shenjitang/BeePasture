/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.component.impl;

import com.alibaba.fastjson.JSON;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.component.Component;
import org.shenjitang.beepasture.component.ComponentUtils;
import org.shenjitang.commons.csv.CSVUtils;

/**
 *
 * @author xiaolie
 */
public class FileComponent implements Component {
    private Map params;
    private File file;

    public FileComponent() {
    }

    @Override
    public void persist(URI uri, String varName, Object obj) throws Exception {
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
                obj = PropertyUtils.getProperty(obj, tailVarName);
            }
        }

        String fileName = uri.getAuthority() + uri.getPath();
        String query  = uri.getQuery();
        List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
        if (queryPair != null) {
            for (NameValuePair nvp : queryPair) {
                params.put(nvp.getName(), nvp.getValue());
            }
        }
        try {
            String encoding = ComponentUtils.get(params, "encoding", "GBK");
            String format = ComponentUtils.get(params, "format", "plant");
            if ("yaml".equalsIgnoreCase(format)) {
                String content = Yaml.dump(obj);
                FileUtils.write(new File(fileName), content, encoding);
            } else if ("json".equalsIgnoreCase(format)) {
                StringBuilder sb = new StringBuilder();
                String jsonStr = JSON.toJSONString(obj);
                FileUtils.write(new File(fileName), jsonStr, encoding);
            } else if ("csv".equalsIgnoreCase(format)) {
                if (obj instanceof List) {
                    CSVUtils csvUtils = new CSVUtils();
                    String csvStr = csvUtils.getCSVWithHeads((List)obj);
                    FileUtils.write(new File(fileName), csvStr, encoding);
                } else {
                    throw new RuntimeException("Object:" + obj.getClass().getName() + " can not trans to csv");
                }
            } else { //default = plant
                if (obj instanceof List) {
                    StringBuilder sb = new StringBuilder();
                    for (Object o : (List) obj) {
                        sb.append(o.toString()).append("\n");
                    }
                    FileUtils.write(new File(fileName), sb, encoding);
                } else {
                    FileUtils.write(new File(fileName), obj.toString(), encoding);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setResource(Object resource) {
        file = (File)resource;
    }

    @Override
    public void setParams(Map params) {
        this.params = params;
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        Map iparams = new HashMap();
        String urlStr = (String)params.get("url");
        URI uri = URI.create(urlStr);
        
        String fileName = uri.getAuthority() + uri.getPath();
        String query  = uri.getQuery();
        List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
        if (queryPair != null) {
            for (NameValuePair nvp : queryPair) {
                iparams.put(nvp.getName(), nvp.getValue());
            }
        }

        String encoding = ComponentUtils.get(iparams, "encoding", "GBK");
        String format = ComponentUtils.get(iparams, "format", "plant");
        if ("yaml".equalsIgnoreCase(format)) {
            return Yaml.load(FileUtils.readFileToString(file, encoding));
        } else if ("line".equalsIgnoreCase(format)) {
            return FileUtils.readLines(file, encoding);
        } else if ("json".equalsIgnoreCase(format)) {
            String str = FileUtils.readFileToString(file, encoding);
            return JSON.parse(str);
        } else { //default = plant
            return FileUtils.readFileToString(file, encoding);
        }
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import org.shenjitang.beepasture.resource.util.ResourceUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codehaus.plexus.util.StringUtils;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.resource.util.ExcelParser;
import org.shenjitang.commons.csv.CSVUtils;

/**
 *
 * @author xiaolie
 */
public class FileResource extends BeeResource {
    protected File file;
    static SerializeConfig mapping = new SerializeConfig();

    public FileResource() {
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
        String fileName = uri.getAuthority() + uri.getPath();
        file = new File(fileName);
        String query  = uri.getQuery();
        if (StringUtils.isNotBlank(query)) {
            List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
            if (queryPair != null) {
                for (NameValuePair nvp : queryPair) {
                    params.put(nvp.getName(), nvp.getValue());
                }
            }
        }
    }

    @Override
    public void persist(String varName, Object obj, Map persistParams) {
        Map allParam = new HashMap();
        allParam.putAll(this.params);
        allParam.putAll(persistParams);
        if (StringUtils.isNotBlank(varName)) {
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
                    try {
                        obj = PropertyUtils.getProperty(obj, tailVarName);
                    } catch (Exception e) {
                        LOGGER.warn("persist:" + varName, e);
                    }
                }
            }
        }
        try {
            String encoding = ResourceUtils.get(allParam, "encoding", "GBK");
            String format = ResourceUtils.get(allParam, "format", "plant");
            String dataFormat = ResourceUtils.get(allParam, "dataFormat", "yyyy-MM-dd HH:mm:ss");
            LOGGER.info("save var:" + varName + " to file:" + file.getAbsolutePath());
            if ("yaml".equalsIgnoreCase(format)) {
                String content = Yaml.dump(obj);
                FileUtils.write(file, content, encoding);
            } else if ("json".equalsIgnoreCase(format)) {          
                mapping.put(Date.class, new SimpleDateFormatSerializer(dataFormat));
                StringBuilder sb = new StringBuilder();
                String jsonStr = JSON.toJSONString(obj, mapping);
                FileUtils.write(file, jsonStr, encoding);
            } else if ("csv".equalsIgnoreCase(format)) {
                if (obj instanceof List) {
                    CSVUtils csvUtils = new CSVUtils();
                    String csvStr = csvUtils.getCSVWithHeads((List)obj);
                    FileUtils.write(file, csvStr, encoding);
                } else {
                    throw new RuntimeException("Object:" + obj.getClass().getName() + " can not trans to csv");
                }
            } else { //default = plant
                if (obj instanceof List) {
                    StringBuilder sb = new StringBuilder();
                    for (Object o : (List) obj) {
                        sb.append(o.toString()).append("\n");
                    }
                    FileUtils.write(file, sb, encoding);
                } else {
                    FileUtils.write(file, obj.toString(), encoding);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("file:" + file.getAbsolutePath(), e);
        }
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        Map iparams = new HashMap();
        
        String query  = uri.getQuery();
        if (query != null) {
            List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
            if (queryPair != null) {
                for (NameValuePair nvp : queryPair) {
                    iparams.put(nvp.getName(), nvp.getValue());
                }
            }
        }

        String encoding = ResourceUtils.get(iparams, "encoding", "GBK");
        String format = ResourceUtils.get(iparams, "format", "plant");
        if ("yaml".equalsIgnoreCase(format)) {
            return Yaml.load(FileUtils.readFileToString(file, encoding));
        } else if ("line".equalsIgnoreCase(format)) {
            return FileUtils.readLines(file, encoding);
        } else if ("json".equalsIgnoreCase(format)) {
            String str = FileUtils.readFileToString(file, encoding);
            return JSON.parse(str);
        } else if ("excel".equalsIgnoreCase(format)) {
            return ExcelParser.parseExcel(file, null);
        } else { //default = plant
            return FileUtils.readFileToString(file, encoding);
        }    
    }
    
}

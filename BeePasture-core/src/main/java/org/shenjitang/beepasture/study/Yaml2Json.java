/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.study;

import java.io.File;
import java.util.Map;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FileUtils;
import org.ho.yaml.Yaml;

/**
 *
 * @author xiaolie
 */
public class Yaml2Json {
    public static void main(String[] args) throws Exception {
        String fileEncoding = "UTF8";//System.getProperty("fileEncoding", "GBK");
        String fileName = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\mongodb_city.yaml";
        if (args.length > 0) {
            fileName = args[0];
        }
        File file = new File(fileName);
        String yaml = FileUtils.readFileToString(file, fileEncoding);

        Yaml2Json yj = new Yaml2Json();
        String json = yj.yaml2json(yaml);
        System.out.println(json);
    }
    
    public String yaml2json(String yaml) {
        Map route = (Map)Yaml.load(yaml);
        return JSONValue.toJSONString(route);
    }
    
}

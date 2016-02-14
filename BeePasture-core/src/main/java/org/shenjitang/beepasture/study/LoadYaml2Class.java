/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.study;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FileUtils;
import org.ho.yaml.Yaml;

/**
 *
 * @author xiaolie
 */
public class LoadYaml2Class {
    
    public static void printit() {
        Map<String, City> map = new HashMap();
        City sh = new City();
        sh.setName("shanghai");
        sh.setTitle("上海");
        map.put(sh.getName(), sh);
        sh = new City();
        sh.setName("beijing");
        sh.setTitle("北京");
        map.put(sh.getName(), sh);
        System.out.println(Yaml.dump(map));
    }
    
    public static void main(String[] args) throws Exception {
        printit();
        String fileEncoding = "UTF8";//System.getProperty("fileEncoding", "GBK");
        String fileName = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\cityList.yaml";
        String fileName2 = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\cityMap.yaml";
        if (args.length > 0) {
            fileName = args[0];
        }
        File file = new File(fileName);
        String yaml = FileUtils.readFileToString(file, fileEncoding);
        //List<City> list = new ArrayList<City>();
        List<City> list = (List)Yaml.load(yaml);
        for (City city : list) {
            System.out.println(city.getClass().getName() + " name:" + city.getName() + " title:" + city.getTitle());
        }
        yaml = FileUtils.readFileToString(new File(fileName2), fileEncoding);
        Map<String, City> map = (Map)Yaml.load(yaml);
        for (String key : map.keySet()) {
            City city = map.get(key);
            System.out.println("key:" + key + " " + city.getClass().getName() + " name:" + city.getName() + " title:" + city.getTitle());
        }
    }
    
}

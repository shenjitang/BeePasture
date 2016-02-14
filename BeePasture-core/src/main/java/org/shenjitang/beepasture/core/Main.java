/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import org.shenjitang.beepasture.core.BeeGather;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.http.HttpTools;

/**
 *
 * @author xiaolie
 */
public class Main {
    private BeeGather webGather;

    public Main() {
    }

    public void setWebGather(BeeGather webGather) {
        this.webGather = webGather;
    }

    
    public static void main( String[] args ) throws Exception {
        System.out.println(493069 + (System.currentTimeMillis()/(24*3600*1000L)));
        //gatherUrlList("D:\\URLlist.txt", "D:\\URLlistResult.txt");
        String fileEncoding = System.getProperty("fileEncoding", "utf8");
        Boolean asyn = Boolean.valueOf(System.getProperty("asyn", "false"));
        //String fileEncoding = System.getProperty("fileEncoding", "utf8");
        //String fileName = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\mysql2mongodb.yaml";
        //String fileName = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\webgather_queue.yaml";
        String fileName = "D:\\workspace\\神机堂\\项目\\BeePasture\\BeePasture-core\\src\\main\\resources\\webgather1.yaml";
        //String fileName = "D:\\temp\\CZCEdata.yaml";
        if (args.length > 0) {
            fileName = args[0];
        }
        File file = new File(fileName);
        String yaml = FileUtils.readFileToString(file, fileEncoding);
        if (asyn) {
            AsynBeeGather webGather = new AsynBeeGather(yaml);
            webGather.init();
            webGather.doGather();
            webGather.saveTo();
        } else {
            BeeGather webGather = new BeeGather(yaml);
            webGather.init();
            webGather.doGather();
            webGather.saveTo();
        }
        //app.printYaml();
        
        
        //String keyword = "美食";
        //String urlKeyword = URLEncoder.encode(keyword, "GBK");
        //app.getShopListFromDianping("1", keyword);
        //List list = app.getCityList();
        //List list = app.getClassify("2");
        //List list = app.getChildClassify("2", "美食");
        //List list = app.getBID("2");
        //for (Object item : list) {
        //    System.out.println(item);
        //}
        
        
    }
    
    public void printYaml() throws Exception {
        Map map = new HashMap();
        Map map1 = new HashMap();
        map.put("var", map1);
        map1.put("name", "cityUrlList");
        map1.put("type", "list");
        List list = new ArrayList();
        list.add("http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${time}&do=getByPY&firstPY=A");
        list.add("http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${time}&do=getByPY&firstPY=B");
        list.add("http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${time}&do=getByPY&firstPY=C");
        list.add("http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${time}&do=getByPY&firstPY=D");
        list.add("http://www.dianping.com/ajax/json/index/citylist/getCitylist?_nr_force=${time}&do=getByPY&firstPY=E");
        map1.put("value", list);
        map1.put("dd", "123");
        map.put("aca", "bac");
        String s = Yaml.dump(map, true);
        System.out.println(s);
    }
    
    
    public static void gatherUrlList(String fileName, String outFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        List<String> urls = FileUtils.readLines(new File(fileName));
        for (String url : urls) {
            HttpTools tools = new HttpTools();
            String content = tools.doGet(url, null);
            sb.append(content).append("\n");
        }
        FileUtils.write(new File(outFile), sb, "gbk");
    }
    
    

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author xiaolie
 */
public class Main {
    public static Log MAIN_LOGGER = LogFactory.getLog("org.shenjitang.beepasture.core.Main");

    public Main() {
    }
    
    public static void main( String[] args ) throws Exception {
        //gatherUrlList("D:\\URLlist.txt", "D:\\URLlistResult.txt");
        String fileEncoding = System.getProperty("fileEncoding", "utf8");
        //String fileName = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\webgather_queue.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\wenshu_court_gov_cn.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\elasticsearch_city.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\webgather1.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\szb_info_es.yaml";
        String fileName = "D:\\temp\\baidu.yaml";
        if (args.length > 0) {
            fileName = args[0];
        }
        MAIN_LOGGER.info("start fileEncoding=" + fileEncoding + " script=" + fileName);
        File file = new File(fileName);
        String yaml = FileUtils.readFileToString(file, fileEncoding);
        if (Boolean.valueOf(System.getProperty("asyn", "false"))) {
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
        MAIN_LOGGER.info("finish fileEncoding=" + fileEncoding + " script=" + fileName);
        System.exit(0);
    }
    
//    public static void gatherUrlList(String fileName, String outFile) throws Exception {
//        StringBuilder sb = new StringBuilder();
//        List<String> urls = FileUtils.readLines(new File(fileName));
//        for (String url : urls) {
//            HttpTools tools = new HttpTools();
//            String content = tools.doGet(url, null);
//            sb.append(content).append("\n");
//        }
//        FileUtils.write(new File(outFile), sb, "gbk");
//    }
}

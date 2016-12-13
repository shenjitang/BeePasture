/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import java.io.File;
import java.io.FileNotFoundException;
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
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\webgather2.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\dir_test.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\dailystock.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\szb_info_es.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\szb_info_camel.yaml";
        //String fileName = "D:\\temp\\baidu.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\mongo2es.yaml";
        //String fileName = "D:\\temp\\webgather\\gt.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\baidu01_multi_1.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\download_1.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\esUpdate.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\baidu01_multi.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\163_news.yml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\baidu_base.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\mssql_kettle.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\mssql2mongodb.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\filter_sample.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\excel_load_3.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\dce_03.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\mongodb_test.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\shfe_03.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\dfcfw.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\bing_test.yml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\xyyj_ftp_log_1.yaml";
        String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\xyyj_bing_db.yml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\wuhua_web_study.yml";
        if (args.length > 0) {
            fileName = args[0];
        }
        MAIN_LOGGER.info("start fileEncoding=" + fileEncoding + " script=" + fileName);
        try {
            File file = new File(fileName);
            String yaml = FileUtils.readFileToString(file, fileEncoding);
            BeeGather webGather = new BeeGather(yaml);
            webGather.init();
            webGather.doGather();
            webGather.saveTo();
            MAIN_LOGGER.info("finish fileEncoding=" + fileEncoding + " script=" + fileName);
            if (args.length > 1 && "-d".equalsIgnoreCase(args[1])) {
                while(Boolean.TRUE) {
                    try {
                        Thread.sleep(60000L);
                    } catch (Exception e) {}
                }
            } else {
                System.exit(0);
            }
        } catch (FileNotFoundException e) {
            MAIN_LOGGER.warn(fileName + " 文件不存在！");
        }
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

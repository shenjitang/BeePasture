/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import java.io.File;
import org.apache.commons.io.FileUtils;

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
        //gatherUrlList("D:\\URLlist.txt", "D:\\URLlistResult.txt");
        String fileEncoding = System.getProperty("fileEncoding", "utf8");
        //String fileName = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\webgather_queue.yaml";
        String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\dir_test.yaml";
        if (args.length > 0) {
            fileName = args[0];
        }
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

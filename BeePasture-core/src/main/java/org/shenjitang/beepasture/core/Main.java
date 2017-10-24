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
        String fileEncoding = System.getProperty("fileEncoding", "utf8");
        String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\全国县以上农村低保情况_月度.yaml";
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
    
}

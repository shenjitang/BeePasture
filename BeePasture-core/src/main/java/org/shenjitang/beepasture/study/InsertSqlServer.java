/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.study;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author xiaolie
 */
public class InsertSqlServer {
    public static void main(String[] args) throws Exception {
        String fileEncoding = "UTF8";//System.getProperty("fileEncoding", "GBK");
        String fileName = "D:\\workspace\\神机堂\\项目\\webgather\\webgather-web\\src\\main\\resources\\graph_define.yaml";
        if (args.length > 0) {
            fileName = args[0];
        }
        File file = new File(fileName);
        String yaml = FileUtils.readFileToString(file, fileEncoding);
        String sql = "insert into plugin_graph_define (name, content) values ('defnie1', '" + yaml + "')";
        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:jtds:sqlserver://172.20.10.229:1433/anaplatform_chajian" , "sa" , "rz_229" ) ; 
        try {
            Statement stmt = conn.createStatement() ;   
            try {
                stmt.execute(sql);
            } finally {
                stmt.close();
            }
        } finally {
            conn.close();
        }
        

    }
}

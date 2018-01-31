/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author xiaolie
 */
public class ElectroHttpProxy implements Runnable {
    private static final Log LOGGER = LogFactory.getLog(ElectroHttpProxy.class);
    private Socket socket = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private String electroHost = "localhost";
    private int electroPort = 7080;
    private final static Object lock = new Object();
    private final static String END_MARK = "<<<__EOF__>>>";
    private Thread recevieThread = null;
    private static ElectroHttpProxy instance;
    private final StringBuilder sb = new StringBuilder();
    private String page = null;

    private ElectroHttpProxy() {
    }
    
    public static ElectroHttpProxy getInstance() {
        if (instance == null) {
            instance = new ElectroHttpProxy();
            instance.open();
            instance.receive();
        }
        return instance;
    }
    
    private void receive() {
        recevieThread = new Thread(this);
        recevieThread.setDaemon(true);
        recevieThread.start();
    }
    
    
    private void open() {
        try {
            socket = new Socket(electroHost, electroPort);
            OutputStream os = socket.getOutputStream();//字节输出流
            pw = new PrintWriter(os);//将输出流包装成打印流
            InputStream is = socket.getInputStream();
            br = new BufferedReader(new InputStreamReader(is, "utf8"));
        } catch (Exception e) {
            LOGGER.error(electroHost + ":" + electroPort + " 连接不上！");
            System.exit(-2);
        }
    }
    
    private void reOpenIfBreak() {
        if (isBreak()) {
            open();
        }
    }
    
    private boolean isBreak() {
        return (socket == null || socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown());
    }
    
    private void close() {
        try {
            pw.close();
        } catch (Exception e) {
            LOGGER.warn(e);
        }
        try {
            br.close();
        } catch (Exception e) {
            LOGGER.warn(e);
        }
        try {
            socket.close();
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }
    
    public synchronized String doGet(String url, Map heads, String encoding) throws Exception {
        reOpenIfBreak();
        page = null;
        pw.write(url + "\n" + END_MARK + "\n");
        pw.flush();
        synchronized (sb) {
            if (page == null) {
                sb.wait(120000);//等待两分钟
            }
        }
        return page;
    }
    
    @Override
    public void run() {
        while(true) {
            try {
                if (isBreak()) {
                    Thread.sleep(1000);
                } else {
                    synchronized(sb) {
                        String line = br.readLine();
                        if (line.contains(END_MARK)) {
                            sb.append(line.replaceAll("END_MARK", ""));
                            page = sb.toString();
                            sb.delete(0, sb.length());
                            sb.notify();
                        } else {
                            sb.append(line);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("", e);
            }
        }
    }
    
}

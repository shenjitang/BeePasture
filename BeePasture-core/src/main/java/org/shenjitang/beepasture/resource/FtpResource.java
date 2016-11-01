/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.util.Iterator;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import static org.shenjitang.beepasture.resource.FileResource.readFile;
import org.shenjitang.beepasture.resource.util.ResourceUtils;

/**
 *
 * @author xiaolie
 */
public class FtpResource extends BeeResource {
    private static final Log LOGGER = LogFactory.getLog(FtpResource.class);
    private FTPClient ftpClient;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private Boolean longConnected;
    private String path;

    public FtpResource() {
        this.ftpClient = new FTPClient();
    }
    
    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        FTPClientConfig config = new FTPClientConfig();
        if (param.containsKey("serverType")) {
            config = new FTPClientConfig((String)param.get("serverType"));
        } else {
            config = new FTPClientConfig();
        }
        ftpClient.configure(config);
        
        path = uri.getPath();
        host = uri.getHost();
        port = uri.getPort();
        if (port == -1) {
            port = 21;
        }
        username = ResourceUtils.get(param, "username", uri.getRawUserInfo());
        password = (String)params.get("password");//ResourceUtils.get(param, "password", (String)null);
        longConnected =  ResourceUtils.get(param, "longConnected", Boolean.FALSE);
        if (longConnected) {
            connect();
        }
        
    }
    
    private void connect() throws Exception {
        ftpClient.setControlKeepAliveReplyTimeout(10000);
            if (port != null) {
                ftpClient.connect(host, port);
            } else {
                ftpClient.connect(host);            
            }
            int reply = ftpClient.getReplyCode();
            System.out.println("connect :" + reply);
            if (StringUtils.isNotBlank(username)) {
                if (!ftpClient.login(username, password)) {
                    System.out.println("登录失败！");
                    System.exit(-1);
                }
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    @Override
    public void persist(String varName, Object obj, Map localParams) {
        String filename = getFilename(localParams);
        String localfile = getLocalfile(localParams, null);
        if (StringUtils.isBlank(localfile)) {
            localfile = getValue(varName, obj).toString();
            if (localfile.startsWith("file:")) {
                URI u = URI.create(localfile);
                localfile = u.getAuthority() + u.getPath();
            }
        }
        try {
            if (!ftpClient.isConnected()) {
                connect();
            }
            InputStream is = new FileInputStream(new File(localfile));            
            ftpClient.storeFile(filename, is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getFilename(Map localParam) {
        String filename = (String)localParam.get("filename");
        if (StringUtils.isBlank(filename)) {
            filename = (String)params.get("filename");
        }
        if (StringUtils.isBlank(filename)) {
            filename = path;
        } else if (!filename.startsWith("/") && StringUtils.isNotBlank(path)) {
            filename = path + "/" + filename;
        }
        return filename;
    }
    
    private String getLocalfile(Map loadParam, String filename) {
        String localfile = (String)loadParam.get("localfile");
        if (StringUtils.isBlank(localfile)) {
            localfile = (String)params.get("localfile");
            if (StringUtils.isBlank(localfile) && StringUtils.isNotBlank(filename)) {
                localfile = FilenameUtils.getName(filename);
            }
        }
        return localfile;
    }

    @Override
    public Object loadResource(Map localParam) throws Exception {
        if (!ftpClient.isConnected()) {
            connect();
        }
        String filename = getFilename(localParam);
        String localfile = (String)localParam.get("localfile");
        if (StringUtils.isBlank(localfile)) {
            localfile = FilenameUtils.getName(filename);
        }
        FileOutputStream localOutputStream = new FileOutputStream(localfile);
        System.out.println("ftp begin:" + filename);
        Boolean result = ftpClient.retrieveFile(filename, localOutputStream);
        int replyCode = ftpClient.getReplyCode();
        System.out.println("ftp down:" + filename + " result:" + result + " code;" + replyCode + ftpClient.getReplyString());
        localOutputStream.flush();
        localOutputStream.close();
        if (!longConnected) {
            ftpClient.disconnect();
        }
//        String encoding = getParam(localParam, "encoding", "GBK");
//        String format = getParam(localParam, "format", "plant");
//        File file = new File(localfile);
//        if (saveDefMap != null) {
//            return readFile(file, encoding, format);
//        } else {
//            return "file://" + localfile;
//        }
        return "file://" + localfile;
    }

    @Override
    public Iterator<Object> iterate(Map param) throws Exception {
        String filename = getFilename(param);//(String)param.get("filename");
        if (!ftpClient.isConnected()) {
            connect();
        }
        String encoding = ResourceUtils.get(param, "encoding", "GBK");
        InputStream stream = ftpClient.retrieveFileStream(filename);
        if (stream == null) {
            System.out.println("retriveFilee:" + filename + " 可能不存在，检查是否是绝对路径 returncode:" + ftpClient.getReplyCode() + " message:" + ftpClient.getReplyString());
            System.exit(-1);
        }
        return new StreamLineIterator(stream, encoding);
    }
    
    public static void main(String[] args) {
        System.out.println(FilenameUtils.getName("/baa/sss/abc.txt"));
    }
}

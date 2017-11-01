/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import com.aliyun.oss.OSSClient;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.util.ParseUtils;

/**
 *
 * @author xiaolie
 */
public class OssResource extends BeeResource {
    private OSSClient ossClient;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    
    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        endpoint = (String)param.get("endpoint");
        accessKeyId = (String)param.get("accessKeyId");
        accessKeySecret = (String)param.get("accessKeySecret");
        ossClient = new OSSClient(endpoint, accessKeyId,accessKeySecret);
    }

    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map params) {
        String encoding = (String)params.get("encoding");
//        if (StringUtils.isBlank(encoding)) {
//            encoding = "GBK";
//        }
        String bucket = (String)params.get("bucket");
        String osskey = (String)params.get("key");
        if (gatherStep != null) {
            osskey = gatherStep.doScript(osskey);
        }
        try {
            if (obj instanceof String) {
                String str = (String) obj;
                byte[] content = null;
                if (str.startsWith("file:")) {
                    URI _uri = URI.create(str);
                    String fileName = _uri.getAuthority() + _uri.getPath();
                    if (fileName == null) {
                        fileName = _uri.getSchemeSpecificPart();
                    }
                    File file = new File(fileName);
                    content = FileUtils.readFileToByteArray(file);
                } else {
                    content = ((String) obj).getBytes(encoding);
                }
                ossClient.putObject(bucket, osskey, new ByteArrayInputStream(content));
            } else if (obj instanceof File) {
                byte[] content = FileUtils.readFileToByteArray((File)obj);
                ossClient.putObject(bucket, osskey, new ByteArrayInputStream(content));
            } else {
                throw new UnsupportedOperationException("OSS 目前只支持字符串"); //To change body of generated methods, choose Tools | Templates.
            }
        } catch (Exception e) {
            LOGGER.warn("", e);
        }
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

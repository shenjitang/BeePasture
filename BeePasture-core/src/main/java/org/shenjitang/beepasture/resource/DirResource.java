/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.shenjitang.beepasture.core.GatherStep;

/**
 *
 * @author xiaolie
 */
public class DirResource extends BeeResource {
    private File path;

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
        String fileName = uri.getAuthority() + uri.getPath();
        path = new File(fileName);
    }

    
    public DirResource() {
    }

    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map persistParams) {
        throw new UnsupportedOperationException("Dir 资源不支持写操作");
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        String urlStr = (String)params.get("url");
        URI uri = URI.create(urlStr);
        
        String fileName = uri.getAuthority() + uri.getPath();
        String query  = uri.getQuery();
        Map iparams = new HashMap();
        List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
        if (queryPair != null) {
            for (NameValuePair nvp : queryPair) {
                iparams.put(nvp.getName(), nvp.getValue());
            }
        }
        IOFileFilter fileFilter = getFileFilter((String)iparams.get("filefilter"));
        IOFileFilter dirFilter = getFileFilter((String)iparams.get("dirfilter"));
        return FileUtils.listFiles(new File(fileName), fileFilter, dirFilter);
    }
    
    private IOFileFilter getFileFilter(String filterStr) {
        IOFileFilter filter = null;
        Boolean isAnd = true;
        if (StringUtils.isNotBlank(filterStr)) {
            List<String> list = splitFilterStr(filterStr);
            
            for(String f : list) {
                if ("|".equals(f)) {
                    isAnd = false;
                } else if ("&".equals(f)) {
                    isAnd = true;
                } else {
                    IOFileFilter aFilter = getOneFileFilter(f);
                    if (filter == null) {
                        filter = aFilter;
                    } else if (!isAnd) {
                        filter = FileFilterUtils.or(filter, aFilter);
                    } else {
                        filter = FileFilterUtils.and(filter, aFilter);
                    }
                }
            }
        }
        if (filter == null) {
            filter = FileFilterUtils.trueFileFilter();
        }
        return filter;
    }

    private List<String> splitFilterStr(String filterStr) {
        List<String> resultList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filterStr.length(); i++) {
            char c = filterStr.charAt(i);
            if (c == '&' || c == '|') {
                resultList.add(sb.toString());
                resultList.add(String.valueOf(c));
            } else {
                sb.append(c);
            }
        }
        resultList.add(sb.toString());
        return resultList;
    }

    private IOFileFilter getOneFileFilter(String f) {
        IOFileFilter filter = null;
        Boolean isNot = false;
        if (f.startsWith("~")) {
            isNot = true;
            f = f.substring(1);
        }
        if(f.endsWith("*")) {
            filter = FileFilterUtils.suffixFileFilter(f.substring(0, f.length() - 1), getCase());
        } else if(f.startsWith("*")) {
            filter = FileFilterUtils.suffixFileFilter(f.substring(1), getCase());
        } else {
            filter = FileFilterUtils.nameFileFilter(f, getCase());
        }
        if (isNot) {
            return FileFilterUtils.notFileFilter(filter);
        } else {
            return filter;
        }
    }
    
    private IOCase getCase() {
        String caseSensitivity = (String)params.get("caseSensitivity");
        if (!StringUtils.isBlank(caseSensitivity)) {
            if(!Boolean.valueOf(caseSensitivity)) {
                return IOCase.INSENSITIVE;
            }
        }
        return IOCase.SENSITIVE;
    }

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
        
}

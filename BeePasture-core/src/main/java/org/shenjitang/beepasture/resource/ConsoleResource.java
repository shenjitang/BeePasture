/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.beust.jcommander.internal.Sets;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.codehaus.plexus.util.StringUtils;
import org.shenjitang.beepasture.core.GatherStep;

/**
 *
 * @author xiaolie
 */
public class ConsoleResource extends BeeResource {
    protected File file;
    protected String fileName;
    static SerializeConfig mapping = new SerializeConfig();

    public ConsoleResource() {
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map persistParams) {
        String format = (String)uriParams.get("format");
        if ("json".equalsIgnoreCase(format)) {
            System.out.println(uri.getHost() + ">> " + varName + "=> " + JSON.toJSONString(obj));
        } else {
            System.out.println(uri.getHost() + ">> " + varName + "=> " + (obj == null? "null" : obj.toString()));
        }
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        throw new UnsupportedOperationException("console component do not support load action!");
    }

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception {
        throw new UnsupportedOperationException("console component do not support iterate action!");
    }

    @Override
    public Set<String> getParamKeys() {
        return Sets.newHashSet();
    }       
}

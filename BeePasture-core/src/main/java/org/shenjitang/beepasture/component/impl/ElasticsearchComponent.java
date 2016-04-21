/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.component.impl;

import com.alibaba.fastjson.JSONObject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.codehaus.plexus.util.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.shenjitang.beepasture.component.Component;
import org.shenjitang.beepasture.resource.ElasticsearchResource;

/**
 *
 * @author xiaolie
 */
public class ElasticsearchComponent implements Component {
    private ElasticsearchResource resource;
    private int batchExecuteCount = 100;
    private Map params;

    @Override
    public void persist(URI uri, String varName, Object obj) throws Exception {
        String index = (String)params.get("_index");
        if (StringUtils.isBlank(index)) {
            index = resource.getIndex();
        }
        String type = (String)params.get("_type");
        if (StringUtils.isBlank(type)) {
            type = resource.getType();
        }
        String idField = (String)params.get("_id");
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                try {
                    String _id = null;
                    if (StringUtils.isNotBlank(idField)) {
                        _id = ((Map)item).remove(idField).toString();
                    }
                    String indexContent = JSONObject.toJSONString(item);
                    if (StringUtils.isBlank(_id)) {
                        IndexRequest indexRequest = new IndexRequest(index, type).source(indexContent);
                        resource.getClient().index(indexRequest);
                    } else {
                        IndexRequest indexRequest = new IndexRequest(index, type, _id).source(indexContent);
                        UpdateRequest updateRequest = new UpdateRequest(index, type, _id)
                               .doc(indexContent)
                               .upsert(indexRequest);              
                        resource.getClient().update(updateRequest).get();       
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        } else {
            String _id = null;
            if (StringUtils.isNotBlank(idField)) {
                _id = (String) ((Map) obj).remove(idField);
            }
            String indexContent = JSONObject.toJSONString(obj);
            if (StringUtils.isBlank(_id)) {
                IndexRequest indexRequest = new IndexRequest(index, type).source(indexContent);
                resource.getClient().index(indexRequest);
            } else {
                IndexRequest indexRequest = new IndexRequest(index, type, _id).source(indexContent);
                UpdateRequest updateRequest = new UpdateRequest(index, type, _id)
                        .doc(indexContent)
                        .upsert(indexRequest);
                resource.getClient().update(updateRequest).get();
            }
        }
    }

    @Override
    public void setResource(Object resource) {
        this.resource = (ElasticsearchResource)resource;
    }

    @Override
    public void setParams(Map params) {
        this.params = params;
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

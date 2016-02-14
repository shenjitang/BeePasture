/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.component.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.shenjitang.beepasture.component.Component;
import org.shenjitang.beepasture.resource.MongoDbResource;

/**
 *
 * @author xiaolie
 */
public class MongodbComponent implements Component {
    private MongoDbResource resource;
    private int batchExecuteCount = 100;
    private Map params;

    public MongodbComponent() {
    }

    public void setDataSource(MongoDbResource resource) {
        this.resource = resource;
    }

    public void setBatchExecuteCount(int batchExecuteCount) {
        this.batchExecuteCount = batchExecuteCount;
    }

    @Override
    public void persist(URI uri, String varName, Object obj) throws Exception {
        String dbName = (String) params.get("database");
        if (StringUtils.isBlank(dbName)) {
            dbName = resource.getDatabaseName();
        }
        String colName = (String) params.get("collection");
        if (StringUtils.isBlank(colName)) {
            colName = varName;
        }
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                resource.getMongoDbOperater().insert(dbName, colName, (Map)item);
            }
        } else {
            resource.getMongoDbOperater().insert(dbName, colName, (Map)obj);
        }
        
    }

    @Override
    public void setResource(Object resource) {
        this.resource = (MongoDbResource) resource;
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

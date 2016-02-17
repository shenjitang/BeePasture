/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.component.impl;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;
import org.shenjitang.beepasture.component.Component;

/**
 *
 * @author xiaolie
 */
public class JdbcComponent implements Component {
    private static final Log LOGGER = LogFactory.getLog(JdbcComponent.class);
    private DataSource dataSource;
    private Map params;
    private int batchExecuteCount = 100;

    public JdbcComponent() {
    }


    public void setBatchExecuteCount(int batchExecuteCount) {
        this.batchExecuteCount = batchExecuteCount;
    }

    @Override
    public void persist(URI uri, String varName, Object obj) throws Exception {
        String topVarName = varName.split("[.]")[0];
        String tailVarName = null;
        if (topVarName.length() < varName.length()) {
            tailVarName = varName.substring(topVarName.length() + 1);
        }
        List<String> sqlList = new ArrayList();
        ScriptTemplateExecuter template = new ScriptTemplateExecuter();
        String sql = (String)params.get("sql");
        if (obj instanceof List) {
            int i = 0;
            for (Object item : (List)obj) {
                try {
                    Map map = new HashMap();
                    map.put(topVarName, item);
                    if (tailVarName != null) {
                        Map mm = (Map)item;
                        List iitemList = (List)mm.get(tailVarName);
                        for(Object iitem : iitemList) {
                            map.put(tailVarName, iitem);
                            sqlList.add(template.expressCalcu(sql, map));
                            if (++i >= batchExecuteCount) {
                                executeSql(sqlList);
                                sqlList.clear();
                            }
                        }
                    } else {
                        sqlList.add(template.expressCalcu(sql, map));
                        if (++i >= batchExecuteCount) {
                            executeSql(sqlList);
                            sqlList.clear();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (sqlList.size() > 0) {
                executeSql(sqlList);
            }
        } else {
            throw new RuntimeException("目前只支持List入库，obj:" + obj);
        }
        
    }
    
    private void executeSql( List<String> sqlList) throws Exception {
        Connection conn = dataSource.getConnection();
        try {
            Statement stmt = conn.createStatement();
            try {
                LOGGER.debug("batch to db begin ......");
                for (String sql : sqlList) {
                    if (StringUtils.isNotBlank(sql)) {
                        LOGGER.info(sql);
                        stmt.addBatch(sql);
                    }
                }
                stmt.executeBatch();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stmt.close();
            }
        } finally {
            conn.close();
        }
    }

    @Override
    public void setResource(Object resource) {
                this.dataSource = (DataSource) resource;
    }

    @Override
    public void setParams(Map params) {
        this.params = params;
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        List list = new ArrayList();
        String sql = (String)loadParam.get("sql");
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                LOGGER.debug("sql:" + sql);
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData meta = rs.getMetaData();
                while(rs.next()) {
                    Map record = new HashMap();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String key = meta.getColumnName(i);
                        int type = meta.getColumnType(i);
                        record.put(key, rs.getObject(i));
                    }
                    list.add(record);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
    
}

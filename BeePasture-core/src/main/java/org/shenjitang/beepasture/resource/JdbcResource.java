/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.lang3.StringUtils;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;

/**
 *
 * @author xiaolie
 */
public class JdbcResource extends BeeResource {
    private DataSource ds;
    private Integer batchExecuteCount = 100;
            
    public JdbcResource() {
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        if (param.containsKey("batch")) {
            batchExecuteCount = Integer.valueOf(param.get("batch").toString());
        }
        Properties props = new Properties();
        props.put("driverClassName", getDriverClassName(url));
        for (Object k : param.keySet()) {
            props.put(k, param.get(k));
        }
        ds = BasicDataSourceFactory.createDataSource(props);
    }

    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map persistParams) {
        String topVarName = varName.split("[.]")[0];
        String tailVarName = null;
        if (topVarName.length() < varName.length()) {
            tailVarName = varName.substring(topVarName.length() + 1);
        }
        List<String> sqlList = new ArrayList();
        ScriptTemplateExecuter template = new ScriptTemplateExecuter();
        String sql = (String)persistParams.get("sql");
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
            try {
                    Map map = new HashMap();
                    map.put(topVarName, obj);
                    if (tailVarName != null) {
                        Map mm = (Map)obj;
                        List iitemList = (List)mm.get(tailVarName);
                        for(Object iitem : iitemList) {
                            map.put(tailVarName, iitem);
                            String rsql = template.expressCalcu(sql, map);
                            executeSql(rsql);
                        }
                    } else {
                        String rsql = template.expressCalcu(sql, map);
                        executeSql(rsql);
                    }
            } catch (Exception ex) {
                    ex.printStackTrace();
            }
//            throw new RuntimeException("目前只支持List入库，obj:" + obj);
        }
    }
    
    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        List list = new ArrayList();
        String sql = (String)loadParam.get("sql");
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.debug("sql:" + sql);
            ResultSet rs = stmt.executeQuery(sql);
            list = resultSet2List(rs);
        }
        return list;
    }   
     
    private void executeSql( List<String> sqlList) {
        try {
            try (Connection conn = ds.getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    LOGGER.debug("batch to db begin ......");
                    for (String sql : sqlList) {
                        if (StringUtils.isNotBlank(sql)) {
                            LOGGER.info(sql);
                            stmt.addBatch(sql);
                        }
                    }
                    stmt.executeBatch();
                } catch (Exception e) {
                    LOGGER.warn("", e);
                }
            } catch (Exception ex) {
                LOGGER.warn("", ex);
            }
        } catch (Exception e) {
            LOGGER.warn("connect database", e);
        }
    }    
    
    private void executeSql(String sql) throws Exception {
        LOGGER.info(sql);
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }    
    
    private void executeSql(String sql, List params) throws Exception {
        LOGGER.info(sql);
        try (Connection conn = ds.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            stmt.execute();
        }
    } 
    
    private void bindParams(PreparedStatement stmt, List params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object obj = params.get(i);
            if (obj == null) {
                stmt.setString(i + 1, null);
            } else if (obj instanceof Integer) {
                stmt.setInt(i + 1, (Integer) obj);
            } else if (obj instanceof Date) {
                java.sql.Date d = new java.sql.Date(((Date) obj).getTime());
                stmt.setDate(i + 1, d);
            } else if (obj instanceof Long) {
                stmt.setBigDecimal(i + 1, BigDecimal.valueOf((Long) obj));
            } else if (obj instanceof Double) {
                stmt.setBigDecimal(i + 1, BigDecimal.valueOf((Double) obj));
            } else if (obj instanceof Float) {
                stmt.setBigDecimal(i + 1, BigDecimal.valueOf((Float) obj));
            } else if (obj instanceof Boolean) {
                stmt.setBoolean(i + 1, ((Boolean) obj));
            } else {
                stmt.setString(i + 1, obj.toString());
            }
        }
    }

    public String getDriverClassName(String url) {
        if (url.contains("jdbc:jtds:sqlserver")) {
            return "net.sourceforge.jtds.jdbc.Driver";
        } else if (url.contains("jdbc:sqlserver")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        } else if (url.contains("jdbc:oracle:thin")) {
            return "oracle.jdbc.OracleDriver";
        } else if (url.contains("jdbc:mysql:")) {
            return "com.mysql.jdbc.Driver";
        } else {
            return null;
        }
    }    

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map loadParam) throws Exception {
        return new RecordIterator(loadParam);
    }

    @Override
    public Set<String> getParamKeys() {
        return Sets.newHashSet("sql");
    }

    @Override
    public void saveTo(GatherStep gatherStep, Map objMap, Map params) {
        Map checkMap = (Map)params.get("check");
        if (checkMap != null) {
            String sql = (String)checkMap.get("sql");
            List checkResult = querySql(sql, gatherStep, objMap, params);
            if (checkResult != null && (!checkResult.isEmpty())) { //查出来有记录
                Object exist = checkMap.get("exist"); //exist键下边必须是map， kye: sql, sqlParams
                if (exist != null) {
                    executeSql(gatherStep, objMap, (Map)exist);
                }
            } else { //查出来没有记录
                Object other = checkMap.get("other"); //other键下边必须是map， kye: sql, sqlParams
                if (other != null) {
                    executeSql(gatherStep, objMap, (Map)other);
                }
            }
        }
        //不管检查结果，如果save键下边有sql，还是要执行
        executeSql(gatherStep, objMap, params);
    }
    
    private List resultSet2List(ResultSet rs) throws SQLException {
        List list = new ArrayList();
        ResultSetMetaData meta = rs.getMetaData();
        while (rs.next()) {
            Map record = new HashMap();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String key = meta.getColumnName(i);
                int type = meta.getColumnType(i);
                record.put(key, rs.getObject(i));
            }
            list.add(record);
        }
        return list;
    }
    
    private List querySql(String sql, GatherStep gatherStep, Map objMap, Map queryParams) {
        List list = new ArrayList();
        sql = gatherStep.doScript(sql);
        LOGGER.debug("sql:" + sql);
        List sqlParams = (List) queryParams.get("sqlParams");
        try (Connection conn = ds.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (sqlParams != null && (!sqlParams.isEmpty())) {
                    List ps = new ArrayList();
                    for (Object param : sqlParams) {
                        Object v = getSqlParamsValue((String) param, objMap);
                        ps.add(v);
                    }
                    bindParams(stmt, ps);
                }
                ResultSet rs = stmt.executeQuery();
                list = resultSet2List(rs);
        } catch (Exception e) {
            LOGGER.warn(sql, e);
        }
        return list;
    }  
    
    private void executeSql(GatherStep gatherStep, Map objMap, Map params) {
        String sql = (String)params.get("sql");
        if (sql == null) {
            return;
        }
        List sqlParams = (List) params.get("sqlParams");
        try {
            sql = gatherStep.doScript(sql);
            if (sqlParams != null && (!sqlParams.isEmpty())) {
                List ps = new ArrayList();
                for (Object param : sqlParams) {
                    Object v = getSqlParamsValue((String) param, objMap);
                    ps.add(v);
                }
                executeSql(sql, ps);
            } else {
                executeSql(sql);
            }
        } catch (Exception e) {
            LOGGER.warn(sql, e);
        }
    }
    
    

    private Object getSqlParamsValue(String param, Map objMap) {
        String[] pa = param.split("[.]");
        if (pa.length == 2) {
            Map map = (Map)objMap.get(pa[0]);
            return map.get(pa[1]);
        } else {
            Object sub = objMap.get("it");
            if (sub != null && sub instanceof Map) {
                Object result = ((Map)sub).get(param);
                if (result != null) {
                    return result;
                }
            }
            sub = objMap.get("local");
            if (sub != null && sub instanceof Map) {
                Object result = ((Map)sub).get(param);
                if (result != null) {
                    return result;
                }
            }
            sub = objMap.get("_this");
            if (sub != null && sub instanceof Map) {
                Object result = ((Map)sub).get(param);
                if (result != null) {
                    return result;
                }
            }
            sub = objMap.get("_page");
            if (sub != null && sub instanceof Map) {
                Object result = ((Map)sub).get(param);
                if (result != null) {
                    return result;
                }
            }
            sub = objMap.get("_item");
            if (sub != null && sub instanceof Map) {
                Object result = ((Map)sub).get(param);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }
    
    public class RecordIterator implements Iterator<Object> {
        private final Connection conn;
        private final Statement stmt;
        private final ResultSet rs;
        private final ResultSetMetaData meta;

        public RecordIterator(Map loadParam) throws Exception {
            String sql = (String)loadParam.get("sql");
            conn = ds.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            meta = rs.getMetaData();
        }

        @Override
        public boolean hasNext() {
            boolean next = false;
            try {
                next = rs.next();
                if (!next) {
                    try {
                        rs.close();
                    } catch (Exception e) {}
                    try {
                        stmt.close();
                    } catch (Exception e) {}
                    try {
                        conn.close();
                    } catch (Exception e) {}
                }
            } catch (SQLException e) {
                throw new RuntimeException("", e);
            }
            return next;
        }

        @Override
        public Object next() {
            Map record = new HashMap();
            try {
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String key = meta.getColumnName(i);
                    int type = meta.getColumnType(i);
                    record.put(key, rs.getObject(i));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return record;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }

}

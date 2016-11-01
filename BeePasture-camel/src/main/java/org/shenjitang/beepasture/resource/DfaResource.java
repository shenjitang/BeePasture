/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.shenjitang.beepasture.algorithm.DeterministicFiniteAutomaton;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.resource.BeeResource;

/**
 *
 * @author xiaolie
 */
public class DfaResource extends BeeResource {
    private final DeterministicFiniteAutomaton dfa = new DeterministicFiniteAutomaton();
    private Object datas;

    @Override
    public void init(String url, Map param) throws Exception {
        ResourceMng resourceMng = BeeGather.getInstance().getResourceMng();
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
        Object wordDef = param.get("words");
        Set wordSet = new HashSet();
        if (wordDef instanceof List) {
            wordSet.addAll((List)wordDef);
        } else if (wordDef instanceof Map) {
            BeeResource res = resourceMng.getResource((String)((Map)wordDef).get("resource"));
            wordSet = getWords(res, (Map)wordDef);
        } else if (wordDef instanceof String) {
            String wordStr = (String) wordDef;
            BeeResource res = resourceMng.getResource(wordStr);
            if (res != null) {
                wordSet = getWords(res, param);
            } else {
                int idx = wordStr.indexOf(":");
                if (idx > 0) {
                    String[] wordContent = (String[])resourceMng.getResource(wordStr).loadResource(null);
                    for (String w : wordContent) {
                        wordSet.add(w);
                    }
                }
            }
        }
        dfa.addSensitiveWordToHashMap(wordSet);
    }
    
    protected Set getWords(BeeResource res, Map params) throws Exception {
        Set wordSet = new HashSet();
        List res1 = (List) res.loadResource(params);
        for (Object obj : res1) {
            if (obj instanceof String) {
                wordSet.add(obj);
            } else if (obj instanceof Map) {
                Map m = (Map) obj;
                Object w = m.values().iterator().next();
                if (w != null) {
                    wordSet.add(w.toString());
                }
            } else {
                wordSet.add(obj.toString());
            }

        }
        return wordSet;
    }
    
    @Override
    public void persist(String varName, Object obj, Map flowParams) {
        List fields = getList(flowParams, "target");
        List headList = new ArrayList();
        if (fields.size() > 0) {
            for (Object field : fields) {
                String str = (String)((Map)obj).get(field);
                Map wordMap = dfa.analysis(str);
                String vec = "dfa_vec_" + field;
                ((Map)obj).put(vec, wordMap);
                headList.add(vec);
            }
            datas = obj;
            flowOut(obj, headList);
        } else {
            Map wordMap = dfa.analysis((String) obj);
            Map resultMap = new HashMap();
            resultMap.put("message", obj);
            resultMap.put("dfa_vec_message", wordMap);
            headList.add("dfa_vec_message");
            datas = resultMap;
            flowOut(resultMap, headList);
        }
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        return datas;
    }

    @Override
    public Iterator<Object> iterate(Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.beepasture.algorithm.NagaoAlgorithm;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.resource.BeeResource;
import org.shenjitang.beepasture.resource.ResourceMng;
import org.shenjitang.beepasture.resource.util.ResourceUtils;

/**
 *
 * @author xiaolie
 */
public class NagaoResource extends BeeResource {
    private NagaoAlgorithm nagao;
    private Integer N = 5;
    private List threshold;
    //private String stopzi = "的很了么呢是嘛个都也比还这于不与才上用就好在和对挺去后没说";
    private String stopzi = "的很了么呢是嘛个都也比还这于不与才上用就好在和对挺去后没说你我他她啊哇";
    private List stopwords;
    private List<Map> result;

    public NagaoResource() {
        super();
        threshold = new ArrayList();
        threshold.add(20);
        threshold.add(3);
        threshold.add(3);
        threshold.add(5);
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
        if (param.containsKey("N")) {
            N = Integer.valueOf(param.get("N").toString());
        }
        if (param.containsKey("threshold")) {
            threshold = (List)param.get("threshold");
        }
        if (param.containsKey("stopzi")) {
            String stop = (String)param.get("stopzi");
            if(!StringUtils.isNotBlank(stop)) {
                if(ResourceMng.maybeResource(stop)) {
                    stopzi = (String)BeeGather.getInstance().getResourceMng().getResource(stop).loadResource(null);
                }
            }
        }
        if (param.containsKey("stopwords")) {
            Object stop = param.get("stopwords");
            if (stop instanceof List) {
                stopwords = (List)stop;
            } else if(StringUtils.isNotBlank((String)stop)) {
                if(ResourceMng.maybeResource((String)stop)) {
                    stopwords = (List)BeeGather.getInstance().getResourceMng().getResource((String)stop).loadResource(null);
                }
            }
        }
    }

    
    @Override
    public void persist(String varName, Object obj, Map params) {
        if ("__start__".equalsIgnoreCase((String)obj)) {
            nagao = new NagaoAlgorithm(N, threshold, stopzi);
        } else if ("__end__".equalsIgnoreCase((String)obj)) {
            //step 2: sort PTable and count LTable
            nagao.countLTable();
            //step3: count TF and Neighbor
            nagao.countTFNeighbor();
            //step4: save TF NeighborInfo and MI
            result = nagao.saveTFNeighborInfoMI(stopwords);
            for (Map record : result) {
                flowOut(record, null);
            }
        } else {
            nagao.addToPTable((String)obj);
        }
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        return result;
    }

    @Override
    public Iterator<Object> iterate(Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}

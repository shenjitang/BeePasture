/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import com.google.common.collect.Sets;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shenjitang.beepasture.core.GatherStep;

/**
 *
 * @author xiaolie
 */
public class CamelContextResource extends BeeResource {
    private static final Log LOGGER = LogFactory.getLog(CamelContextResource.class);
    
//    private ApplicationContext springContext;
    private CamelContext camelContext;
    private ProducerTemplate camelProducer;
    private ConsumerTemplate camelConsumer;
//    private List<Map> flowList;
//    private static int threadNo = 1;

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
        this.camelConsumer = camelContext.createConsumerTemplate();
        this.camelProducer = camelContext.createProducerTemplate();
    }

    public Exchange receive(String endpnt) {
        return camelConsumer.receive(endpnt);
    }



    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
    }

    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map params) {
        String to = (String)params.get("route");
        if (to == null) {
            to = (String)params.get("endpoint");
//            if (to == null) {
//                Object map1 = params.get("to");
//                if (map1 instanceof Map) {
//                    to = (String)((Map)map1).get("endpoint");
//                }
//            }
        }
        List headFields = (List)params.get("header");
        List bodyFields = (List)params.get("body");
        
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                try {
                    camelProducer.sendBodyAndHeaders(to, subBodyMap(item, bodyFields), subHeaderMap(item, headFields));
                    //camelProducer.requestBodyAndHeaders(to, subBodyMap(item, bodyFields), subHeaderMap(item, headFields));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                camelProducer.sendBodyAndHeaders(to, subBodyMap(obj, bodyFields), subHeaderMap(obj, headFields));
                //camelProducer.requestBodyAndHeaders(to, subBodyMap(obj, bodyFields), subHeaderMap(obj, headFields));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        String endPoint = (String)loadParam.get("endpoint");
        Object obj = camelConsumer.receiveBody(endPoint);
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^endPoint:" + endPoint);
        
        return obj;
    }
    
    protected Object subBodyMap(Object item, List fields) throws Exception {
        if (fields == null || fields.isEmpty()) {
            return item;
        }
        Map resultMap = new HashMap();
        Method method = method(item);
        for (Object name : fields) {
            Object value = item instanceof Map ? ((Map)item).get(name) : method.invoke(item, (String)name);
            resultMap.put(name, value);
        }
        return resultMap;
    }

    protected Map subHeaderMap(Object item, List fields) throws Exception {
        Map resultMap = new HashMap();
        if (fields == null) {
            return resultMap;
        }
        Method method = method(item);
        for (Object name : fields) {
            Object value = item instanceof Map ? ((Map)item).get(name) : method.invoke(item, (String)name);
            resultMap.put(name, value);
        }
        return resultMap;
    }   
    
    private Method method(Object obj) throws NoSuchMethodException {
        try {
            return obj.getClass().getMethod("get", Object.class);
        } catch (NoSuchMethodException e) {
            return obj.getClass().getMethod("get", String.class);
        }
    }

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<String> getParamKeys() {
        return Sets.newHashSet("endpoint", "route", "header", "body");
    }

}

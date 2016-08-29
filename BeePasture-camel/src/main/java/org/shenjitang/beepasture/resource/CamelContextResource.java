/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.core.GatherStep;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 *
 * @author xiaolie
 */
public class CamelContextResource extends BeeResource {
    private static final Log LOGGER = LogFactory.getLog(CamelContextResource.class);
    
    private ApplicationContext springContext;
    private CamelContext camelContext;
    private ProducerTemplate camelProducer;
    private ConsumerTemplate camelConsumer;
    private List<Map> flowList;
    private static int threadNo = 1;

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        String part = uri.getSchemeSpecificPart();
        LOGGER.debug("******part: " + part);
        if (StringUtils.isNotBlank(part) && (!"null".equalsIgnoreCase(part))) {
            springContext = new FileSystemXmlApplicationContext(part);
            camelContext = springContext.getBean(CamelContext.class);
        } else {
            camelContext = new DefaultCamelContext();
        }
        camelProducer = camelContext.createProducerTemplate();
        camelConsumer = camelContext.createConsumerTemplate();
        BeeGather beeGather = BeeGather.getInstance();
        flowList = (List)beeGather.getProgram().get("flow");//(List<Map>)param.get("gather");
        if (flowList != null) {
            for (Map processMap : flowList) {
                String with = (String)processMap.get("with");
                if (StringUtils.isBlank(with) || name.equalsIgnoreCase(with)) {
                    startProcess(processMap);
                }
            }
        }
    }
    
    protected void startProcess(final Map processMap) throws ClassNotFoundException {
        final String url = (String)processMap.get("url");
        final String bodyType = (String)processMap.get("type");
        final Class clazz = bodyType == null?null:Class.forName(bodyType);
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Exchange exchange = camelConsumer.receive(url);
                        Object body = clazz == null? exchange.getIn().getBody() : exchange.getIn().getBody(clazz);
                        if (body instanceof org.fusesource.hawtbuf.Buffer) {
                            try {
                                body = new String(((org.fusesource.hawtbuf.Buffer)body).getData(), "utf8");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Map<String, Object> headers = exchange.getOut().getHeaders();
                        doProcess(processMap, body, headers);
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                }
            }

        }, "bee-camel-process-" + threadNo++);
        th.start();
        LOGGER.debug("Thread: " + th.getName() + " started.");
    }

    private void doProcess(final Map processMap, final Object body, final Map<String, Object> headers) {
        GatherStep gatherStep = new GatherStep(processMap);
        gatherStep.onceGather(body);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ApplicationContext getSpringContext() {
        return springContext;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public ProducerTemplate getCamelProducer() {
        return camelProducer;
    }

    public ConsumerTemplate getCamelConsumer() {
        return camelConsumer;
    }
    
    public static void main(String[] args) throws Exception {
//        String url = "camelContext:cameltest.xml";
//        CamelContextResource resource = new CamelContextResource();
//        resource.init(url, null);
        //org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/szb_info_camel_asyn.yaml"});
        //org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/szb_info_camel.yaml"});
        //org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/camel_only.yaml", "-d"});
        org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/esper_accesslog.yaml", "-d"});
        
    }

    @Override
    public void persist(String varName, Object obj, Map params) {
        String to = (String)params.get("route");
        if (to == null) {
            to = (String)params.get("endpoint");
        }
        List headFields = (List)params.get("header");
        List bodyFields = (List)params.get("body");
        
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                try {
                    camelProducer.requestBodyAndHeaders(to, subBodyMap(item, bodyFields), subHeaderMap(item, headFields));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                camelProducer.requestBodyAndHeaders(to, subBodyMap(obj, bodyFields), subHeaderMap(obj, headFields));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        String endPoint = (String)loadParam.get("endpoint");
        Object obj = camelConsumer.receiveBody(endPoint);
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^endPoint:" + endPoint);
        
        return obj;
    }
    
    protected Object subBodyMap(Object item, List fields) throws Exception {
        if (fields == null) {
            return item;
        }
        Map resultMap = new HashMap();
        Method method = item.getClass().getMethod("get", String.class);
        for (Object name : fields) {
            Object value = method.invoke(item, (String)name);
            resultMap.put(name, value);
        }
        return resultMap;
    }

    protected Map subHeaderMap(Object item, List fields) throws Exception {
        Map resultMap = new HashMap();
        if (fields == null) {
            return resultMap;
        }
        Method method = item.getClass().getMethod("get", String.class);
        for (Object name : fields) {
            Object value = method.invoke(item, (String)name);
            resultMap.put(name, value);
        }
        return resultMap;
    }    
}

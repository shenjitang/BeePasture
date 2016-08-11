/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

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
    private List<Map> processList;
    private static int threadNo = 1;

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        String part = uri.getSchemeSpecificPart();
        LOGGER.debug("******part: " + part);
        if (StringUtils.isNotBlank(part)) {
            springContext = new FileSystemXmlApplicationContext(part);
            camelContext = springContext.getBean(CamelContext.class);
        } else {
            camelContext = new DefaultCamelContext();
        }
        camelProducer = camelContext.createProducerTemplate();
        camelConsumer = camelContext.createConsumerTemplate();
        processList = (List<Map>)param.get("gather");
        if (processList != null) {
            for (Map processMap : processList) {
                startProcess(processMap);
            }
        }
    }
    
    protected void startProcess(final Map processMap) {
        final String url = (String)processMap.get("url");
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                Exchange exchange = camelConsumer.receive(url);
                Object body = exchange.getOut().getBody();
                Map<String, Object> headers = exchange.getOut().getHeaders();
                doProcess(processMap, body, headers);
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
        org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/szb_info_camel_asyn.yaml"});
        //org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/szb_info_camel.yaml"});
        //org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/camel_only.yaml", "-d"});
        
    }

    @Override
    public void persist(String varName, Object obj, Map params) {
        String to = (String)params.get("route");
        if (to == null) {
            to = (String)params.get("endpoint");
        }
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                try {
                    camelProducer.requestBody(to, item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                camelProducer.requestBody(to, obj);
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
    
}

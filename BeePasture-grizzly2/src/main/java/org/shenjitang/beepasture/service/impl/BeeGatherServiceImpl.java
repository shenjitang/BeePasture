/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.shenjitang.beepasture.service.impl;

import com.alibaba.fastjson.JSON;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.service.BeeGatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("service")
public class BeeGatherServiceImpl implements BeeGatherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeeGatherServiceImpl.class);

    public BeeGatherServiceImpl() {
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "text/html; charset=UTF-8"})
    public String help() {
        Map result = new HashMap();
        result.put("success", Boolean.TRUE);
        result.put("time", new Date());
        result.put("help", "请使用POST方法上传并执行脚本");
        return JSON.toJSONString(result);
    }
    
    @Override
    @POST
    @Produces({MediaType.APPLICATION_JSON, "text/html; charset=UTF-8"})
    public String gather(String yaml) {
        Map result = new HashMap();
        try {
            BeeGather beeGather = new BeeGather(yaml);
            beeGather.init();
            Map vars = beeGather.doGather();
            beeGather.saveTo();
            result.put("success", Boolean.TRUE);
            result.put("data", vars);
        } catch (Exception e) {
            LOGGER.warn("", e);
            result.put("success", Boolean.FALSE);
            result.put("error", e.getMessage());
            result.put("data", e.getStackTrace());
        }
        return JSON.toJSONString(result);
    }

}
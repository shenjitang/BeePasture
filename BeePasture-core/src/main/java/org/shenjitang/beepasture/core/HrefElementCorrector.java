/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlcleaner.TagNode;
import org.shenjitang.beepasture.http.HttpService;
import org.shenjitang.beepasture.http.HttpServiceMng;
import org.shenjitang.beepasture.resource.BeeResource;

/**
 *
 * @author xiaolie
 */
public class HrefElementCorrector {
    public static final ImmutableMap<String, String> CONTENT_TYPE_MAP = ImmutableMap.<String, String>builder()
        .put("application/zip", "zip")
        .put("application/msword", "doc")
        .put("application/x-ppt", "ppt")
        .put("application/pdf", "pdf")
        .put("application/x-xls", "xls")
        .put("application/vnd.ms-excel", "xls")
        .build();

    
    protected static final Log LOGGER = LogFactory.getLog(GatherStep.class);
    private GatherStep gatherStep;

    public HrefElementCorrector(GatherStep gatherStep) {
        this.gatherStep = gatherStep;
    }
    
    List attachmentUrlCorrectAll (List pages, Map params) {
        List list = new ArrayList();
        for (Object page : pages) {
            list.add(attachmentUrlCorrect(page, params));
        }
        return list;
    }
    
    TagNode attachmentUrlCorrect (Object page, Map params) {
        try {
            if (page instanceof TagNode) {
                return attachmentUrl((TagNode) page, params);
            } else {
                return attachmentUrl(page.toString(), params);
            }
        } catch (Exception e) {
            LOGGER.warn("dataimage", e);
        }
        return null;
    }
    

    private TagNode attachmentUrl(TagNode tagNode, Map params) {
        List aNodeList = tagNode.getElementListByName("a", true);
        for (Object objNode : aNodeList) {
            try {
                TagNode aNode = (TagNode)objNode;
                String href = aNode.getAttributeByName("href");
                if (StringUtils.isBlank(href) || href.contains("#") || href.endsWith(".html") || href.contains("javascript")) {
                    continue;
                }
                if (!href.toLowerCase().startsWith("http")) {
                    if (href.startsWith("/")) {
                        URI uri = URI.create(gatherStep.ourl.toString());
                        href = uri.getScheme() + "://" + uri.getHost() + href;
                    } else {
                        href = StringUtils.substringBeforeLast(gatherStep.ourl.toString(), "/") + "/" + href;
                    }
                }
                LOGGER.info("attach url: " + href);
                String fileName = downloadIfIs(href);
                if (StringUtils.isNotBlank(fileName)) {
                    LOGGER.info("attach download to : " + fileName);
                    String shortFilename = new File(fileName).getName();
                    String url = (String)params.get("url");
                    String path = (String)params.get("path");
                    String osskey = null;
                    if (path == null) {
                        osskey = shortFilename;
                    } else {
                        if (path.endsWith("/")) {
                            osskey = path + shortFilename;
                        } else {
                            osskey = path + "/" + shortFilename;
                        }
                    }
                    params.put("key", osskey);
                    BeeResource resource = gatherStep.beeGather.getResourceMng().getResource(url, false);
                    resource.persist(gatherStep, null, new File(fileName), params);
                    String targetHref = (String)params.get("targetHref");
                    if (!targetHref.endsWith("/")) {
                        targetHref += "/";
                    }
                    targetHref += osskey;
                    aNode.removeAttribute("href");
                    aNode.addAttribute("href", targetHref);
                }
            } catch (Exception e) {
                LOGGER.warn("check head for judge download", e);
            }
        }
        return tagNode;
    }


    private TagNode attachmentUrl(String tagStr, Map params) throws Exception {
        TagNode node = gatherStep.pageAnalyzer.toTagNode(tagStr);
        return attachmentUrl(node, params);
    }
    
    List dataimageAll(List pages) {
        List list = new ArrayList();
        for (Object page : pages) {
            list.add(dataimage(page));
        }
        return list;
    }

    TagNode dataimage(Object page) {
            try {
                if (page instanceof TagNode) {
                    return dataimage((TagNode)page);
                } else {
                    return dataimage(page.toString());
                }
            } catch (Exception e) {
                LOGGER.warn("dataimage", e);
            }
            return null;
    }

    TagNode dataimage(TagNode tagNode) throws Exception {
        TagNode[] allNode = tagNode.getAllElements(true);
        for (TagNode node : allNode) {
            if ("img".equalsIgnoreCase(node.getName())) {
                String attributeName = "src";
                String imgUrl = node.getAttributeByName(attributeName);
                if (imgUrl == null) {
                    attributeName = "data-src";
                    imgUrl = node.getAttributeByName(attributeName);
                }
                if (StringUtils.isNotBlank(imgUrl)) {
                    if (!imgUrl.toLowerCase().startsWith("http")) {
                        if (imgUrl.startsWith("/")) {
                            URI uri = URI.create(gatherStep.ourl.toString());
                            imgUrl = uri.getScheme() + "://" + uri.getHost() + imgUrl;
                        } else {
                            imgUrl = StringUtils.substringBeforeLast(gatherStep.ourl.toString(), "/") + "/" + imgUrl;
                        }
                    }
                    LOGGER.info("image:" + imgUrl);
                    BeeResource beeResource = gatherStep.beeGather.getResourceMng().getResource(imgUrl, false);
                    String str = (String)beeResource.loadResource(gatherStep, ImmutableMap.of("dataimage", Boolean.TRUE));
                    node.removeAttribute("src");
                    node.addAttribute("src", str);
                }
            }
        }
        return tagNode;
    }

    TagNode dataimage(String str) throws Exception {
        TagNode node = gatherStep.pageAnalyzer.toTagNode(str);
        return dataimage(node);
    }

    private String downloadIfIs(String href) throws Exception  {
        BeeResource beeResource = gatherStep.beeGather.getResourceMng().getResource(href, false);
        HttpService httpTools = HttpServiceMng.get(beeResource.getUri());
        Map<String, String> responseHeads = httpTools.doHead(href, null);
        String contentType = responseHeads.get("Content-Type");
        if (CONTENT_TYPE_MAP.containsKey(contentType)) {
        //if (responseHeads.containsKey("Content-Disposition")) {
            return httpTools.downloadFile(href, null, "./temp", null);
        }
        return null;
    }
}

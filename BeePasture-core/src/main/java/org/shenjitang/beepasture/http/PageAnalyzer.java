/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shenjitang.beepasture.http;

import java.util.ArrayList;
import java.util.List;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;

/**
 *
 * @author xiaolie
 */
public class PageAnalyzer {
    public HtmlCleaner cleaner;  

    public PageAnalyzer() {
         CleanerProperties prop = new  CleanerProperties();
         prop.setOmitUnknownTags(false);
         cleaner = new HtmlCleaner(prop);
        //cleaner.initCleanerTransformations(null);
    }


    public TagNode toTagNode(String pageContent) throws Exception {
        //pageContent = app.html2Xml(pageContent);
        return cleaner.clean(pageContent);
        //document = DocumentHelper.parseText(pageContent);
    }
    
    public List<String> getList(TagNode node, String xpathExpress) throws Exception {
//        XPath dbPaths = new DefaultXPath(xpathExpress);
//        List results = dbPaths.selectNodes(document);
//        return results;
        XPather xPather = new XPather(xpathExpress);
        Object[] objs = xPather.evaluateAgainstNode(node);
        List resList = new ArrayList();
        for (Object o : objs) {
            if (o instanceof TagNode) {
                resList.add(cleaner.getInnerHtml((TagNode)o));
            } else {
                resList.add(o.toString());
            }
        }
        return resList;
    }

    public String getText(TagNode node, String xpathExpress) throws Exception {
        List<String> list = getList(node, xpathExpress);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }
    
}

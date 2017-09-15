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

    public List<String> getList(TagNode node, String xpathExpress, boolean innerHtml) throws Exception {
          XPather xPather = new XPather(xpathExpress);
          Object[] objs = xPather.evaluateAgainstNode(node);
          List resList = new ArrayList();
          for (Object o : objs) {
              if (o instanceof TagNode) {
                  if (innerHtml) {
                      String oo = cleaner.getInnerHtml((TagNode)o);
                      resList.add(oo);
                  } else {
                      resList.add(o);
                  }
              } else {
                  resList.add(o.toString());
              }
          }
          return resList;
    }

    public String getText(TagNode node, String xpathExpress) throws Exception {
        boolean innerHtml = false;
        if (xpathExpress.startsWith("innerHtml(")) {
            xpathExpress = xpathExpress.substring(10, xpathExpress.length() - 1);
            innerHtml = true;
        }
        StringBuilder sb = new StringBuilder();
        List<String> list = getList(node, xpathExpress, false);
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            if (o instanceof TagNode) {
                System.out.println(">>>>>>>>>>>>>>>>> " + o);
                if (innerHtml) {
                  sb.append(cleaner.getInnerHtml((TagNode)o));
                } else {
                  XPather xPather = new XPather("text()");
                  Object[] oo = xPather.evaluateAgainstNode((TagNode)o);
                  for (Object one : oo) {
                      System.out.println(">>>>>>>>>>>>>>>>oo> " + oo);
                      sb.append(one.toString());
                  }
                }
            } else {
                System.out.println("><<<<<<<<<<<<<<< " + o);
                sb.append(o.toString());
            }
            if (i < list.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

}

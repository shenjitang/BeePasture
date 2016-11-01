/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.study;

import java.io.IOException;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import ik.analyzer.dic.Dictionary;
import ik.analyzer.lucene.IKAnalyzer;


/**
 * 算文档相识度：http://www.ruanyifeng.com/blog/2013/03/cosine_similarity.html
 * @author xiaolie
 */
public class IKAnalzyerDemo {
	public static void main(String[] args){
		//构建IK分词器，使用smart分词模式
		Dictionary.initial("target/classes/config/");
		IKAnalyzer analyzer = new IKAnalyzer();
		//获取Lucene的TokenStream对象
	    TokenStream ts = null;
		try {
			//ts = analyzer.tokenStream("myfield", new StringReader("这是一个中文分词的例子，你可以直接运行它！IKAnalyer can analysis english text too"));
            ts = analyzer.tokenStream("myfield", "这是一个中文分词的例子，你可以直接运行它！IKAnalyer can analysis english text too，中文， ikanalyer");
			//获取词元位置属性
		    OffsetAttribute  offset = ts.addAttribute(OffsetAttribute.class); 
		    //获取词元文本属性
		    CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
		    //获取词元文本属性
		    TypeAttribute type = ts.addAttribute(TypeAttribute.class);
		    
		    
		    //重置TokenStream（重置StringReader）
			ts.reset(); 
			//迭代获取分词结果
			while (ts.incrementToken()) {
			  System.out.println(offset.startOffset() + " - " + offset.endOffset() + " : " + term.toString() + " | " + type.type());
			}
			//关闭TokenStream（关闭StringReader）
			ts.end();   // Perform end-of-stream operations, e.g. set the final offset.

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//释放TokenStream的所有资源
			if(ts != null){
		      try {
				ts.close();
		      } catch (IOException e) {
				e.printStackTrace();
		      }
			}
	    }
		
	}    
}

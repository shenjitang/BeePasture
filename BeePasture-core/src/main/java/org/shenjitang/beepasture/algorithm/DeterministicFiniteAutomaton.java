/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author xiaolie
 */
public class DeterministicFiniteAutomaton {
    private Map<Object, Object> sensitiveWordMap;//标签关键字
    
    public DeterministicFiniteAutomaton() {
    }

    public Map<String, List> analysis(String content) {
        Map<String, List> resultMap = new HashMap();
        content = content + ".";
        int begin = -1;
        StringBuilder sb = new StringBuilder();
        Map thisMap = sensitiveWordMap;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (thisMap.containsKey(c)) {
                sb.append(c);
                if (begin == -1) {
                    begin = i;
                }
                thisMap = (Map) thisMap.get(c);
                if (thisMap.containsKey("isEnd")) {
                    String word = sb.toString();
                    List wv = resultMap.get(word);
                    if (wv == null) {
                        wv = new ArrayList();
                        resultMap.put(word, wv);
                    }
                    wv.add(begin);
                }
            } else {
                if (thisMap != sensitiveWordMap) {
                    thisMap = sensitiveWordMap;
                    if (sb.length() > 0) {
                        sb.delete(0, sb.length());
                    }
                    begin = -1;
                    i--;
                }
            }
        }
        return resultMap;
    }
    
    /** 
     * 读取敏感词库，将敏感词放入HashSet中，构建一个DFA算法模型：<br> 
     */  
    public void addSensitiveWordToHashMap(Set<String> keyWordSet) {  
        sensitiveWordMap = new HashMap(keyWordSet.size());     //初始化敏感词容器，减少扩容操作  
        String key = null;    
        Map nowMap = null;  
        Map<String, String> newWorMap = null;  
        //迭代keyWordSet  
        Iterator<String> iterator = keyWordSet.iterator();  
        while(iterator.hasNext()){  
            key = iterator.next();    //关键字  
            nowMap = sensitiveWordMap;  
            for(int i = 0 ; i < key.length() ; i++){  
                char keyChar = key.charAt(i);       //转换成char型  
                Object wordMap = nowMap.get(keyChar);       //获取  
                if(wordMap != null){        //如果存在该key，直接赋值  
                    nowMap = (Map) wordMap;  
                } else{     //不存在则，则构建一个map，同时将isEnd设置为0，因为他不是最后一个  
                    newWorMap = new HashMap();  
                    //newWorMap.put("isEnd", "0");     //不是最后一个  
                    nowMap.put(keyChar, newWorMap);  
                    nowMap = newWorMap;  
                }  
                  
                if(i == key.length() - 1){  
                    nowMap.put("isEnd", "1");    //最后一个  
                }  
            }  
        }  
    }  

    public static void main(String[] args) {
        Set<String> set = new HashSet();
        set.add("毛贼");
        set.add("毛贼洞");
        set.add("傻叉");
        set.add("傻X");
        set.add("二逼");
        set.add("二B");
        set.add("二b");
        set.add("二百五");
        set.add("250");
        String content = "你这个傻逼毛贼洞，带领一邦傻叉毛贼，建立了一个二百五的国度，真特么膈应。";
        DeterministicFiniteAutomaton dfa = new DeterministicFiniteAutomaton();
        dfa.addSensitiveWordToHashMap(set);
        Map map = dfa.analysis(content);
        for (Object key : map.keySet()) {
            System.out.println(key + " => " + map.get(key));
        }
        content = "你这个傻逼，真特么膈应。";
        DeterministicFiniteAutomaton dfa1 = new DeterministicFiniteAutomaton();
        dfa1.addSensitiveWordToHashMap(set);
        map = dfa1.analysis(content);
        for (Object key : map.keySet()) {
            System.out.println(key + " => " + map.get(key));
        }
    }
}

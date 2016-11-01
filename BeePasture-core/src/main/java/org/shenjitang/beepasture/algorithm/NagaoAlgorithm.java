/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.algorithm;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * http://www.jb51.net/article/70198.htm
 * Nagao算法：一种快速的统计文本里所有子字符串频次的算法。详细算法可见http://www.doc88.com/p-664123446503.html
 * 词频：该字符串在文档中出现的次数。出现次数越多越重要。
 * 左右邻个数：文档中该字符串的左边和右边出现的不同的字的个数。左右邻越多，说明字符串成词概率越高。
 * 左右熵：文档中该字符串的左边和右边出现的不同的字的数量分布的熵。类似上面的指标，有一定区别。
 * 交互信息：每次将某字符串分成两部分，左半部分字符串和右半部分字符串，计算其同时出现的概率除于其各自独立出现的概率，最后取所有的划分里面概率最小值。这个值越大，说明字符串内部凝聚度越高，越可能成词。
 * 
 * 输出：
 * 词，词频、左邻个数、右邻个数、左熵、右熵、交互信息
 * 
 * 
 * 算法具体流程：
 * 1.  将输入文件逐行读入，按照非汉字（[^\u4E00-\u9FA5]+）以及停词“的很了么呢是嘛个都也比还这于不与才上用就好在和对挺去后没说”，
 * 分成一个个字符串，代码如下：
 * String[] phrases = line.split("[^\u4E00-\u9FA5]+|["+stopwords+"]");
 * 停用词可以修改。
 * 2.  获取所有切分后的字符串的左子串和右子串，分别加入左、右PTable
 * 3.  对PTable排序，并计算LTable。LTable记录的是，排序后的PTable中，下一个子串同上一个子串具有相同字符的数量
 * 4.  遍历PTable和LTable，即可得到所有子字符串的词频、左右邻
 * 5.  根据所有子字符串的词频、左右邻结果，输出字符串的词频、左右邻个数、左右熵、交互信息
 *     term, tf, lnn, rnn, lne, rne, mi
 * 
 * @author xiaolie
 */
public class NagaoAlgorithm {
      private int N;
    
  private List<String> leftPTable;
  private int[] leftLTable;
  private List<String> rightPTable;
  private int[] rightLTable;
  private double wordNumber;
  private int[] threshold = new int[4];
    
  private Map<String, TFNeighbor> wordTFNeighbor;
    
  private String stopzi = "的很了么呢是嘛个都也比还这于不与才上用就好在和对挺去后没说";
    
  public NagaoAlgorithm(){
      //default N = 5
      N = 5;
      threshold[0] = 20;
      threshold[1] = 3;
      threshold[2] = 3;
      threshold[3] = 5;
      leftPTable = new ArrayList<String>();
      rightPTable = new ArrayList<String>();
      wordTFNeighbor = new HashMap<String, TFNeighbor>();
  }

  public NagaoAlgorithm(Integer N, List threshold, String stopzi){
    //default N = 5
    this.N = N;
    this.threshold[0] = Integer.valueOf(threshold.get(0).toString());
    this.threshold[1] = Integer.valueOf(threshold.get(1).toString());
    this.threshold[2] = Integer.valueOf(threshold.get(2).toString());
    this.threshold[3] = Integer.valueOf(threshold.get(3).toString());
    this.stopzi = stopzi;
    leftPTable = new ArrayList<String>();
    rightPTable = new ArrayList<String>();
    wordTFNeighbor = new HashMap<String, TFNeighbor>();
  }
  //reverse phrase
  private String reverse(String phrase) {
    StringBuilder reversePhrase = new StringBuilder();
    for (int i = phrase.length() - 1; i >= 0; i--)
      reversePhrase.append(phrase.charAt(i));
    return reversePhrase.toString();
  }
  //co-prefix length of s1 and s2
  private int coPrefixLength(String s1, String s2){
    int coPrefixLength = 0;
    for(int i = 0; i < Math.min(s1.length(), s2.length()); i++){
      if(s1.charAt(i) == s2.charAt(i))  coPrefixLength++;
      else break;
    }
    return coPrefixLength;
  }
  //add substring of line to pTable
  public void addToPTable(String line){
    //split line according to consecutive none Chinese character
    String[] phrases = line.split("[^\u4E00-\u9FA5]+|["+stopzi+"]");
    for(String phrase : phrases){
      for(int i = 0; i < phrase.length(); i++)
        rightPTable.add(phrase.substring(i));
      String reversePhrase = reverse(phrase);
      for(int i = 0; i < reversePhrase.length(); i++)
        leftPTable.add(reversePhrase.substring(i));
      wordNumber += phrase.length();
    }
  }
    
  //count lTable
  public void countLTable(){
    Collections.sort(rightPTable);
    rightLTable = new int[rightPTable.size()];
    for(int i = 1; i < rightPTable.size(); i++)
      rightLTable[i] = coPrefixLength(rightPTable.get(i-1), rightPTable.get(i));
      
    Collections.sort(leftPTable);
    leftLTable = new int[leftPTable.size()];
    for(int i = 1; i < leftPTable.size(); i++)
      leftLTable[i] = coPrefixLength(leftPTable.get(i-1), leftPTable.get(i));
      
    System.out.println("Info: [Nagao Algorithm Step 2]: having sorted PTable and counted left and right LTable");
  }
  //according to pTable and lTable, count statistical result: TF, neighbor distribution
  public void countTFNeighbor(){
    //get TF and right neighbor
    for(int pIndex = 0; pIndex < rightPTable.size(); pIndex++){
      String phrase = rightPTable.get(pIndex);
      for(int length = 1 + rightLTable[pIndex]; length <= N && length <= phrase.length(); length++){
        String word = phrase.substring(0, length);
        TFNeighbor tfNeighbor = new TFNeighbor();
        tfNeighbor.incrementTF();
        if(phrase.length() > length)
          tfNeighbor.addToRightNeighbor(phrase.charAt(length));
        for(int lIndex = pIndex+1; lIndex < rightLTable.length; lIndex++){
          if(rightLTable[lIndex] >= length){
            tfNeighbor.incrementTF();
            String coPhrase = rightPTable.get(lIndex);
            if(coPhrase.length() > length)
              tfNeighbor.addToRightNeighbor(coPhrase.charAt(length));
          }
          else break;
        }
        wordTFNeighbor.put(word, tfNeighbor);
      }
    }
    //get left neighbor
    for(int pIndex = 0; pIndex < leftPTable.size(); pIndex++){
      String phrase = leftPTable.get(pIndex);
      for(int length = 1 + leftLTable[pIndex]; length <= N && length <= phrase.length(); length++){
        String word = reverse(phrase.substring(0, length));
        TFNeighbor tfNeighbor = wordTFNeighbor.get(word);
        if(phrase.length() > length)
          tfNeighbor.addToLeftNeighbor(phrase.charAt(length));
        for(int lIndex = pIndex + 1; lIndex < leftLTable.length; lIndex++){
          if(leftLTable[lIndex] >= length){
            String coPhrase = leftPTable.get(lIndex);
            if(coPhrase.length() > length)
              tfNeighbor.addToLeftNeighbor(coPhrase.charAt(length));
          }
          else break;
        }
      }
    }
    System.out.println("Info: [Nagao Algorithm Step 3]: having counted TF and Neighbor");
  }
  //according to wordTFNeighbor, count MI of word
  private double countMI(String word){
    if(word.length() <= 1)  return 0;
    double coProbability = wordTFNeighbor.get(word).getTF()/wordNumber;
    List<Double> mi = new ArrayList<Double>(word.length());
    for(int pos = 1; pos < word.length(); pos++){
      String leftPart = word.substring(0, pos);
      String rightPart = word.substring(pos);
      double leftProbability = wordTFNeighbor.get(leftPart).getTF()/wordNumber;
      double rightProbability = wordTFNeighbor.get(rightPart).getTF()/wordNumber;
      mi.add(coProbability/(leftProbability*rightProbability));
    }
    return Collections.min(mi);
  }
  //save TF, (left and right) neighbor number, neighbor entropy, mutual information
  public List<Map> saveTFNeighborInfoMI(List stopwordList){
      List<Map> resultList = new ArrayList();
//    try {
      //read stop words file
      Set<String> stopWords = new HashSet<>();
      stopWords.addAll(stopwordList);
      //output words TF, neighbor info, MI
      for(Map.Entry<String, TFNeighbor> entry : wordTFNeighbor.entrySet()){
        if( entry.getKey().length() <= 1 || stopWords.contains(entry.getKey()) ) continue;
        TFNeighbor tfNeighbor = entry.getValue();          
          
        int tf, leftNeighborNumber, rightNeighborNumber;
        double mi;
        tf = tfNeighbor.getTF();
        leftNeighborNumber = tfNeighbor.getLeftNeighborNumber();
        rightNeighborNumber = tfNeighbor.getRightNeighborNumber();
        mi = countMI(entry.getKey());
        if(tf > threshold[0] && leftNeighborNumber > threshold[1] && 
            rightNeighborNumber > threshold[2] && mi > threshold[3] ){
            Map oneResultMap = new HashMap();
            oneResultMap.put("term", entry.getKey());
            oneResultMap.put("tf", tf);
            oneResultMap.put("lnn", leftNeighborNumber);
            oneResultMap.put("rnn", rightNeighborNumber);
            oneResultMap.put("lne", tfNeighbor.getLeftNeighborEntropy());
            oneResultMap.put("rne", tfNeighbor.getRightNeighborEntropy());
            oneResultMap.put("mi", mi);
            resultList.add(oneResultMap);
        }
      }
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
    return resultList;
  }
  //apply nagao algorithm to input file
//  public static void applyNagao(String[] inputs, String out, String stopList){
//    NagaoAlgorithm nagao = new NagaoAlgorithm();
//    //step 1: add phrases to PTable
//    String line;
//    for(String in : inputs){
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(in));
//        while((line = br.readLine()) != null){
//          nagao.addToPTable(line);
//        }
//        br.close();
//      } catch (IOException e) {
//        throw new RuntimeException();
//      }
//    }
//    System.out.println("Info: [Nagao Algorithm Step 1]: having added all left and right substrings to PTable");
//    //step 2: sort PTable and count LTable
//    nagao.countLTable();
//    //step3: count TF and Neighbor
//    nagao.countTFNeighbor();
//    //step4: save TF NeighborInfo and MI
//    nagao.saveTFNeighborInfoMI(out, stopList, "20,3,3,5".split(","));
//  }
//  public static void applyNagao(String[] inputs, String out, String stopList, int n, String filter){
//    NagaoAlgorithm nagao = new NagaoAlgorithm();
//    nagao.setN(n);
//    String[] threshold = filter.split(",");
//    if(threshold.length != 4){
//      System.out.println("ERROR: filter must have 4 numbers, seperated with ',' ");
//      return;
//    }
//    //step 1: add phrases to PTable
//    String line;
//    for(String in : inputs){
//      try {
//        BufferedReader br = new BufferedReader(new FileReader(in));
//        while((line = br.readLine()) != null){
//          nagao.addToPTable(line);
//        }
//        br.close();
//      } catch (IOException e) {
//        throw new RuntimeException();
//      }
//    }
//    System.out.println("Info: [Nagao Algorithm Step 1]: having added all left and right substrings to PTable");
//    //step 2: sort PTable and count LTable
//    nagao.countLTable();
//    //step3: count TF and Neighbor
//    nagao.countTFNeighbor();
//    //step4: save TF NeighborInfo and MI
//    nagao.saveTFNeighborInfoMI(out, stopList, threshold);
//  }
  private void setN(int n){
    N = n;
  }
    
//  public static void main(String[] args) {
//    String[] ins = {"D://temp//wordanalysis//shenyou.txt"};
//    applyNagao(ins, "D://temp//wordanalysis//out.txt", "D://temp//wordanalysis//stopword.dic");
//  }
}

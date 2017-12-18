/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.parse;

import com.google.common.collect.Sets;
import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.storage.WebPage;
import org.apache.nutch.urlfilter.api.RegexRule;
import org.apache.nutch.urlfilter.api.RegexURLFilterBase;
import org.apache.nutch.util.NodeWalker;
import org.apache.nutch.util.PageUtils;
import org.apache.nutch.util.TimeUtils;
import org.apache.wicket.util.collections.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.w3c.dom.DocumentFragment;
//import org.w3c.dom.Node;
import org.w3c.dom.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Indexing filter that offers an option to either index all inbound anchor text
 * for a document or deduplicate anchors. Deduplication does have it's con's,
 * 
 * @see {@code anchorIndexingFilter.deduplicate} in nutch-default.xml.
 */
public class AllParsingFilter extends RegexURLFilterBase implements ParseFilter {
  public static final Logger LOG = LoggerFactory
      .getLogger(AllParsingFilter.class);
  private Configuration conf;

  private static final Collection<WebPage.Field> FIELDS = new HashSet<WebPage.Field>();

  static {
    //FIELDS.add(WebPage.Field.INLINKS);
  }
//-------------------------------------------------------------------------------------
  public AllParsingFilter() {
  super();
}

  public AllParsingFilter(String filename) throws IOException,
          PatternSyntaxException {
    super(filename);
  }

  public AllParsingFilter(Reader reader) throws IOException, IllegalArgumentException {
    super(reader);
  }
  @Override
  protected RegexRule createRule(boolean sign, String regex) {
    return new Rule(sign, regex);
  }

  @Override
  protected Reader getRulesReader(Configuration conf) throws IOException {
    String fileRules = conf.get(ALL_REGEX_FILE);
    return conf.getConfResourceAsReader(fileRules);
  }
  private class PickedNode{
    public String nodeName;
    public Map<String,Pattern> attrPattern;
  }
  private class Rule extends RegexRule {

    private Pattern pattern;
    private PickedNode timeRegex=new PickedNode();
    private PickedNode textRegex=new PickedNode();
    private PickedNode titleRegex=new PickedNode();
    private String source;
    private Map<String,Pattern> resolveAttrPattern(String s){
      Map<String,Pattern> result=new HashMap<String,Pattern>();
      String[] parts=s.split("%");
      String[] keyvalue;
      for(String part:parts){
        keyvalue=part.split(",");
        result.put(keyvalue[0].substring(keyvalue[0].indexOf("=")+1),Pattern.compile(keyvalue[1].substring(keyvalue[1].indexOf("=")+1)));
      }
      return result;
    }
    Rule(boolean sign, String regex) {
      super(sign, regex);
      System.out.println(regex);
      String[] parts=regex.split("#");
      pattern = Pattern.compile(parts[0]);
      //---------------------------------------------------------------
      String[] timeParts=parts[1].split("&");
      String[] keyvalue;
      keyvalue=timeParts[0].split("=");
      timeRegex.nodeName=keyvalue.length>1?keyvalue[1]:"";
      timeRegex.attrPattern=resolveAttrPattern(timeParts[1]);
      //----------------------------------------------------------------
      String[] textParts=parts[2].split("&");
      keyvalue=textParts[0].split("=");
      textRegex.nodeName=keyvalue.length>1?keyvalue[1]:"";
      textRegex.attrPattern=resolveAttrPattern(textParts[1]);
      //----------------------------------------------------------------
      String[] titleParts=parts[3].split("&");
      keyvalue=titleParts[0].split("=");
      titleRegex.nodeName=keyvalue.length>1?keyvalue[1]:"";
      titleRegex.attrPattern=resolveAttrPattern(titleParts[1]);
      //----------------------------------------------------------------
      source=parts[4];
    }

    public boolean match(String url) {
      return pattern.matcher(url).find();
    }
    public PickedNode getTimeRegex(){
      return timeRegex;
    }
    public PickedNode getTextRegex(){
      return textRegex;
    }
    public String getSource(){
      return source;
    }
    public PickedNode getTitleRegex(){
      return titleRegex;
    }
  }

  public static final String ALL_REGEX_FILE = "parse.regex.file";
  private List<RegexRule> readRules(Reader reader) throws IOException,
          IllegalArgumentException{
    BufferedReader in = new BufferedReader(reader);
    List<RegexRule> rules = new ArrayList<RegexRule>();
    String line;

    while ((line = in.readLine()) != null) {
      if (line.length() == 0) {
        continue;
      }
      char first = line.charAt(0);
      boolean sign = false;
      switch (first) {
        case '+':
          sign = true;
          break;
        case '-':
          sign = false;
          break;
        case ' ':
        case '\n':
        case '#': // skip blank & comment lines
          continue;
        default:
          throw new IOException("Invalid first character: " + line);
      }

      String regex = line.substring(1);
      if (LOG.isTraceEnabled()) {
        LOG.trace("Adding rule [" + regex + "]");
      }
      RegexRule rule = createRule(sign, regex);
      rules.add(rule);
    }
    return rules;

  }
  //----------------------------------------------------------------------------------

  /**
   * Set the {@link Configuration} object
   */

  /**
   * Get the {@link Configuration} object
   */
  public Configuration getConf() {
    return this.conf;
  }

  public void addIndexBackendOptions(Configuration conf) {
  }
  /**
   * The {@link AnchorIndexingFilter} filter object which supports boolean
   * configuration settings for the deduplication of anchors. See
   * {@code anchorIndexingFilter.deduplicate} in nutch-default.xml.
   * 
   * @param doc
   *          The {@link NutchDocument} object
   * @param url
   *          URL to be filtered for anchor text
   * @param page
   *          {@link WebPage} object relative to the URL
   * @return filtered NutchDocument
   */
  @Override
  public Parse filter(String url, WebPage page, Parse parse,
                      HTMLMetaTags metaTags, DocumentFragment doc){
       LOG.info("all parse filter:"+url);
       Parse newParse=null;
       Rule rule=null;
       if((rule=this.filter2(url))!=null){
         Combine combine=walk(doc,rule,url);
         newParse=new Parse(combine.text,combine.title,parse.getOutlinks(),parse.getParseStatus());
         page.getHeaders().put(new Utf8("EditTime"),new Utf8(combine.time));
         page.getHeaders().put(new Utf8("Source"),new Utf8(rule.getSource()));
         if(combine.text==null || "".equals(combine.text) || combine.title==null || "".equals(combine.title) || combine.time==null || "".equals(combine.time)) {
           final String source=rule.getSource();
           final String url2=url;
           if (sources.add(rule.getSource())) {
             new Thread(new Runnable() {
               @Override
               public void run() {
                 send("线上爬取新闻发生问题", source + "的规则发生变化,url 是:"+url2, Sets.newHashSet("591465704@qq.com"),Sets.newHashSet("591465704@qq.com"));
               }
             }).start();
           }
         }
         return newParse;
       }else{
         newParse=new Parse(parse.getText(),parse.getTitle(),parse.getOutlinks(),parse.getParseStatus());
       }
    return newParse;
  }
  public static Logger logger=LoggerFactory.getLogger(AllParsingFilter.class);
  public final static String USERNAME = "";
  public final static String PASSWORD = "";
  public static ConcurrentHashSet<String> sources=new ConcurrentHashSet<String>();
  private static String appid;
  private static String posturl;
  private static int FILESIZE = 5242880;
  private static int TOTALFILESIZE = 10485760;
  private static String from;
  private static String password;
  static{

    InputStream is=null;
    try {
      URL url =AllParsingFilter.class.getClassLoader().getResource("sendmail.properties");
      Properties p = new Properties();
      is=url.openStream();
      p.load(url.openStream());
      posturl = p.getProperty("address", "");
      String e = p.getProperty("fileSize", "-1");
      FILESIZE = Integer.valueOf(e).intValue();
      String totalsize = p.getProperty("totalFileSize", "10485760");
      appid = p.getProperty("appid", "");
      from=p.getProperty("from","");
      password=p.getProperty("password","");
      TOTALFILESIZE = Integer.valueOf(totalsize).intValue();
    } catch (Exception var13) {
      logger.warn(var13.getMessage());
    } finally {
      if(is != null) {
        try {
          is.close();
        } catch (IOException var12) {

        }
      }

    }
  }
  public static void send(String subject, String content, Set<String> to, Set<String> cc){
    try {
      MultipartContent multipartContent = new MultipartContent();
      multipartContent.setText(content);
      Subject sub = new Subject();
      sub.setText(subject);
      Reciption reciption = new Reciption();
      reciption.getTO().addAll(to);
      reciption.getCC().addAll(cc);

      DefaultHttpClient httpClient = new DefaultHttpClient();
      HttpPost httpPost = new HttpPost(posturl);
      ArrayList nameValuePairList = new ArrayList();
      BasicNameValuePair bnv_from = new BasicNameValuePair("from", from);
      nameValuePairList.add(bnv_from);
      BasicNameValuePair bnv_appid = new BasicNameValuePair("appid", appid);
      nameValuePairList.add(bnv_appid);
      BasicNameValuePair bnv_password = new BasicNameValuePair("password", password);
      nameValuePairList.add(bnv_password);
      BasicNameValuePair bnv_to = new BasicNameValuePair("to", JSON.toJSONString(reciption));
      nameValuePairList.add(bnv_to);
      BasicNameValuePair bnv_sub = new BasicNameValuePair("sub", new String(JSON.toJSONString(sub).getBytes("utf-8"), "iso8859-1"));
      nameValuePairList.add(bnv_sub);
      BasicNameValuePair bnv_mc = new BasicNameValuePair("mc", new String(JSON.toJSONString(multipartContent).getBytes("utf-8"), "iso8859-1"));
      nameValuePairList.add(bnv_mc);

      UrlEncodedFormEntity encodedFormEntity1 = new UrlEncodedFormEntity(nameValuePairList);
      httpPost.setEntity(encodedFormEntity1);
      HttpResponse execute1 = httpClient.execute(httpPost);
      HttpEntity entity = execute1.getEntity();
      BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(entity);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bufferedHttpEntity.writeTo(byteArrayOutputStream);
      byte[] responseBytes = byteArrayOutputStream.toByteArray();
      String result = new String(responseBytes);
      logger.info("send email result:"+result);
    }catch (Exception e){
      logger.error("send email error:"+e.getMessage(),e);
    }
  }
  public Rule filter2(String url) {
    for (RegexRule rule : rules) {
      if (rule.match(url)) {
        return (Rule)rule;
      }
    }
    ;
    return null;
  }
  private class Combine{
    public String text="";
    public String time="";
    public String title="";
  }
  public boolean getPickedContent(PickedNode pickedNode,Node currentNode){
    Iterator<Map.Entry<String,Pattern>> iter=pickedNode.attrPattern.entrySet().iterator();
    while(iter.hasNext()){
      Map.Entry<String,Pattern> entry=iter.next();
      if(entry.getKey().equals("") || (  currentNode.getAttributes().getNamedItem(entry.getKey())!=null &&
              entry.getValue().matcher(currentNode.getAttributes().getNamedItem(entry.getKey()).getNodeValue()).find() ) )
        return true;
    }
    return false;
  }
  private Combine walk(DocumentFragment root,Rule rule,String url){
    Combine combine=new Combine();
    NodeWalker walker=new NodeWalker(root);
    StringBuffer sb=new StringBuffer();
    while(walker.hasNext()){
      Node currentNode = walker.nextNode();
      String nodeName = currentNode.getNodeName();
      if("head".equalsIgnoreCase(nodeName))
        walker.skipChildren();
      if ("script".equalsIgnoreCase(nodeName)) {
        walker.skipChildren();
      }
      if ("style".equalsIgnoreCase(nodeName)) {
        walker.skipChildren();
      }
      if(rule.getTitleRegex().nodeName.equalsIgnoreCase(nodeName)){
        if(getPickedContent(rule.getTitleRegex(),currentNode))
          combine.title= currentNode.getTextContent().replace("\\s+", " ").trim();
      }
      if(rule.getTimeRegex().nodeName.equalsIgnoreCase(nodeName)){
        if(getPickedContent(rule.getTimeRegex(),currentNode))
          combine.time= String.valueOf(TimeUtils.convert2time(currentNode.getTextContent().replace("\\s+", " ").trim()));
      }
      if(rule.getTextRegex().nodeName.equalsIgnoreCase(nodeName)){
        if(getPickedContent(rule.getTextRegex(),currentNode)){
          combine.text= PageUtils.pasteAll(currentNode,url);
          break;
        }
      }
    }
    return combine;
  }

  /**
   * Gets all the fields for a given {@link WebPage} Many datastores need to
   * setup the mapreduce job by specifying the fields needed. All extensions
   * that work on WebPage are able to specify what fields they need.
   */
  @Override
  public Collection<WebPage.Field> getFields() {
    return FIELDS;
  }

}

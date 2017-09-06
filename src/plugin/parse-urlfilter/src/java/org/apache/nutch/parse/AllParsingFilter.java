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

import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.storage.WebPage;
import org.apache.nutch.urlfilter.api.RegexRule;
import org.apache.nutch.urlfilter.api.RegexURLFilterBase;
import org.apache.nutch.util.NodeWalker;
import org.apache.nutch.util.PageUtils;
import org.apache.nutch.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.w3c.dom.DocumentFragment;
//import org.w3c.dom.Node;
import org.w3c.dom.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
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
  private class Rule extends RegexRule {

    private Pattern pattern;
    private Map<String,String> timeRegex=new HashMap<String,String>();
    private Map<String,String> textRegex=new HashMap<String,String>();
    private Map<String,String> titleRegex=new HashMap<String,String>();
    private String source;
    Rule(boolean sign, String regex) {
      super(sign, regex);
      String[] parts=regex.split("#");
      pattern = Pattern.compile(parts[0]);
      //---------------------------------------------------------------
      String[] timeParts=parts[1].split("&");
      for(int i=0;i<timeParts.length;i++){
        String[] keyValue=timeParts[i].split("=");
        timeRegex.put(keyValue[0],keyValue.length>1?keyValue[1]:"");
      }
      //----------------------------------------------------------------
      String[] textParts=parts[2].split("&");
      for(int i=0;i<textParts.length;i++){
        String[] keyValue=textParts[i].split("=");
        textRegex.put(keyValue[0],keyValue.length>1?keyValue[1]:"");
      }
      //----------------------------------------------------------------
      String[] titleParts=parts[3].split("&");
      for(int i=0;i<titleParts.length;i++){
        String[] keyValue=titleParts[i].split("=");
        titleRegex.put(keyValue[0],keyValue.length>1?keyValue[1]:"");
      }
      //----------------------------------------------------------------
      source=parts[4];
    }

    public boolean match(String url) {
      return pattern.matcher(url).find();
    }
    public Map<String,String> getTimeRegex(){
      return timeRegex;
    }
    public Map<String,String> getTextRegex(){
      return textRegex;
    }
    public String getSource(){
      return source;
    }
    public Map<String,String> getTitleRegex(){
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
         return newParse;
       }else{
         newParse=new Parse(parse.getText(),parse.getTitle(),parse.getOutlinks(),parse.getParseStatus());
       }
    return newParse;
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
      if(rule.getTitleRegex().get("nodeName").equalsIgnoreCase(nodeName)){
        if(rule.getTitleRegex().get("attribute").equals("")){
          combine.title= currentNode.getTextContent().replace("\\s+", " ").trim();
        }else  if(currentNode.getAttributes().getNamedItem(rule.getTitleRegex().get("attribute"))!=null && currentNode.getAttributes().getNamedItem(rule.getTitleRegex().get("attribute")).getNodeValue().equalsIgnoreCase(rule.getTitleRegex().get("value"))){
          combine.title= currentNode.getTextContent().replace("\\s+", " ").trim();
        }
      }
      if(rule.getTimeRegex().get("nodeName").equalsIgnoreCase(nodeName)){
        if(rule.getTimeRegex().get("attribute").equals("") || (currentNode.getAttributes().getNamedItem(rule.getTimeRegex().get("attribute"))!=null && currentNode.getAttributes().getNamedItem(rule.getTimeRegex().get("attribute")).getNodeValue().equalsIgnoreCase(rule.getTimeRegex().get("value")))){
          combine.time= String.valueOf(TimeUtils.convert2time(currentNode.getTextContent().replace("\\s+", " ").trim()));
        }
      }
      if(rule.getTextRegex().get("nodeName").equalsIgnoreCase(nodeName)){
        if(rule.getTextRegex().get("attribute").equals("") || (currentNode.getAttributes().getNamedItem(rule.getTextRegex().get("attribute"))!=null && currentNode.getAttributes().getNamedItem(rule.getTextRegex().get("attribute")).getNodeValue().equalsIgnoreCase(rule.getTextRegex().get("value")))){
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

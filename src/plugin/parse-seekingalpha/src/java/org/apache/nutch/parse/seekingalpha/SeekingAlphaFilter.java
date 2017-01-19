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
package org.apache.nutch.parse.seekingalpha;

import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseFilter;
import org.apache.nutch.parse.Parser;
import org.apache.nutch.storage.WebPage;
import org.apache.nutch.urlfilter.api.RegexRule;
import org.apache.nutch.urlfilter.api.RegexURLFilterBase;
import org.apache.nutch.util.NodeWalker;
import org.apache.nutch.util.PageUtils;
import org.apache.nutch.util.TableUtil;
import org.apache.nutch.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.w3c.dom.DocumentFragment;
//import org.w3c.dom.Node;
import org.w3c.dom.*;

import java.io.IOException;
import java.io.Reader;
import java.lang.CharSequence;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Indexing filter that offers an option to either index all inbound anchor text
 * for a document or deduplicate anchors. Deduplication does have it's con's,
 * 
 * @see {@code anchorIndexingFilter.deduplicate} in nutch-default.xml.
 */
public class SeekingAlphaFilter extends RegexURLFilterBase implements ParseFilter {
  public static final Logger LOG = LoggerFactory
      .getLogger(SeekingAlphaFilter.class);
  private Configuration conf;

  private static final Collection<WebPage.Field> FIELDS = new HashSet<WebPage.Field>();

  static {
    //FIELDS.add(WebPage.Field.INLINKS);
  }
//-------------------------------------------------------------------------------------
public SeekingAlphaFilter() {
  super();
}

  public SeekingAlphaFilter(String filename) throws IOException,
          PatternSyntaxException {
    super(filename);
  }

  SeekingAlphaFilter(Reader reader) throws IOException, IllegalArgumentException {
    super(reader);
  }
  @Override
  protected RegexRule createRule(boolean sign, String regex) {
    return new Rule(sign, regex);
  }

  @Override
  protected Reader getRulesReader(Configuration conf) throws IOException {
    String fileRules = conf.get(SEEKINGALPHA_REGEX_FILE);
    return conf.getConfResourceAsReader(fileRules);
  }
  private class Rule extends RegexRule {

    private Pattern pattern;

    Rule(boolean sign, String regex) {
      super(sign, regex);
      pattern = Pattern.compile(regex);
    }

    public boolean match(String url) {
      return pattern.matcher(url).find();
    }
  }
  public static final String SEEKINGALPHA_REGEX_FILE = "seekingalphaindexingfilter.regex.file";
  //----------------------------------------------------------------------------------

  /**
   * Set the {@link Configuration} object
   */
  //RegexURLFilterBase已经帮我们实现了这个玩意
//  public void setConf(Configuration conf) {
//    this.conf = conf;
//  }

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
       LOG.info("urlparsing filter:"+url);
       Parse newParse=null;
       if(this.filter(url)!=null){
         Combine combine=walk(doc);
         newParse=new Parse(combine.text,parse.getTitle(),parse.getOutlinks(),parse.getParseStatus());
         page.getHeaders().put(new Utf8("EditTime"),new Utf8(combine.time));
         page.getHeaders().put(new Utf8("Source"),new Utf8("Seeking Alpha"));
         return newParse;
       }else{
         newParse=new Parse(parse.getText(),parse.getTitle(),parse.getOutlinks(),parse.getParseStatus());
       }
    return newParse;
  }
  private class Combine{
    public String text="";
    public String time="";
  }
      private Combine walk(DocumentFragment root){
        Combine combine=new Combine();
        NodeWalker walker=new NodeWalker(root);
        while(walker.hasNext()){
          Node currentNode = walker.nextNode();
          String nodeName = currentNode.getNodeName();
          short nodeType = currentNode.getNodeType();
          if("head".equalsIgnoreCase(nodeName))
            walker.skipChildren();
          if ("script".equalsIgnoreCase(nodeName)) {
            walker.skipChildren();
          }
          if ("style".equalsIgnoreCase(nodeName)) {
            walker.skipChildren();
          }

          if("div".equalsIgnoreCase(nodeName) && currentNode.getAttributes().getNamedItem("itemprop") !=null
                  && currentNode.getAttributes().getNamedItem("itemprop").getNodeValue().equalsIgnoreCase("datePublished")){
            combine.time=currentNode.getAttributes().getNamedItem("content").getNodeValue();
          }
          if("time".equalsIgnoreCase(nodeName) && currentNode.getAttributes().getNamedItem("itemprop") !=null
                  && currentNode.getAttributes().getNamedItem("itemprop").getNodeValue().equalsIgnoreCase("datePublished")){
            combine.time= String.valueOf(TimeUtils.convert2time(currentNode.getAttributes().getNamedItem("content").getNodeValue()));
          }
          if("div".equalsIgnoreCase(nodeName) && currentNode.getAttributes().getNamedItem("itemprop") !=null
                  && currentNode.getAttributes().getNamedItem("itemprop").getNodeValue().equalsIgnoreCase("articleBody")){

//            NodeList nodeList=currentNode.getChildNodes();
//            for(int i=0;i<nodeList.getLength();i++){
//              Node childNode=nodeList.item(i);
//
//              if("DIV".equalsIgnoreCase(childNode.getNodeName())){
//                if("bullets_ul".equalsIgnoreCase(childNode.getAttributes().getNamedItem("id").getNodeValue())){
//                  // System.out.println("child node: "+childNode.getNodeName());
//                  // System.out.println("child1: "+childNode.getTextContent());
//
//                  NodeList pList = childNode.getChildNodes();
//                  for(int j=0;j<pList.getLength();j++){
//                    Node p =pList.item(j);
//                    if(!"#text".equalsIgnoreCase(p.getNodeName()) && !"A".equalsIgnoreCase(p.getFirstChild().getNodeName())){
//                      //System.out.println("pNode node: "+p.getNodeName());
//                      // System.out.println("pNode text: "+p.getTextContent());
//                      // System.out.println("pNode child length:"+p.getFirstChild().getNodeName());
//                      combine.text+=p.getTextContent()+"\r\n";
//                    }
//
//                  }
//
//                }
//
//              }
//
//              // System.out.println("child1: "+childNode.getTextContent().length());
//            }
            combine.text= PageUtils.pasteAll(currentNode);
            break;
          }

        }
        return combine;

  }
//  @Override
//  public NutchDocument filter(NutchDocument doc, String url, WebPage page)
//      throws IndexingException {
//    LOG.info("urlindexingfilter:"+url);
//    if(this.filter(url)!=null) {  //接受url
//      LOG.info("accept url");
//      return doc;
//
//    }
//    else {                       //否则拒绝这个url
//      LOG.info("refuse url");
//      return null;
//    }
//  }

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

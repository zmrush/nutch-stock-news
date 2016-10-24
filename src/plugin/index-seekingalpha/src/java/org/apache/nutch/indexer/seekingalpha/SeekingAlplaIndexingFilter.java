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
package org.apache.nutch.indexer.seekingalpha;

import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.storage.WebPage;
import org.apache.nutch.urlfilter.api.RegexRule;
import org.apache.nutch.urlfilter.api.RegexURLFilterBase;
import org.apache.nutch.util.TableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SeekingAlplaIndexingFilter extends RegexURLFilterBase implements IndexingFilter {

  public static final Logger LOG = LoggerFactory
      .getLogger(SeekingAlplaIndexingFilter.class);
  private Configuration conf;

  private static final Collection<WebPage.Field> FIELDS = new HashSet<WebPage.Field>();

  static {
    FIELDS.add(WebPage.Field.HEADERS);
  }
//-------------------------------------------------------------------------------------
public SeekingAlplaIndexingFilter() {
  super();
}

  public SeekingAlplaIndexingFilter(String filename) throws IOException,
          PatternSyntaxException {
    super(filename);
  }

  SeekingAlplaIndexingFilter(Reader reader) throws IOException, IllegalArgumentException {
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

    protected boolean match(String url) {
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
  public NutchDocument filter(NutchDocument doc, String url, WebPage page)
      throws IndexingException {
    LOG.info("urlindexingfilter:"+url);
    if(this.filter(url)!=null) {  //接受url
      LOG.info("accept url");
      doc.add("source",page.getHeaders().get(new Utf8("Source")).toString());
      //doc.removeField("tstamp");
      //不同的来源可能时间格式不一样，因此需要详细设计
      doc.add("estamp",page.getHeaders().get(new Utf8("EditTime")).toString());
      return doc;

    }
    else {                       //否则拒绝这个url
      LOG.info("refuse url");
      return null;
    }
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
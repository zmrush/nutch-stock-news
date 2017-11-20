package org.apache.nutch.crawl;

import org.apache.avro.util.Utf8;
import org.apache.gora.filter.FilterOp;
import org.apache.gora.filter.MapFieldValueFilter;
import org.apache.gora.filter.SingleFieldValueFilter;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.storage.Mark;
import org.apache.nutch.storage.StorageUtils;
import org.apache.nutch.storage.WebPage;
import org.apache.nutch.util.NutchConfiguration;
import org.apache.nutch.util.NutchTool;

import java.util.Map;

/**
 * Created by mingzhu7 on 2016/10/21.
 */
public class CleanJob extends NutchTool implements Tool {
    private final static String MAGIC_NUMBER="197328";
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(NutchConfiguration.create(), new CleanJob(),
                args);
        System.exit(res);
    }

    @Override
    public Map<String, Object> run(Map<String, Object> args) throws Exception {
        return null;
    }

    public int run(String[] args) throws Exception {
        //getConf().addResource("hbase-site.xml");
        if (args.length < 2) {
            System.err.println("Usage: CleanJob [-crawlId <id>]");
            return -1;
        }
        for (int i = 0; i < args.length; i++) {
            if ("-crawlId".equals(args[i])) {
                getConf().set(Nutch.CRAWL_ID_KEY, args[i + 1]);
                i++;
            } else {
                System.err.println("Unrecognized arg " + args[i]);
                return -1;
            }
        }
        DataStore<String, WebPage> store=null;
        try {
            store = StorageUtils.createWebStore(getConf(), String.class, WebPage.class);
            Query<String, WebPage> query = store.newQuery();
//            MapFieldValueFilter<String, WebPage> filter = new MapFieldValueFilter<String, WebPage>();
//            filter.setFieldName(WebPage.Field.MARKERS.toString());
//            filter.setMapKey(Mark.INDEX_MARK.getName());
//            filter.setFilterIfMissing(false);
//            filter.setFilterOp(FilterOp.NOT_EQUALS);
//            filter.getOperands().add(new Utf8(MAGIC_NUMBER));
            //----------------------------------------------------------------
            SingleFieldValueFilter<String,WebPage> filter=new SingleFieldValueFilter<String, WebPage>();
            filter.setFieldName(WebPage.Field.PREV_FETCH_TIME.toString());
            filter.setFilterOp(FilterOp.LESS);
            filter.setFilterIfMissing(false);
            long now=System.currentTimeMillis();
            now=now-12*3600*1000;
            filter.getOperands().add(now);
            //----------------------------------------------------------------
            query.setFilter(filter);
            query.setFields(WebPage._ALL_FIELDS);
            store.deleteByQuery(query);
            store.close();
            store=null;
        } catch (Throwable e) {
            throw new RuntimeException("clean job encounters problems",e);
        }
//        Thread.currentThread().sleep(60000);
//        try {
//            store = StorageUtils.createWebStore(getConf(), String.class, WebPage.class);
//            Query<String, WebPage> query = store.newQuery();
//            MapFieldValueFilter<String, WebPage> filter = new MapFieldValueFilter<String, WebPage>();
//            filter.setFieldName(WebPage.Field.MARKERS.toString());
//            filter.setMapKey(Mark.INDEX_MARK.getName());
//            filter.setFilterIfMissing(false);
//            filter.setFilterOp(FilterOp.NOT_EQUALS);
//            filter.getOperands().add(new Utf8(MAGIC_NUMBER));
//            query.setFilter(filter);
//            query.setFields(WebPage._ALL_FIELDS);
//            store.deleteByQuery(query);
//            store.close();
//        } catch (Throwable e) {
//            throw new RuntimeException("clean job encounters problems",e);
//        }
        finally{
            System.out.println("clean job done");
        }
        return 0;
    }
}

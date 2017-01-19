package org.apache.nutch.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mingzhu7 on 2016/10/26.
 */
public class TimeUtils {
    static final Pattern pattern1=Pattern.compile("[0-9]{4}年[0-9]{2}月[0-9]{2}日[0-9]{2}:[0-9]{2}");
    static final Pattern pattern2=Pattern.compile("[0-9]{4}年[0-9]{2}月[0-9]{2}日 [0-9]{2}:[0-9]{2}");
    static final Pattern pattern3=Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}");
    static final Pattern pattern4=Pattern.compile("[a-zA-Z]{3}. [0-9]{2}, [0-9]{4} [0-9]{1,2}:[0-9]{2} (A|P)M ET");
    static final Pattern pattern5=Pattern.compile("[0-9]{2}/[0-9]{2}/[0-9]{4}");
    static final Pattern pattern6=Pattern.compile("[0-9]{1,2}:[0-9]{2} (A|P)M ET");
    static final Pattern pattern7=Pattern.compile("[a-zA-Z]{3} [0-9]{2}, [0-9]{4} [0-9]{1,2}:[0-9]{2} (a|p).m. ET");
    static final Pattern pattern8=Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}");
    static final Pattern pattern9=Pattern.compile("[0-9]{4}.[0-9]{2}.[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}");
    static final Pattern pattern10=Pattern.compile("[0-9]{4}年 [0-9]{2}月 [0-9]{1,2}日.*[0-9]{2}:[0-9]{2} BJT");
    public static long convert2time(String input){
        Matcher matcher=null;
        try {
            if ((matcher=pattern1.matcher(input)).find()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
                Date date = simpleDateFormat.parse(matcher.group(0));
                return date.getTime();
            } else if ((matcher=pattern2.matcher(input)).find()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                Date date = simpleDateFormat.parse(matcher.group(0));
                return date.getTime();
            } else if((matcher=pattern3.matcher(input)).find()){
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = simpleDateFormat.parse(matcher.group(0));
                return date.getTime();
            }else if((matcher=pattern4.matcher(input)).find()){
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("MMM. dd, yyyy h:mm a z", Locale.ENGLISH);
                input=matcher.group(0).replace("ET","EDT");
                Date date=simpleDateFormat.parse(input);
                return date.getTime();
            }else if((matcher=pattern5.matcher(input)).find()){
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("MM/dd/yyyy");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT-04:00"));
                Date date=simpleDateFormat.parse(matcher.group(0));
                return date.getTime();
            }else if((matcher=pattern6.matcher(input)).find()){
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("MMM. dd, yyyy h:mm a z", Locale.ENGLISH);
                input=matcher.group(0).replace("ET","EDT");

                SimpleDateFormat simpleDateFormat2=new SimpleDateFormat("MMM. dd, yyyy ", Locale.ENGLISH);
                simpleDateFormat2.setTimeZone(TimeZone.getTimeZone("GMT-04:00"));
                String s=simpleDateFormat2.format(new Date());

                Date date=simpleDateFormat.parse(s+input);
                //我们假设不会晚于一天爬取到新闻
                return date.getTime()>System.currentTimeMillis()?date.getTime()-24*60*60*1000:date.getTime();
            }else if((matcher=pattern7.matcher(input)).find()){
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("MMM dd, yyyy h:mm a z", Locale.ENGLISH);
                input=matcher.group(0).replace("ET","EDT");
                input=input.replace("a.m.","AM");
                input=input.replace("p.m.","PM");
                Date date=simpleDateFormat.parse(input);
                return date.getTime();
            }else if((matcher=pattern8.matcher(input)).find()){
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm");
                return simpleDateFormat.parse(matcher.group(0)).getTime();
            }else if((matcher=pattern9.matcher(input)).find()){
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                Date date = simpleDateFormat.parse(matcher.group(0));
                return date.getTime();
            }else if((matcher=pattern10.matcher(input)).find()){
                String[] tmp=matcher.group(0).split(" ");
                StringBuffer sb=new StringBuffer();
                sb.append(tmp[0]);
                sb.append(tmp[1].length()<=2?("0"+tmp[1]):tmp[1]);
                sb.append(tmp[2].length()<=2?("0"+tmp[2]):tmp[2]);
                sb.append(tmp[4]);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
                Date date=simpleDateFormat.parse(sb.toString());
                return date.getTime();

            }
            else {
                return new Date().getTime();
            }
        }catch (Throwable throwable){
            return new Date().getTime();
        }
    }
}

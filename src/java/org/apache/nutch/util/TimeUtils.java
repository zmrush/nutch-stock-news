package org.apache.nutch.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Created by mingzhu7 on 2016/10/26.
 */
public class TimeUtils {
    static final Pattern pattern1=Pattern.compile("^[0-9]{4}年[0-9]{2}月[0-9]{2}日[0-9]{2}:[0-9]{2}$");
    static final Pattern pattern2=Pattern.compile("^[0-9]{4}年[0-9]{2}月[0-9]{2}日 [0-9]{2}:[0-9]{2}$");
    static final Pattern pattern3=Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$");
    static final Pattern pattern4=Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$");
    static final Pattern pattern5=Pattern.compile("^[0-9]{2}/[0-9]{2}/[0-9]{4}$");
    public static long convert2time(String input){
        try {
            if (pattern1.matcher(input).find()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
                Date date = simpleDateFormat.parse(input);
                return date.getTime();
            } else if (pattern2.matcher(input).find()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                Date date = simpleDateFormat.parse(input);
                return date.getTime();
            } else if(pattern3.matcher(input).find()){
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = simpleDateFormat.parse(input);
                return date.getTime();
            }else if(pattern4.matcher(input).find()){
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date=simpleDateFormat.parse(input);
                return date.getTime();
            }else if(pattern5.matcher(input).find()){
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("MM/dd/yyyy");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
                Date date=simpleDateFormat.parse(input);
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

package com.linzi.httpmanager.tool;

/**
 * Created by jiang on 2016/11/30.
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
    //字符串转时间戳
    public static String getTime(String timeString){
        String timeStamp = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        Date d;
        try{
            d = sdf.parse(timeString);
            long l = d.getTime();
            timeStamp = String.valueOf(l);
        } catch(ParseException e){
            e.printStackTrace();
        }
        return timeStamp;
    }

    //时间戳转字符串
    public static String getStrTime(String timeStamp){
        String timeString = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 hh:mm:ss");
        long  l = Long.valueOf(timeStamp);
        timeString = sdf.format(l*1000);//单位秒
        return timeString;
    }

    //时间戳转字符串
    public static String getStr2Time(String timeStamp){
        String timeString = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        long  l = Long.valueOf(timeStamp);
        timeString = sdf.format(l*1000);//单位秒
        return timeString;
    }
    //时间戳转字符串
    public static String getStrTime1(String timeStamp){
        String timeString = null;
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        long  l = Long.valueOf(timeStamp);
        timeString = sdf.format(l*1000);//单位秒
        return timeString;
    }
}
package ranking.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by haoxy on 2018/7/26.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 */
public class StringUtil {

    public static String showTime(Date date) {
        if (date == null) {
            return null;
        } else {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
            c.set(11, 0);
            c.set(12, 0);
            c.set(13, 0);
            SimpleDateFormat df;
            if (date.getTime() > c.getTimeInMillis()) {
                float diffSecond = (float) ((System.currentTimeMillis() - date.getTime()) / 1000L);
                if (diffSecond <= 3.0F) {
                    return "3秒前";
                }

                if (diffSecond <= 10.0F) {
                    return "10秒前";
                }

                if (diffSecond <= 50.0F) {
                    return (int) Math.ceil((double) (diffSecond / 10.0F)) * 10 + "秒前";
                }

                if (diffSecond < 3600.0F) {
                    return (int) Math.ceil((double) (diffSecond / 60.0F)) + "分钟前";
                }

                if (diffSecond < 86400.0F) {
                    df = new SimpleDateFormat("HH:mm");
                    return "今天" + df.format(date);
                }
            }

            c.add(5, -1);
            if (date.getTime() > c.getTimeInMillis()) {
                df = new SimpleDateFormat("HH:mm");
                return "昨天" + df.format(date);
            } else {
                if (c.get(1) == 1900 + date.getYear()) {
                    df = new SimpleDateFormat("MM-dd HH:mm");
                } else {
                    df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                }
                return df.format(date);
            }
        }
    }
}

package com.ebay.dss.zds.common;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.dss.zds.exception.ToolSetCheckException;

/**
 * Created by zhouhuang on 2018年5月14日
 */
public class DateUtil {

  private final static Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

  public static final String TS0 = "yyyy-MM-dd HH:mm:ss";

  public static final String DT = "yyyy-MM-dd";

  // Get Current timestamp value with format 'yyyy-MM-dd HH:mm:ss'
  public static String currentTimeStamp(String... formats) {
    return addHour(0, formats);
  }

  // Get Current date with format 'yyyy-MM-dd'
  public static String currentDate(String... formats) {
    return addHour(0, formats);
  }

  public static String addDay(int days, String... formats) {
    String format = formats.length > 0 ? formats[0] : DT;
    return LocalDateTime.now().plusDays(days).format(DateTimeFormatter.ofPattern(format));
  }

  public static String addHour(int hours, String... formats) {
    String format = formats.length > 0 ? formats[0] : TS0;
    return LocalDateTime.now().plus(hours, ChronoUnit.HOURS).format(DateTimeFormatter.ofPattern(format));
  }

  public static LocalDate parseStringToDate(String time, String format) {
    DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
    return LocalDate.parse(time, df);
  }

  public static String convertDateToString(Date date, String... formats) {
    String format = formats.length > 0 ? formats[0] : TS0;
    ZoneId zoneId = ZoneId.systemDefault();
    LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), zoneId);
    return localDateTime.format(DateTimeFormatter.ofPattern(format));
  }

  public static Date getCronDate(String crontab, Date lastRun) {
    CronExpression expression;
    try {
      expression = new CronExpression(crontab);
      LOGGER.info(TimeZone.getDefault().getDisplayName() + " : " + TimeZone.getDefault().getID());
      expression.setTimeZone(TimeZone.getDefault());
      LOGGER.info(expression.getTimeZone().getDisplayName() + " : " + expression.getTimeZone().getID());
      return expression.getNextValidTimeAfter(lastRun);
    } catch (ParseException e) {
      LOGGER.error("fail to parse cron express", e);
      throw new ToolSetCheckException("Crontab is not availabe!");
    }
  }

  public static Boolean compareCrontab(String crontab, Date curTime, Date lastRun) {
    Date nextValidTime = getCronDate(crontab, lastRun);
    if (nextValidTime.before(curTime)) {
      return true;
    } else {
      return curTime.toInstant().truncatedTo(ChronoUnit.MINUTES)
          .compareTo(nextValidTime.toInstant().truncatedTo(ChronoUnit.MINUTES)) == 0 ? true : false;
    }
  }

  public static ZonedDateTime nowOfUtc() {
    return ZonedDateTime.now(ZoneId.of("UTC"));
  }


}

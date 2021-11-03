package com.ebay.dss.zds.common;

import com.cronutils.Function;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.SingleCron;
import com.cronutils.model.definition.CronConstraintsFactory;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.Always;
import com.cronutils.model.field.expression.QuestionMark;
import com.cronutils.parser.CronParser;
import com.ebay.dss.zds.exception.ScheduleException;
import com.ebay.dss.zds.model.schedule.ScheduleTime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static com.cronutils.model.field.expression.FieldExpression.questionMark;

/**
 * Created by zhouhuang on 2018年10月30日
 */
public class ScheduleUtil {

  enum ScheduleType {
    MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY
  }

  private static void setDefaultMinuteAndHour(ScheduleTime scheduleTime
      , StringBuffer cronExp) {
    cronExp.append(Objects.isNull(scheduleTime.getMinute()) ?
        "0" : scheduleTime.getMinute()).append(" ");
    cronExp.append(Objects.isNull(scheduleTime.getHour()) ?
        "0" : scheduleTime.getHour()).append(" ");
  }

  public static String createCronExpression(ScheduleTime scheduleTime) {
    if (null == scheduleTime.getJobType()) {
      throw new ScheduleException("Schedule Mode is not configured");
    }
    StringBuffer cronExp = new StringBuffer("0 ");
    switch (ScheduleType.valueOf(scheduleTime.getJobType())) {
      case MINUTELY:
        cronExp.append("*").append(Objects.isNull(scheduleTime.getMinute()) ?
            " " : "/" + scheduleTime.getMinute() + " ");
        cronExp.append("* ");
        cronExp.append("* ");
        cronExp.append("* ");
        cronExp.append("?");
        break;
      case HOURLY:
        cronExp.append(Objects.isNull(scheduleTime.getMinute()) ?
            "0" : scheduleTime.getMinute()).append(" ");
        cronExp.append("*").append(Objects.isNull(scheduleTime.getHour()) ?
            " " : "/" + scheduleTime.getHour() + " ");
        cronExp.append("* ");
        cronExp.append("* ");
        cronExp.append("?");
        break;
      case DAILY:
        setDefaultMinuteAndHour(scheduleTime, cronExp);
        cronExp.append("* ");
        cronExp.append("* ");
        cronExp.append("?");
        break;
      case WEEKLY:
        setDefaultMinuteAndHour(scheduleTime, cronExp);
        cronExp.append("? ");
        cronExp.append("* ");
        cronExp.append(scheduleTime.getDayOfWeeks());
        break;
      case MONTHLY:
        setDefaultMinuteAndHour(scheduleTime, cronExp);
        cronExp.append(scheduleTime.getDayOfMonths()).append(" ");
        cronExp.append("* ");
        cronExp.append("?");
        break;
      default:
        throw new ScheduleException(scheduleTime.getJobType() + " is not recognized");
    }
    return cronExp.toString();
  }

  public static ScheduleTime parseScheduleTime(String cronExp) {
    ScheduleTime scheduleTime = new ScheduleTime();
    String[] timeMark = cronExp.split(" ");
    if (timeMark.length < 5) {
      return null;
    }
    boolean isMonthly = false;
    if (timeMark[4].equals("*")) {
      if (timeMark.length == 5) {
        isMonthly = true;
      } else if (timeMark.length >= 6) {
        isMonthly = timeMark[5].equals("?") || timeMark[5].equals("*");
      }
    }
    scheduleTime.setMinute(Integer.parseInt(timeMark[1]));
    // is Hourly
    if (timeMark[2].equals("*")) {
      scheduleTime.setJobType("HOURLY");
    }
    // is Daily
    else if (timeMark[3].equals("*")) {
      scheduleTime.setJobType("DAILY");
      scheduleTime.setHour(Integer.parseInt(timeMark[2]));
    }
    // is Monthly
    else if (isMonthly) {
      scheduleTime.setJobType("MONTHLY");
      scheduleTime.setHour(Integer.parseInt(timeMark[2]));
      scheduleTime.setDayOfMonths(Integer.parseInt(timeMark[3]));
    }
    // is Weekly
    else if (timeMark.length >= 6 && !timeMark[5].equals("?") && !timeMark[5].equals("*")) {
      scheduleTime.setJobType("WEEKLY");
      scheduleTime.setHour(Integer.parseInt(timeMark[2]));
      scheduleTime.setDayOfWeeks(Integer.parseInt(timeMark[5]));
    }
    return scheduleTime;

  }

  private static Function<Cron, Cron> setQuestionMark() {
    return cron -> {
      final CronField dow = cron.retrieve(CronFieldName.DAY_OF_WEEK);
      final CronField dom = cron.retrieve(CronFieldName.DAY_OF_MONTH);
      if (dow == null && dom == null) {
        return cron;
      }
      if (dow.getExpression() instanceof QuestionMark || dom.getExpression() instanceof QuestionMark) {
        return cron;
      }
      final Map<CronFieldName, CronField> fields = new EnumMap<>(CronFieldName.class);
      fields.putAll(cron.retrieveFieldsAsMap());
      if (dow.getExpression() instanceof Always) {
        fields.put(CronFieldName.DAY_OF_WEEK,
            new CronField(CronFieldName.DAY_OF_WEEK, questionMark(), fields.get(CronFieldName.DAY_OF_WEEK).getConstraints()));
      } else {
        if (dom.getExpression() instanceof Always) {
          fields.put(CronFieldName.DAY_OF_MONTH,
              new CronField(CronFieldName.DAY_OF_MONTH, questionMark(), fields.get(CronFieldName.DAY_OF_MONTH).getConstraints()));
        } else {
          cron.validate();
        }
      }
      return new SingleCron(cron.getCronDefinition(), new ArrayList<>(fields.values()));
    };
  }

  public static String unixCronToQuartzCron(String unixCronExp) {
    CronDefinition unixDef = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    CronDefinition quartzDef = CronDefinitionBuilder.defineCron()
        .withSeconds().and()
        .withMinutes().and()
        .withHours().and()
        .withDayOfMonth().withValidRange(1, 32).supportsL().supportsW().supportsLW().supportsQuestionMark().and()
        .withMonth().withValidRange(1, 13).and()
        // monday value should be 1
        .withDayOfWeek().withValidRange(1, 7).withMondayDoWValue(1).supportsHash().supportsL().supportsQuestionMark().and()
        .withCronValidation(CronConstraintsFactory.ensureEitherDayOfWeekOrDayOfMonth()).instance();

    CronParser parser = new CronParser(unixDef);

    Cron unixCron = parser.parse(unixCronExp);
    CronMapper cronMapper;
    cronMapper = new CronMapper(unixDef, quartzDef, setQuestionMark());

    Cron quartzCron = cronMapper.map(unixCron);
    return quartzCron.asString();
  }

}

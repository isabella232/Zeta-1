package com.ebay.dss.zds.service;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ZetaEmailRepository;
import com.ebay.dss.zds.model.*;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import com.ebay.dss.zds.model.ZetaToolKitsJobEmailTemplate.TemplateStatus;

import static com.ebay.dss.zds.model.ZetaToolKitsJobEmailTemplate.TemplateStatus.*;
import static com.ebay.dss.zds.service.MailService.ZetaEmailTemplateInfo.*;


@Service
public class MailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

  @Resource(name = "error-handle-rest-template")
  private RestTemplate restTemplate;

  @Autowired
  private ZetaEmailRepository zetaEmailRepository;

  @Value("${email.host.url}")
  private String SEND_EMAIL_API;

  @Value("${email.from.addr}")
  private void setFromAddr(String fromAddr) {
    FROM_ADDR = fromAddr;
  }

  public MailService setMailAPI(String SEND_EMAIL_API) {
    this.SEND_EMAIL_API = SEND_EMAIL_API;
    return this;
  }

  @Value("${zeta.host.url}")
  private void setServiceUrl(String hostURL) {
    DATA_VALIDATE_URL = hostURL + "dataValidation";
    DATA_MOVE_URL = hostURL + "dataMove";
    META_TABLE_URL = hostURL + "zeta-sheet";
    SCHEDULE_URL = hostURL + "schedule";
    SCHEDULE_JOB_URL = hostURL + "scheduleDetail/%s";
  }

  // tool kits
  private static final String TOOL_KITS_SUBJECT =
      "[%s]-[%s]-%s Job %s";
  private static final String DATA_MOVE_CONTENT =
      "You received this email since your DataMove job of table %s is finished, " +
          "result is %s. Please <a href=\"%s\">click</a> here to check details.";
  private static final String DATA_VALIDATE_CONTENT =
      "You received this email since your DataValidate job of table %s is finished, " +
          "result is %s. Please <a href=\"%s\">click</a> here to check details.";
  private static final String NB_SCHEDULER_CONTENT =
      "You received this email since your scheduled job [%s] is %s. " +
          "Please <a href=\"%s\">click</a> here to check details.{adjunct}";
  // schedule alert
  private static final String NB_SCHEDULER_ALERT_SUBJECT =
      "[Alert]-[%s]-SCHEDULER Job Start";
  private static final String NB_SCHEDULER_ALERT_CONTENT =
      "You received this email since your scheduled job %s have been skipped " +
          "to wait dependency table. The scheduled job have been started." +
          " Please <a href=\"%s\">click</a> here to check details.";
  // vmd override
  private static final String VDM_MOVE_OVERRIDE_SUBJECT =
      "[Alert] VDM Data Move Job Overwrite Confirmation";
  private static final String VDM_MOVE_OVERRIDE_CONTENT =
      "<p>Here’s the confirmation email of your data move task.</p>\n" +
          "<p>You have confirmed that below list of HDM tables on Hermes LVS will be overwritten by data from Hermes RNO.</p>\n" +
          "<p><font color = \"red\"><b>Reminder: </b></font>" +
          "<b>Please be aware that the overwritten data cannot be recovered.</b></p> \n" +
          "<br>\n" +
          "<table style=\"width:100%; color: #333333; font-family: Arial, sans-serif; font-size: 16px; line-height: 20px;\"\n" +
          "       border cellspacing=0>\n" +
          "    <tr>\n" +
          "        <td style=\"font-weight:bold\">Source table(s) on Hermes RNO</td>\n" +
          "        <td style=\"font-weight:bold\">Target table(s) on Hermes LVS</td>\n" +
          "    </tr>\n" +
          "        {info}\n" +
          "</table>";


  protected static String DATA_VALIDATE_URL;
  private static String DATA_MOVE_URL;
  public static String META_TABLE_URL;
  public static String SCHEDULE_URL;
  public static String SCHEDULE_JOB_URL;
  private static String FROM_ADDR;

  public ZetaToolKitsJobEmailTemplate constructToolKitsJobTemplate(
      String jobName,
      String url,
      TemplateStatus status,
      String nt,
      String jobId,
      List<String> ccList,
      String adjunct,
      MailTemplate mailTemplate) {
    ZetaToolKitsJobEmailTemplate zetaEmailTemplate = new ZetaToolKitsJobEmailTemplate();
    zetaEmailTemplate.setJobName(jobName);
    zetaEmailTemplate.setUrl(url);
    zetaEmailTemplate.setTemplateStatus(status);
    zetaEmailTemplate.setnT(nt);
    zetaEmailTemplate.setJobId(jobId);
    zetaEmailTemplate.setCcList(ccList);
    zetaEmailTemplate.setAdjunct(adjunct);
    zetaEmailTemplate.setMailTemplate(mailTemplate);
    return zetaEmailTemplate;
  }

  public ZetaVdmMoveJobEmailTemplate constructVDMMoveJobTemplate(
      String sourceDb,
      String targetDb,
      List<String> overrideTables,
      String nt,
      String jobId,
      List<String> ccList,
      MailTemplate mailTemplate) {
    ZetaVdmMoveJobEmailTemplate zetaEmailTemplate = new ZetaVdmMoveJobEmailTemplate();
    zetaEmailTemplate.setSourceDb(sourceDb);
    zetaEmailTemplate.setTargetDb(targetDb);
    zetaEmailTemplate.setOverrideTables(overrideTables);
    zetaEmailTemplate.setnT(nt);
    zetaEmailTemplate.setJobId(jobId);
    zetaEmailTemplate.setCcList(ccList);
    zetaEmailTemplate.setMailTemplate(mailTemplate);
    return zetaEmailTemplate;
  }

  private String getEmailAddress(String email) {
    return email.contains("@") ? email : String.format("%s@ebay.com", email.trim());
  }

  enum ZetaEmailTemplateInfo {
    TO("toAddr"),
    CC("ccAddr"),
    FROM("fromAddr"),
    SUBJECT("subject"),
    CONTENT("content"),
    TEMPLATE("template"),
    TYPE("type");
    private String value;

    ZetaEmailTemplateInfo(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }

  public ZetaEmail sendZetaTemplateEmail(ZetaEmailTemplate zetaEmailTemplate) {
    LOGGER.info("Send [{}] mail", zetaEmailTemplate.getMailTemplate().name());
    Map<String, String> template = zetaEmailTemplate.getMailTemplate()
        .getTemplate(zetaEmailTemplate);
    template.put(TO.getValue(), getEmailAddress(zetaEmailTemplate.getnT()));
    if (Objects.nonNull(zetaEmailTemplate.getCcList())) {
      String ccList = zetaEmailTemplate.getCcList().stream()
          .filter(StringUtils::isNotBlank)
          .map(this::getEmailAddress)
          .collect(Collectors.joining(";"));
      LOGGER.info("Get Email CCList: [{}]", ccList);
      template.put(CC.getValue(), ccList);
    }
    LOGGER.info("Email Template: {}", JsonUtil.toJson(template));
    LOGGER.info("Email API: {}", SEND_EMAIL_API);
    ResponseEntity<String> res = restTemplate.postForEntity(SEND_EMAIL_API, template, String.class);
    int status = res.getStatusCode() == HttpStatus.OK ?
        (int) JsonUtil.fromJson(res.getBody(), Map.class).get("code")
        : res.getStatusCodeValue();
    LOGGER.info("Call Mail API With Zeta Template Response Status [{}], Res [{}]", status, res);
    ZetaEmail zetaEmail = new ZetaEmail();
    zetaEmail.setJobId(zetaEmailTemplate.getJobId());
    zetaEmail.setCreateTime(new Date());
    zetaEmail.setTemplate(zetaEmailTemplate.getMailTemplate().name());
    if (status == 200) {
      String emailId = Optional.ofNullable(JsonUtil.fromJson(res.getBody(), Map.class)
          .get("content")).orElse("-1").toString();
      zetaEmail.setEmailId(emailId.isEmpty() ? -1 : Long.parseLong(emailId));
      zetaEmail.setStatus("NotStart");
    } else {
      zetaEmail.setErrorLog("Call Email API Failed");
      zetaEmail.setStatus("Fail");
    }
    zetaEmailRepository.save(zetaEmail);
    return zetaEmail;
  }

  public boolean sendEmail(MailTemplate mailTemplate, String subject, String content
      , String toAddr, String... ccAddr) {
    LOGGER.info("Send [{}] mail", mailTemplate.name());
    Map<String, String> template = mailTemplate.getTemplate(null);
    template.put(SUBJECT.getValue(), subject);
    template.put(TO.getValue(), toAddr);
    template.put(CONTENT.getValue(), content);
    if (ccAddr.length > 0) {
      template.put(CC.getValue(), ccAddr[0]);
    }
    LOGGER.info("Call Mail API: {}", SEND_EMAIL_API);
    LOGGER.info("Call Mail API Post body: {}", JsonUtil.toJson(template));
    ResponseEntity<Map> res = restTemplate.postForEntity(SEND_EMAIL_API, template, Map.class);
    int status = res.getStatusCodeValue() == 200 ?
        (int) res.getBody().get("code") : res.getStatusCodeValue();
    LOGGER.info("Call Mail API Response Status [{}], Res [{}]", status, res);
    return status == 200;
  }

  public enum MailTemplate {
    DATA_MOVE {
      @Override
      Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate) {
        ZetaToolKitsJobEmailTemplate template = (ZetaToolKitsJobEmailTemplate) zetaEmailTemplate;
        String subject = getZetaToolKitsTemplateMailSubject(template);
        String text = getZetaToolKitsTemplateMailBody(template, DATA_MOVE_CONTENT);
        return initZetaToolKitsMailTemplate(template, subject, text);
      }
    }, DATA_VALIDATE {
      @Override
      Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate) {
        ZetaToolKitsJobEmailTemplate template = (ZetaToolKitsJobEmailTemplate) zetaEmailTemplate;
        String subject = getZetaToolKitsTemplateMailSubject(template);
        String text = getZetaToolKitsTemplateMailBody(template, DATA_VALIDATE_CONTENT);
        return initZetaToolKitsMailTemplate(template, subject, text);
      }
    }, SCHEDULER {
      @Override
      Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate) {
        ZetaToolKitsJobEmailTemplate template = (ZetaToolKitsJobEmailTemplate) zetaEmailTemplate;
        String subject = getZetaToolKitsTemplateMailSubject(template);
        String text = getZetaToolKitsTemplateMailBody(template, NB_SCHEDULER_CONTENT);
        return initZetaToolKitsMailTemplate(template, subject, text);
      }
    }, SCHEDULER_ALERT {
      @Override
      Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate) {
        ZetaToolKitsJobEmailTemplate template = (ZetaToolKitsJobEmailTemplate) zetaEmailTemplate;
        String subject = String.format(NB_SCHEDULER_ALERT_SUBJECT, template.getJobName());
        String text = String.format(NB_SCHEDULER_ALERT_CONTENT, template.getJobName(), template.getUrl());
        return initZetaToolKitsMailTemplate(template, subject, text);
      }
    }, VDM_MOVE {
      @Override
      Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate) {
        ZetaVdmMoveJobEmailTemplate template = (ZetaVdmMoveJobEmailTemplate) zetaEmailTemplate;
        String targetDb = template.getTargetDb();
        String sourceDb = template.getSourceDb();
        String tableInfo = template.getOverrideTables().stream()
            .map(tbl -> String.format("<tr><td>%s.%s</td><td>%s.%s</td></tr>"
                , sourceDb, tbl.trim(), targetDb, tbl.trim()))
            .collect(Collectors.joining("\n"));
        String text = VDM_MOVE_OVERRIDE_CONTENT.replace("{info}", tableInfo);
        return initZetaToolKitsMailTemplate(template, VDM_MOVE_OVERRIDE_SUBJECT, text);
      }
    }, TEXT {
      @Override
      Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate) {
        Map<String, String> template = initBasicTemplate();
        template.put(TYPE.getValue(), "2");
        return template;
      }
    }, HTML {
      @Override
      Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate) {
        Map<String, String> template = initBasicTemplate();
        template.put(TYPE.getValue(), "1");
        return template;
      }
    };

    abstract Map<String, String> getTemplate(ZetaEmailTemplate zetaEmailTemplate);

    Map<String, String> initBasicTemplate() {
      Map<String, String> template = Maps.newLinkedHashMap();
      template.put(FROM.getValue(), FROM_ADDR);
      return template;
    }

    Map<String, String> initZetaToolKitsMailTemplate(ZetaEmailTemplate zetaEmailTemplate
        , String mailSubject, String mailBody) {
      Map<String, String> emailTemplate = initBasicTemplate();
      emailTemplate.put(TEMPLATE.getValue(), "ZetaNotification");
      emailTemplate.put(TYPE.getValue(), "3");
      emailTemplate.put(SUBJECT.getValue(), mailSubject);
      Map<String, String> content = Maps.newLinkedHashMap();
      content.put("name", zetaEmailTemplate.getnT());
      content.put("msg", mailBody);
      emailTemplate.put(CONTENT.getValue(), JsonUtil.toJson(content));
      LOGGER.info("Zeta Tool Kits Email Init Template: {}", emailTemplate);
      return emailTemplate;
    }

    String getZetaToolKitsTemplateMailBody(ZetaToolKitsJobEmailTemplate template
        , String contentTemplate) {
      String content = String.format(contentTemplate, template.getJobName(),
          getBodyStatus(template.getTemplateStatus()), template.getUrl());
      return content.replace("{adjunct}", Optional.ofNullable(template.getAdjunct()).orElse(""));
    }

    String getZetaToolKitsTemplateMailSubject(ZetaToolKitsJobEmailTemplate template) {
      return String.format(TOOL_KITS_SUBJECT, template.getTemplateStatus(),
          template.getJobName(), template.getMailTemplate().name(), template.getTemplateStatus());
    }

    private String getBodyStatus(TemplateStatus status) {
      switch (status) {
        case Started:
          return "<font color = \"DodgerBlue\">Started</font>";
        case Failed:
          return "<font color = \"red\">Failed</font>";
        case Succeed:
          return "<font color = \"green\">Succeed</font>";
        case Canceled:
          return "<font color = \"Orange\">Canceled</font>";
        default:
          return String.format("<font color = \"Gray\">%s</font>", status.getDesc());
      }
    }
  }

  public ZetaEmail sendVdmMoveOverrideEmail(DataMoveDetail dataMoveDetail
      , String sourceDb, List<String> overrideTables) {
    History history = dataMoveDetail.getHistory();
    LOGGER.info("Start to send vdm move override email - {}", dataMoveDetail);
    ZetaVdmMoveJobEmailTemplate template = constructVDMMoveJobTemplate(
        sourceDb
        , history.getTargetTable()
        , overrideTables
        , history.getNt()
        , Optional.ofNullable(history.getHistoryId()).orElse(new Long(0)).toString()
        , null
        , MailService.MailTemplate.VDM_MOVE
    );
    return sendZetaTemplateEmail(template);
  }

  public ZetaEmail sendDataMoveDoneEmail(DataMoveDetail dataMoveDetail) {
    LOGGER.info("Start to send data move done email - {}", dataMoveDetail);
    ZetaToolKitsJobEmailTemplate zetaEmailTemplate = constructToolKitsJobTemplate(
        dataMoveDetail.getHistory().getSourceTable(),
        MailService.DATA_MOVE_URL,
        dataMoveDetail.getHistory().getStatus() == 1 ? Succeed : Failed,
        dataMoveDetail.getHistory().getNt(),
        dataMoveDetail.getHistory().getHistoryId().toString(),
        null, null,
        MailService.MailTemplate.DATA_MOVE);
    return sendZetaTemplateEmail(zetaEmailTemplate);
  }

  public ZetaEmail sendDataValidateDoneEmail(DataValidateDetail dataValidateDetail) {
    LOGGER.info("Start to send data validate done email - {}", dataValidateDetail);
    ZetaToolKitsJobEmailTemplate zetaEmailTemplate = constructToolKitsJobTemplate(
        dataValidateDetail.getHistory().getSourceTable(),
        MailService.DATA_VALIDATE_URL,
        dataValidateDetail.getHistory().getStatus() == 1 ? Succeed : Failed,
        dataValidateDetail.getHistory().getNt(),
        dataValidateDetail.getHistory().getHistoryId().toString(),
        null, null,
        MailService.MailTemplate.DATA_VALIDATE);
    return sendZetaTemplateEmail(zetaEmailTemplate);
  }
}
package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.common.NumberUtils;
import com.ebay.dss.zds.common.TempFile;
import com.ebay.dss.zds.common.ZipSqlFile;
import com.ebay.dss.zds.dao.ZetaStatementRepository;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InvalidInputException;
import com.ebay.dss.zds.exception.PermissionDenyException;
import com.ebay.dss.zds.interpreter.InterpreterFactory;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.model.UpdateStatementPlotConfigRequest;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.ZetaStatement;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.DumpFileService;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.ResultSet;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.ebay.dss.zds.common.ZipSqlFile.toFileBytes;

@RestController
@RequestMapping("/statements")
public class ZetaStatementController {

  protected static final Logger LOGGER = LoggerFactory.getLogger(ZetaStatementController.class);

  @Autowired
  private ZetaStatementRepository zetaStatementRepository;

  @Autowired
  private InterpreterFactory factory;

  @Autowired
  private DumpFileService dumpFileService;


  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  public ZetaResponse<ZetaStatement> getZetaStatement(
          @AuthenticationNT String nt,
          @PathVariable("id") int id) {
    ZetaStatement zetaStatement = zetaStatementRepository.getZetaStatement(nt, id);
    return new ZetaResponse<>(zetaStatement, HttpStatus.OK);
  }

  @RequestMapping(value = "/multiget//{idlist}", method = RequestMethod.GET)
  public ZetaResponse<List<ZetaStatement>> getZetaStatementList(
          @AuthenticationNT String nt,
          @PathVariable("idlist") String idList) {
    if (StringUtils.isEmpty(idList))
      throw new InvalidInputException(ErrorCode.INVALID_INPUT, "id list is not set");
    List<Long> ids = new ArrayList<>();
    for (String strId : idList.split(",")) {
      try {
        ids.add(Long.parseLong(strId));
      } catch (NumberFormatException ne) {
        throw new InvalidInputException(ErrorCode.INVALID_INPUT, "id list should be a number list separated by ','");
      }
    }

    List<ZetaStatement> zetaStatements = zetaStatementRepository.getZetaStatements(nt, ids);
    return new ZetaResponse<>(zetaStatements, HttpStatus.OK);
  }

  @RequestMapping(value = "/plotconfig/update", method = RequestMethod.POST)
  public ZetaResponse<String> updatePlotConfig(
          @AuthenticationNT String nt,
          @RequestBody UpdateStatementPlotConfigRequest plotConfig) {

    // check zetastatement by owner and id
    try {
      ZetaStatement statement = zetaStatementRepository.getZetaStatement(nt, plotConfig.getId());
    } catch (EmptyResultDataAccessException e) {
      throw new PermissionDenyException(ErrorCode.UNAUTHORIZED, String.format("The user has no access to the statement: (%d, %s).", plotConfig.getId(), nt));
    }
    zetaStatementRepository.updatePlotConfigById(plotConfig.getId(), plotConfig.getPlotConfig());
    return new ZetaResponse<>("update successfully.", HttpStatus.OK);
  }

  @GetMapping("/dump/job/{id}")
  public void getDumpResult(@AuthenticationNT String nt,
                            @PathVariable("id") String jobRequestId,
                            HttpServletResponse response) throws IOException {
    try (
            OutputStream out = response.getOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(out);
    ) {
      writeToZip(nt, jobRequestId, zipOut);
    }
  }

  @Value("${zds.dump.response.buffer-size: #{64*1024}}")
  private int dumpBufferSize = 64 * 1024;

  private void writeToZip(String nt, String jobRequestId, ZipOutputStream zipOut) throws IOException {
    ZipEntry zip = new ZipEntry(nt + jobRequestId + ".csv");
    zipOut.putNextEntry(zip);
    try (
            Writer writer = new OutputStreamWriter(zipOut);
    ) {

      TempFile dumpFile = dumpFileService.get(nt, jobRequestId);
      dumpFile.read(reader -> {
        readToWrite(reader, writer);
      });
    }
  }

  private void readToWrite(Reader reader, Writer writer) {
    try {
      int readed = 0;
      char[] buffer = new char[dumpBufferSize];
      while (readed >= 0) {
        readed = reader.read(buffer, 0, buffer.length);
        if (readed > 0) {
          writer.write(buffer, 0, readed);
        }
      }
    } catch (IOException e) {
      LOGGER.error("Dump error", e);
    }
  }

  private void readToBuffer(BufferedReader reader, StringBuilder stringBuilder) {
    try {
      BufferedReader br = reader;
      br.lines().forEach(stringBuilder::append);
    } catch (Exception ex) {
      LOGGER.error("Dump error", ex);
    }
  }

  @RequestMapping(value = "/dump/{noteId}/{interpreter}/{requestId}", method = RequestMethod.GET)
  public void getDumpResult(
          @AuthenticationNT String nt,
          // mapping to jobId in the execution context
          @PathVariable("requestId") int requestId,
          @PathVariable("interpreter") String interpreter,
          @PathVariable("noteId") String noteId,
          HttpServletResponse response) throws IOException {
    String respFileName = String.format("result-%s-%d.zip", nt, requestId);
    String respFileNameHeader = String.format("attachment; filename=\"%s\"", respFileName);
    response.setHeader("Content-Disposition", respFileNameHeader);
    InterpreterGroup intpGrp = null;
    Interpreter intp = null;
    InterpreterType.EnumType interpreterType = null;
    try {
      LOGGER.info("start to get dump result for user: [{}]'s note: [{}]", nt, noteId);

      intpGrp = factory.searchInterpreterGroup(nt, noteId);

      if (intpGrp == null) throw new IOException("failed to get the result," +
              " because the session is not found in " + nt + "'s note: " + noteId + ", please reconnect");

      interpreterType = InterpreterType.fromString(interpreter);

      if (InterpreterType.EnumType.UNKNOWN.equals(interpreterType))
        throw new IOException("unknown interpreter: " + interpreter);

      intp = intpGrp.getInterpreter(interpreterType.getInterpreterClass().getName());

      if (intp == null) throw new IOException("failed to get the result," +
              " because the session instance is not found in " + nt + "'s note: " + noteId + ", please reconnect");
    } catch (Exception ex) {
      LOGGER.error("Dump error, note: {}, error: {}", noteId, ExceptionUtils.getFullStackTrace(ex));
      respFileName = String.format("result-%s-%d-connection-lost-error.txt", nt, requestId);
      respFileNameHeader = String.format("attachment; filename=\"%s\"", respFileName);
      response.setHeader("Content-Disposition", respFileNameHeader);
      ServletOutputStream outputStream = response.getOutputStream();
      outputStream.write(("Error happened: " + ex.getMessage()).getBytes());
      outputStream.flush();
      outputStream.close();
      return;
    }

    String csv = "";
    long start = 0L;
    byte[] zip;
    long end = 0L;

    LOGGER.info("interpreter type: " + interpreterType.getName());
   if (interpreterType == InterpreterType.EnumType.JDBC) {
      getDumpResult(nt, String.valueOf(requestId), response);
      return;
    } else {

      List<ZetaStatement> zetaStatements = zetaStatementRepository.getZetaStatementByRequest(nt, requestId);

      zetaStatements.sort((l, r) -> r.getLivyStatementId() - l.getLivyStatementId());

      // get the last one only
      int stmtId = zetaStatements.get(0).getLivyStatementId();

      InterpreterResult interpreterResult = (InterpreterResult) intp.getStore().get(stmtId);

      intp.clearStore();

      if (interpreterResult.code() == InterpreterResult.Code.SUCCESS) {
        csv = interpreterResult.getResultMessage(InterpreterResult.Type.TEXT).getData();
        LOGGER.info("dump result got for user: [{}]'s note: [{}]", nt, noteId);
      }

      start = System.currentTimeMillis();
      zip = ZipSqlFile.getFileZipBytes(System.currentTimeMillis() + "_zeta.csv", csv);
      end = System.currentTimeMillis();
    }

    String zipSize = NumberUtils.formatBytes((long) zip.length);

    LOGGER.info("zip file ready for user: [{}]'s note: [{}], dumping..., zip size: [{}], zip cost: [{}]s",
            nt, noteId, zipSize, (end - start) / 1000.0);

    long trans_start = System.currentTimeMillis();
    byte[] buffer = new byte[102400];
    ByteArrayInputStream zis = null;
    BufferedInputStream bis = null;
    try {
      zis = new ByteArrayInputStream(zip);
      bis = new BufferedInputStream(zis, 102400);
      OutputStream outputStream = response.getOutputStream();
      int i = bis.read(buffer);
      while (i != -1) {
        outputStream.write(buffer, 0, i);
        i = bis.read(buffer);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (bis != null) {
        try {
          bis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (zis != null) {
        try {
          zis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    long trans_end = System.currentTimeMillis();

    LOGGER.info("zip file dump finished for user: [{}]'s note: [{}] size: [{}], download time: [{}]s",
            nt, noteId, zipSize, (trans_end - trans_start) / 1000.0);
  }
}

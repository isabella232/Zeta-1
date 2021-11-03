package com.ebay.dss.zds.magic;

import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.model.NotebookVarsMap;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;

import java.util.regex.Matcher;

/**
 * Created by tatian on 2020-11-17.
 */
public class HandleContext {

  public final Interpreter interpreter;
  public final Matcher matcher;
  public final CodeWithSeq codeWithSeq;
  public final ZetaNotebook notebook;
  public final NotebookVarsMap varsMap;
  public final ZetaNotebookRepository repository;

  public HandleContext(Interpreter interpreter,
                       Matcher matcher,
                       CodeWithSeq codeWithSeq,
                       ZetaNotebook notebook,
                       NotebookVarsMap varsMap,
                       ZetaNotebookRepository repository) {
    this.interpreter = interpreter;
    this.matcher = matcher;
    this.codeWithSeq = codeWithSeq;
    this.notebook = notebook;
    this.varsMap = varsMap;
    this.repository = repository;
  }
}

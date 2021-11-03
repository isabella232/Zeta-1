package org.apache.zeppelin.interpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static org.apache.zeppelin.interpreter.InterpreterResult.Type.MERGED_MSG;

/**
 * Interpreter result template.
 */
public class InterpreterResult implements Serializable {
    transient Logger logger = LoggerFactory.getLogger(InterpreterResult.class);

    /**
     * Type of result after code execution.
     */
    public static enum Code {
        CLOSED,
        SUCCESS,
        INCOMPLETE,
        ERROR,
        KEEP_PREVIOUS_RESULT
    }

    /**
     * Type of Data.
     */
    public static enum Type {
        TEXT,
        HTML,
        ANGULAR,
        CSV,
        TABLE,
        IMG,
        SVG,
        NULL,
        NETWORK,
        MERGED_MSG
    }

    Code code;
    List<InterpreterResultMessage> msg = new LinkedList<>();

    public InterpreterResult(Code code) {
        this.code = code;
    }

    public InterpreterResult(Code code, List<InterpreterResultMessage> msgs) {
        this.code = code;
        msg.addAll(msgs);
    }

    public InterpreterResult(Code code, String msg) {
        this.code = code;
        add(msg);
    }

    public InterpreterResult(Code code, Type type, String msg) {
        this.code = code;
        add(type, msg);
    }

    /**
     * Automatically detect %[display_system] directives
     *
     * @param msg
     */
    public void add(String msg) {
        InterpreterOutput out = new InterpreterOutput(null);
        try {
            out.write(msg);
            out.flush();
            this.msg.addAll(out.toInterpreterResultMessage());
            out.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public void add(Type type, String data) {
        msg.add(new InterpreterResultMessage(type, data));
    }

    public void add(InterpreterResultMessage interpreterResultMessage) {
        msg.add(interpreterResultMessage);
    }

    public Code code() {
        return code;
    }

    public List<InterpreterResultMessage> message() {
        return msg;
    }

    public InterpreterResultMessage getResultMessage(Type type) {
        for(InterpreterResultMessage message : msg) {
            if (message.getType() == type) {
                return message;
            }
        }
        return new InterpreterResultMessage(Type.TEXT, "");
    }

    public String getMergedJsonMessage(){
        return msg.stream()
                .filter(m->m.type.equals(MERGED_MSG))
                .findFirst()
                .orElse(new InterpreterResultMessage(MERGED_MSG,""))
                .data;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Type prevType = null;
        for (InterpreterResultMessage m : msg) {
            if (prevType != null) {
                sb.append("\n");
                if (prevType == Type.TABLE) {
                    sb.append("\n");
                }
            }
            sb.append(m.toString());
            prevType = m.getType();
        }

        return sb.toString();
    }
}

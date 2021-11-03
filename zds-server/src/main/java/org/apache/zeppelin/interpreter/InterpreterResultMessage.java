package org.apache.zeppelin.interpreter;

/**
 * Created by tatian on 2019-04-02.
 */
import org.apache.zeppelin.interpreter.InterpreterResult.Type;

public class InterpreterResultMessage {
    Type type;
    String data;

    public InterpreterResultMessage(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return this.type;
    }

    public String getData() {
        return this.data;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String toString() {
        return "%" + this.type.name().toLowerCase() + " " + this.data;
    }
}

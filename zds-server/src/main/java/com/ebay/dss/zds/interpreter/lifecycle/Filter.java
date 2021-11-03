package com.ebay.dss.zds.interpreter.lifecycle;

import java.util.Properties;

/**
 * Created by tatian on 2018/6/11.
 */
public interface Filter {

    boolean filtered();
    void addRule();
    void removeRule();
    Filter create();
    void apply(Properties prop);
    String explain();
    String introduce();
}

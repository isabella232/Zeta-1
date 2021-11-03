package com.ebay.dss.zds.function;

/**
 * Created by tatian on 2020-06-12.
 */
public interface MapFunc<I, O> {

  O map(I in) throws Exception;
}

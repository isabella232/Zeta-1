package com.ebay.dss.zds.state.annotation;

import com.ebay.dss.zds.state.source.InMemoryStateSource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by tatian on 2020-09-18.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StateSourcePrefer {

  Class prefer() default InMemoryStateSource.class;
}

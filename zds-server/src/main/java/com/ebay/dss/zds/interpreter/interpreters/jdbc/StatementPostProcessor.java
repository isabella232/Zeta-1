package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import java.sql.Statement;

@FunctionalInterface
interface StatementPostProcessor<T> {

    T execute(Statement statement) throws Exception;
}

package com.ebay.dss.zds.jupyter;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public class JupyterConfRepository {
  @Autowired
  private JdbcTemplate jdbcTemplate;

  public String getUserPySparkConf(String nt, String confName) {
    String sql = "SELECT pyspark_conf FROM jupyter_notebook_conf WHERE nt=? and conf_name=?";
    return jdbcTemplate.queryForObject(sql, new Object[]{nt, confName}, String.class);
  }

  @Transactional
  public String insertPysparkConf(String nt, String confName, String confBody) {
    String sql = "insert into jupyter_notebook_conf(nt,conf_name,pyspark_conf) values (?,?,?)";
    if (jdbcTemplate.update(sql, new Object[]{nt, confName, confBody}) > 0) {
      return confBody;
    }
    return null;
  }

  @Transactional
  public String updatePysparkConf(String nt, String confName, String confBody) {
    String sql = "update jupyter_notebook_conf set pyspark_conf=? where nt=? and conf_name=?";
    if (jdbcTemplate.update(sql, new Object[]{confBody, nt, confName}) > 0) {
      return confBody;
    }
    return null;
  }

}

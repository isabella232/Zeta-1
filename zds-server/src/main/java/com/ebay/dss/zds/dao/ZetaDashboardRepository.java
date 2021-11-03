package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.model.ZetaDashboard;
import com.ebay.dss.zds.model.ZetaStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class ZetaDashboardRepository {
  @Autowired
  private NamedParameterJdbcTemplate template;

  private RowMapper<ZetaDashboard> mapper = BeanPropertyRowMapper.newInstance(ZetaDashboard.class);

  /**
   * Insert a new dashboard into DB per parameter passed in.
   * @param dashboard
   * @return
   */
  public long addDashboard(ZetaDashboard dashboard) {
    String sql = "insert into zeta_dashboard (nt, `name`, `path`, `layout_config`, `create_dt`, `update_dt`) " +
      "values (:nt, :name, :path, :layoutConfig, :createDt, :updateDt)";

    if ( dashboard.getCreateDt() == null ) dashboard.setCreateDt(new Timestamp(System.currentTimeMillis()));
    MapSqlParameterSource ps = new MapSqlParameterSource();
    ps.addValue("nt", dashboard.getNt())
      .addValue("name", dashboard.getName())
      .addValue("path", dashboard.getPath())
      .addValue("layoutConfig", dashboard.getLayoutConfig())
      .addValue("createDt", dashboard.getCreateDt())
      .addValue("updateDt", dashboard.getUpdateDt());

    GeneratedKeyHolder kh = new GeneratedKeyHolder();
    template.update(sql, ps, kh);
    long id = kh.getKey().longValue();
    dashboard.setId(id);
    return id;
  }

  /**
   * Update layout configuration for the given id.
   * @param id - id of the dashboard
   * @param layoutConfig - layout config to be set
   * @throws EntityNotFoundException if no record found and updated.
   */
  public void updateLayoutConfigById(long id, String layoutConfig) {
    String sql = "update zeta_dashboard set `layout_config` = :layoutConfig, `update_dt` = :updateDt where id = :id";

    MapSqlParameterSource ps = new MapSqlParameterSource();
    ps.addValue("layoutConfig", layoutConfig)
      .addValue("updateDt", new Timestamp(System.currentTimeMillis()))
      .addValue("id", id);


    int updateCount = template.update(sql, ps);
    if ( updateCount != 1 ) throw new EntityNotFoundException(String.format("Record not found for update, %d",id));
  }

  /**
   * Get all dashboards owned by the given user.
   * @param nt - nt of the owner
   * @return - list of dashboards of the user
   */
  public List<ZetaDashboard> queryForUser(String nt) {
    String sql = "select * from zeta_dashboard where `nt` = :nt";

    MapSqlParameterSource ps = new MapSqlParameterSource();
    ps.addValue("nt", nt);

    return template.query(sql, ps, mapper);
  }

  /**
   * Get zeta dashboard by the given id
   * @param id - id of dashboard
   * @return - Zeta dashboard instance found.
   */
  public ZetaDashboard queryById(long id) {
    String sql = "select * from zeta_dashboard where id = :id";

    MapSqlParameterSource ps = new MapSqlParameterSource();
    ps.addValue("id", id);

    return template.queryForObject(sql, ps, mapper);
  }

  /**
   * Delete the dashboard with given id
   * @param id - id of the dashboard to be deleted.
   */
  public boolean deleteById(long id) {
    String sql = "delete from zeta_dashboard where id = :id";
    MapSqlParameterSource ps = new MapSqlParameterSource();
    ps.addValue("id", id);

    int cnt = template.update(sql, ps);
    return cnt == 1;
  }
}

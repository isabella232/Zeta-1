package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaStatementContext;
import com.ebay.dss.zds.model.ZetaStatus;
import com.ebay.dss.zds.exception.EntityIsNullException;
import com.ebay.dss.zds.model.ZetaStatement;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class ZetaStatementRepository {


    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private ZetaStatementMapper mapper = new ZetaStatementMapper();
    private ZetaStatementContextMapper contextMapper = new ZetaStatementContextMapper();

    static final String QUERY_GET_STATEMENT = "select * from zeta_statement " +
            "join zeta_job_request on zeta_statement.request_id = zeta_job_request.id " +
            "join zeta_notebook on zeta_job_request.notebook_id = zeta_notebook.id " +
            "where zeta_statement.id=?";

    static final String QUERY_GET_STATEMENT_WITHOUT_NT = "select * from zeta_statement " +
            "join zeta_job_request on zeta_statement.request_id = zeta_job_request.id " +
            "join zeta_notebook on zeta_job_request.notebook_id = zeta_notebook.id " +
            "where zeta_statement.id=?";

    static final String QUERY_GET_STATEMENT_BY_REQUEST = "select * from zeta_statement " +
            "join zeta_job_request on zeta_statement.request_id = zeta_job_request.id " +
            "join zeta_notebook on zeta_job_request.notebook_id = zeta_notebook.id " +
            "where zeta_statement.request_id=? and zeta_notebook.nt=?";

    static final String QUERY_GET_STATEMENTS = "select * from zeta_statement " +
      "join zeta_job_request on zeta_statement.request_id = zeta_job_request.id " +
      "join zeta_notebook on zeta_job_request.notebook_id = zeta_notebook.id " +
      "where zeta_statement.id in (:ids) and zeta_notebook.nt=:nt";
    static final String QUERY_GET_STATEMENTS_WITHOUT_NT = "select * from zeta_statement " +
            "join zeta_job_request on zeta_statement.request_id = zeta_job_request.id " +
            "join zeta_notebook on zeta_job_request.notebook_id = zeta_notebook.id " +
            "where zeta_statement.id in (:ids)";
    static final String QUERY_GET_STATEMENT_WITH_CONTEXT = "select a.id, a.request_id, a.statement, a.seq, a.status,\n" +
            "       b.preference, b.livy_job_url, b.proxy_user, b.platform,\n" +
            "       a.create_dt, a.start_dt, a.update_dt\n" +
            "from zeta_statement a, zeta_job_request b\n" +
            "where a.id=? and a.request_id = b.id";

    @Autowired
    public ZetaStatementRepository(JdbcTemplate jdbcTemplate,NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public ZetaStatement getZetaStatement(String nt, long id){
        return jdbcTemplate.queryForObject(QUERY_GET_STATEMENT,
                new Object[]{id}, mapper);
    }
    public ZetaStatement getZetaStatement(long id){
        return jdbcTemplate.queryForObject(QUERY_GET_STATEMENT_WITHOUT_NT,
                new Object[]{id}, mapper);
    }

    public List<ZetaStatement> getZetaStatements(String nt, List<Long> ids) {
        MapSqlParameterSource ps = new MapSqlParameterSource();
        ps.addValue("ids", ids).addValue("nt", nt);
        return namedParameterJdbcTemplate.query(QUERY_GET_STATEMENTS, ps, mapper);
    }

    public List<ZetaStatement> getZetaStatements(List<Long> ids) {
        MapSqlParameterSource ps = new MapSqlParameterSource();
        ps.addValue("ids", ids);
        return namedParameterJdbcTemplate.query(QUERY_GET_STATEMENTS_WITHOUT_NT, ps, mapper);
    }
    public List<ZetaStatement> getZetaStatementByRequest(long requestId){
        return jdbcTemplate.query("select * from zeta_statement where request_id=?",
                new Object[]{requestId}, mapper);
    }

    public List<ZetaStatement> getZetaStatementByRequest(String nt, long requestId){
        return jdbcTemplate.query(QUERY_GET_STATEMENT_BY_REQUEST,
                new Object[]{requestId, nt}, mapper);
    }

    public ZetaStatementContext getZetaStatementContext(long id){
        return jdbcTemplate.queryForObject(QUERY_GET_STATEMENT_WITH_CONTEXT,
                new Object[]{id}, contextMapper);
    }

    public PreparedStatement proxyPreparedStatementCreator(Connection connection, String sql, ZetaStatement statement) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, statement.getRequestId());
        ps.setString(2, statement.getStatement());
        ps.setString(3, statement.getResult());
        ps.setTimestamp(4, new Timestamp(new DateTime().getMillis()));
        ps.setTimestamp(5, statement.getStartDt());
        ps.setTimestamp(6, new Timestamp(new DateTime().getMillis()));
        ps.setInt(7, statement.getSeq());
        ps.setString(8, statement.getStatus());
        ps.setInt(9, statement.getLivySessionId());
        ps.setInt(10, statement.getLivyStatementId());
        return ps;
    }

    public long addZetaStatement(ZetaStatement statement){
        if(statement == null) {
            throw new EntityIsNullException("Cannot insert NULL entity into Zeta Statement Repository!");
        }
        final ZetaStatement finalStatement = statement;
        String sql = "insert into zeta_statement (request_id, statement, result, create_dt,start_dt,update_dt, seq, status, livy_session_id,livy_statement_id) values(?,?,?,?,?,?,?,?,?,?)";
        final KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update((connection) -> proxyPreparedStatementCreator(connection, sql, finalStatement), holder);
        return holder.getKey().longValue();
    }

    public int updateZetaStatement(ZetaStatement statement){
        if(statement == null) {
            throw new EntityIsNullException("Cannot update NULL entity from Zeta Statement Repository!");
        }
        return jdbcTemplate.update("update zeta_statement set result=?,start_dt=?,update_dt=?,status=?,progress=? where id=?",
                statement.getResult(),statement.getStartDt(),new Timestamp(new DateTime().getMillis()),statement.getStatus(),statement.getProgress(), statement.getId());
    }

    public int updateZetaStatementStateById(long id, String status,String result,int progress,Timestamp now){
        return jdbcTemplate.update("update zeta_statement set status=?,update_dt=?,result=?,progress=? where id=?",status,now,result,progress, id);
    }

    public int updateUnfinishedZetaStatementStateById(long id, String status,String result,int progress,Timestamp now){
        return jdbcTemplate.update("update zeta_statement set status=?,update_dt=?,result=?,progress=? where id=? and status!=? and status!=?",
                status,now,result,progress, id,ZetaStatus.FINISHED.getStatusCode(),ZetaStatus.CANCELED.getStatusCode());
    }

    public int updateZetaStatementResultById(long id, String result){
        return jdbcTemplate.update("update zeta_statement set result=? where id=?", result, id);
    }

    public int updatePlotConfigById(long id, String plotConfig) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return jdbcTemplate.update("update zeta_statement set plot_config=?,update_dt=? where id=?", plotConfig,now,id);
    }

    public int deleteZetaStatementByRequest(long request) {
        return jdbcTemplate.update("delete from zeta_statement where request_id=?", request);
    }

    public static final class ZetaStatementMapper implements RowMapper<ZetaStatement> {
        @Override
        public ZetaStatement mapRow(ResultSet rs, int i) throws SQLException {
            ZetaStatement statement = new ZetaStatement();
            statement.setId(rs.getLong("id"));
            statement.setRequestId(rs.getLong("request_id"));
            statement.setStatement(rs.getString("statement"));
            statement.setResult(rs.getString("result"));
            statement.setCreateDt(rs.getTimestamp("create_dt"));
            statement.setStartDt(rs.getTimestamp("start_dt"));
            statement.setUpdateDt(rs.getTimestamp("update_dt"));
            statement.setSeq(rs.getInt("seq"));
            statement.setStatus(rs.getString("status"));
            statement.setLivySessionId(rs.getInt("livy_session_id"));
            statement.setLivyStatementId(rs.getInt("livy_statement_id"));
            statement.setProgress(rs.getInt("progress"));
            //to enable plotting.
            statement.setPlotConfig(rs.getString("plot_config"));
            return statement;
        }
    }

    public static final class ZetaStatementContextMapper implements RowMapper<ZetaStatementContext> {
        @Override
        public ZetaStatementContext mapRow(ResultSet rs, int i) throws SQLException {
            ZetaStatementContext statementContext = new ZetaStatementContext();
            statementContext.setId(rs.getLong("id"));
            statementContext.setRequestId(rs.getLong("request_id"));
            statementContext.setStatement(rs.getString("statement"));
            statementContext.setSeq(rs.getInt("seq"));
            statementContext.setStatus(rs.getString("status"));
            statementContext.setPreference(rs.getString("preference"));
            statementContext.setJobUrl(rs.getString("livy_job_url"));
            statementContext.setProxyUser(rs.getString("proxy_user"));
            statementContext.setPlatform(rs.getString("platform"));

            Timestamp createDt = rs.getTimestamp("create_dt");
            if (createDt != null) {
                statementContext.setCreateDt(createDt.getTime());
            }

            Timestamp startDt = rs.getTimestamp("start_dt");
            if (startDt != null) {
                statementContext.setStartDt(startDt.getTime());
            } else if (createDt != null) {
                statementContext.setStartDt(createDt.getTime());
            }

            Timestamp updateDt = rs.getTimestamp("update_dt");
            if (updateDt != null) {
                statementContext.setUpdateDt(updateDt.getTime());
            }
            return statementContext;
        }
    }
}

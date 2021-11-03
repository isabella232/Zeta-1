package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.exception.EntityIsNullException;
import com.ebay.dss.zds.model.ZetaJobRequest;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class ZetaJobRequestRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<ZetaJobRequest> getAllZetaJobRequests(){
        List<ZetaJobRequest> rs = jdbcTemplate.query("select * from zeta_job_request", new ZetaJobRequestMapper());
        return rs;
    }

    public ZetaJobRequest getZetaJobRequest(long id){
        return jdbcTemplate.queryForObject("select *  from zeta_job_request where id=?", new Object[]{id}, new ZetaJobRequestMapper());
    }

    public List<ZetaJobRequest> getZetaJobRequestsByNotebook(String notebook){
        List<ZetaJobRequest> rs = jdbcTemplate.query("select * from zeta_job_request where notebook_id=?", new Object[]{notebook}, new ZetaJobRequestMapper());
        return rs;
    }

    public long addZetaJobRequest(ZetaJobRequest request){
        if (request == null){
            throw new EntityIsNullException("Cannot insert NULL Job request into repository!");
        }
        String sql = "insert into zeta_job_request (notebook_id,content,create_dt,start_dt,update_dt,livy_session_id,livy_job_url,status,preference,proxy_user, platform) values (?,?,?,?,?,?,?,?,?,?,?)";
        final KeyHolder holder = new GeneratedKeyHolder();

        ZetaJobRequest finalRequest = request;
        jdbcTemplate.update(((connection) -> {
            PreparedStatement ps = connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, finalRequest.getNotebookId());
            ps.setString(2, finalRequest.getContent());
            ps.setTimestamp(3, new Timestamp(new DateTime().getMillis()));
            ps.setTimestamp(4, finalRequest.getStartDt());
            ps.setTimestamp(5, new Timestamp(new DateTime().getMillis()));
            ps.setInt(6, finalRequest.getLivySessionId());
            ps.setString(7, finalRequest.getLivyJobUrl());
            ps.setString(8, finalRequest.getStatus());
            ps.setString(9, finalRequest.getPreference());
            ps.setString(10, finalRequest.getProxyUser());
            ps.setString(11, finalRequest.getPlatform());
            return ps;
        }), holder);
        return holder.getKey().longValue();
    }

    public int updateZetaJobRequest(ZetaJobRequest request){
        if (request == null){
            throw new EntityIsNullException("Cannot update NULL Job request from repository!");
        }
        return jdbcTemplate.update("update zeta_job_request set start_dt=?,update_dt=?,livy_session_id=?,livy_job_url=?,status=? where id=?", request.getStartDt(),new Date(new DateTime().getMillis()),request.getLivySessionId(),request.getLivyJobUrl(),request.getStatus(),request.getId());
    }

    public int updateZetaJobRequestStateById(long id, String status){
        return jdbcTemplate.update("update zeta_job_request set status=?, update_dt=? where id=?", status, new Date(new DateTime().getMillis()), id);
    }

    public int deleteZetaJobRequest(long id){
        return jdbcTemplate.update("delete from zeta_job_request where id=?", id);
    }

    public int deleteZetaJobRequestByNotebook(String notebookId){
        return jdbcTemplate.update("delete from zeta_job_request where notebook_id=?", notebookId);
    }

    private static final class ZetaJobRequestMapper implements RowMapper<ZetaJobRequest>{

        @Override
        public ZetaJobRequest mapRow(ResultSet rs, int i) throws SQLException {
            ZetaJobRequest request = new ZetaJobRequest();
            request.setId(rs.getLong("id"));
            request.setNotebookId(rs.getString("notebook_id"));
            request.setContent(rs.getString("content"));
            request.setCreateDt(rs.getTimestamp("create_dt"));
            request.setStartDt(rs.getTimestamp("start_dt"));
            request.setLivySessionId(rs.getInt("livy_session_id"));
            request.setLivyJobUrl(rs.getString("livy_job_url"));
            request.setStatus(rs.getString("status"));
            request.setPreference(rs.getString("preference"));
            request.setProxyUser(rs.getString("proxy_user"));
            request.setPlatform(rs.getString("platform"));
            return request;
        }
    }
}

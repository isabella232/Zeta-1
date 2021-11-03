package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.exception.EntityIsNullException;
import com.ebay.dss.zds.message.event.ZetaOperationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

/**
 * Created by tatian on 2019-06-19.
 */
@Repository
public class ZetaEventRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public long saveLivyConnectionSuccess(ZetaOperationEvent.ZetaLivyConnectionSuccess success) {
        if (success == null) {
            throw new EntityIsNullException("Cannot insert NULL Job request into repository!");
        }
        String sql = "insert into zeta_livy_event_fact " +
                "(nt," +
                "proxy_user," +
                "cluster_id," +
                "targetQueue," +
                "endpoint," +
                "notebook_id," +
                "interpreter_class," +
                "usingConnectionQueue," +
                "connectionQueueSuccess," +
                "connectionQueueFailReason," +
                "connectionQueueCost," +
                "totalCost," +
                "success," +
                "reason," +
                "create_dt) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        final KeyHolder holder = new GeneratedKeyHolder();

        jdbcTemplate.update(((connection) -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, success.getNt());
            ps.setString(2, success.getProxyUser());
            ps.setInt(3, success.getClusterId());
            ps.setString(4, success.getTargetQueue());
            ps.setString(5, success.getEndpoint());
            ps.setString(6, success.getNoteId());
            ps.setString(7, success.getInterpreterClass());
            ps.setInt(8, success.isUsingConnectionQueue() ? 1 : 0);
            ps.setInt(9, success.isConnectionQueueSuccess() ? 1 : 0);
            ps.setString(10, success.getConnectionQueueFailReason());
            ps.setLong(11, success.getConnectionQueueCost());
            ps.setLong(12, success.getTotalCost());
            ps.setInt(13, 1); // this is an success event
            ps.setString(14, "success");
            ps.setTimestamp(15, new Timestamp(success.getRecordTime().getMillis()));
            return ps;
        }), holder);
        return holder.getKey().longValue();
    }

    public long saveLivyConnectionFail(ZetaOperationEvent.ZetaLivyConnectionFail fail) {
        if (fail == null) {
            throw new EntityIsNullException("Cannot insert NULL Job request into repository!");
        }
        String sql = "insert into zeta_livy_event_fact " +
                "(nt," +
                "proxy_user," +
                "cluster_id," +
                "targetQueue," +
                "endpoint," +
                "notebook_id," +
                "interpreter_class," +
                "usingConnectionQueue," +
                "connectionQueueSuccess," +
                "connectionQueueFailReason," +
                "connectionQueueCost," +
                "totalCost," +
                "success," +
                "reason," +
                "create_dt) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        final KeyHolder holder = new GeneratedKeyHolder();

        jdbcTemplate.update(((connection) -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fail.getNt());
            ps.setString(2, fail.getProxyUser());
            ps.setInt(3, fail.getClusterId());
            ps.setString(4, fail.getTargetQueue());
            ps.setString(5, fail.getEndpoint());
            ps.setString(6, fail.getNoteId());
            ps.setString(7, fail.getInterpreterClass());
            ps.setInt(8, fail.isUsingConnectionQueue() ? 1 : 0);
            ps.setInt(9, fail.isConnectionQueueSuccess() ? 1 : 0);
            ps.setString(10, fail.getConnectionQueueFailReason());
            ps.setLong(11, fail.getConnectionQueueCost());
            ps.setLong(12, fail.getTotalCost());
            ps.setInt(13, 0); // this is an success event
            ps.setString(14, fail.getReason());
            ps.setTimestamp(15, new Timestamp(fail.getRecordTime().getMillis()));
            return ps;
        }), holder);
        return holder.getKey().longValue();
    }

    public long saveManualSQLConvertEvent(ZetaOperationEvent.ZetaManualSQLConvertEvent event) {
        if (Objects.isNull(event)) {
            throw new EntityIsNullException("Cannot insert NULL Job request into repository!");
        }
        String sql = "insert into sql_convert_history(nt,content,type,status,error,cre_date) values (?,?,?,?,?,?)";
        if (event instanceof ZetaOperationEvent.ZetaManualSQLConvertEvent.ZetaManualSQLConvertFailedEvent) {
            return (long) jdbcTemplate.update(sql, event.getNt(), event.getQuery(), 1, "Fail", ((ZetaOperationEvent.ZetaManualSQLConvertFailedEvent) event).getError(), new Date());
        } else {
            return (long) jdbcTemplate.update(sql, event.getNt(), event.getQuery(), 1, "Success", null, new Date());
        }

    }

    public long saveBatchSQLConvertEvent(ZetaOperationEvent.ZetaBatchSQLConvertEvent event) {
        if (Objects.isNull(event)) {
            throw new EntityIsNullException("Cannot insert NULL Job request into repository!");
        }
        String sql = "insert into sql_convert_history(platform,nt,content,type,status,cre_date) values (?,?,?,?,?,?)";
        if (event instanceof ZetaOperationEvent.ZetaBatchSQLConvertEvent.ZetaBatchSQLConvertFailedEvent) {
            return (long) jdbcTemplate.update(sql, event.getPlatform(), event.getNt(), event.getTable(), 0, "Fail", new Date());
        } else {
            return jdbcTemplate.update(sql, event.getPlatform(), event.getNt(), event.getTable(), 0, "Success", new Date());
        }
    }

}

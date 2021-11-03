package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.exception.EntityIsNullException;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.ZetaNotification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by tianrsun on 2018/7/19.
 */
@Repository
public class ZetaNotificationRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public List<ZetaNotification> getNotificationsStartedButNotEnd() {
        return jdbcTemplate.query("select * from zeta_notification where end_time > now() and start_time <= now()", new Object[]{}, new ZetaNotificationMapper());
    }


    private static final class ZetaNotificationMapper implements RowMapper<ZetaNotification> {

        @Override
        public ZetaNotification mapRow(ResultSet rs, int i) throws SQLException {
            ZetaNotification notification = new ZetaNotification();
            notification.setId(rs.getInt("id"));
            notification.setContent(rs.getString("content"));
            notification.setTitle(rs.getString("title"));
            notification.setStartTime(rs.getTimestamp("start_time"));
            notification.setEndTime(rs.getTimestamp("end_time"));
            notification.setCreateTime(rs.getTimestamp("create_time"));
            notification.setCreator(rs.getString("creator"));
            return notification;
        }
    }
}

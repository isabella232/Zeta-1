package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.exception.EntityIsNullException;
import com.ebay.dss.zds.model.ZetaUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * @author shighuang
 */
@Repository
public class ZetaUserRepository {

    static final String QUERY_GET_USER = "select * from zeta_user where nt=?";
    static final String QUERY_ADD_USER = "insert into zeta_user (" +
            "nt, name, token, td_pass, preference, " +
            "create_dt,update_dt, batch_accounts,github_token, " +
            "windows_auto_account_pass) " +
            "values (?,?,?,?,?,?,?,?,?,?)";
    static final String UNBATCH_ALATION_USERS = "select distinct user.nt from zeta_user user \n" +
            "inner join (select meta.* from alation_integrate_meta meta\n" +
            "left join alation_query_map map on meta.alation_query_id = map.query_id\n" +
            "where meta.status >=0) alation \n" +
            "on user.nt = alation.author";
    static final String QUERY_UPDATE_USER = "update zeta_user set name=:name,token=:token, %s preference=:preference,update_dt=:updateDt where nt=:nt";
    private static final Logger LOG = LogManager.getLogger();
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ZetaUserMapper zetaUserMapper = new ZetaUserMapper();

    @Autowired
    public ZetaUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public ZetaUser getUser(String nt) {
        return jdbcTemplate.queryForObject(QUERY_GET_USER, new Object[]{nt}, zetaUserMapper);
    }

    private void checkUser(ZetaUser user) {
        if (user == null || StringUtils.isEmpty(user.getNt())) {
            throw new EntityIsNullException("Cannot insert null entity into User repository!");
        }
    }

    public List<String> getAllUsers() {
        return jdbcTemplate.queryForList("SELECT nt FROM zeta_user", String.class);
    }

    public List<String> getUnbatchAlationUsers() {
        return jdbcTemplate.queryForList(UNBATCH_ALATION_USERS, String.class);
    }

    public ZetaUser addUser(ZetaUser user) {
        checkUser(user);

        Timestamp ts = new Timestamp(new DateTime().getMillis());
        user.setCreateDt(ts);
        user.setUpdateDt(ts);
        jdbcTemplate.update(QUERY_ADD_USER, user.getNt(), user.getName(), user.getToken(), user.getTdPass(),
                user.getPreference(), user.getCreateDt(), user.getUpdateDt(), user.getBatchAccounts(),
                user.getGithubToken(), user.getWindowsAutoAccountPass());
        return user;
    }

    public ZetaUser updateUser(ZetaUser user) {
        checkUser(user);

        user.setUpdateDt(new Timestamp(new DateTime().getMillis()));
        StringBuffer updateFields = new StringBuffer();
        updateFields.append(Objects.nonNull(user.getWindowsAutoAccountPass()) ? "windows_auto_account_pass=:windowsAutoAccountPass," : "");
        updateFields.append(Objects.nonNull(user.getTdPass()) ? "td_pass=:tdPass," : "");
        updateFields.append(Objects.nonNull(user.getGithubToken()) ? "github_token=:githubToken," : "");
        BeanPropertySqlParameterSource parameters = new BeanPropertySqlParameterSource(user);
        namedParameterJdbcTemplate.update(String.format(QUERY_UPDATE_USER, updateFields.toString()), parameters);
        return user;
    }

    public boolean updateToken(String nt, String token) {
        return jdbcTemplate.update("UPDATE zeta_user SET token = ? WHERE nt = ?", token, nt) > 0;
    }

    public static final class ZetaUserMapper implements RowMapper<ZetaUser> {
        @Override
        public ZetaUser mapRow(ResultSet rs, int i) throws SQLException {
            ZetaUser user = new ZetaUser();
            user.setNt(rs.getString("nt"));
            user.setName(rs.getString("name"));
            user.setToken(rs.getString("token"));
            user.setTdPass(rs.getString("td_pass"));
            user.setPreference(rs.getString("preference"));
            user.setCreateDt(rs.getTimestamp("create_dt"));
            user.setUpdateDt(rs.getTimestamp("update_dt"));
            user.setBatchAccounts(rs.getString("batch_accounts"));
            user.setAdmin(rs.getInt("admin"));
            user.setGithubToken(rs.getString("github_token"));
            user.setWindowsAutoAccountPass(rs.getString("windows_auto_account_pass"));
            user.setAceAdmin(rs.getBoolean("ace_admin"));
            return user;
        }
    }
}

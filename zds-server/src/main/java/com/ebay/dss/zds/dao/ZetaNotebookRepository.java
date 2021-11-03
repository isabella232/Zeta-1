package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.model.ZetaStatus;
import com.ebay.dss.zds.exception.EntityIDIsEmptyException;
import com.ebay.dss.zds.exception.EntityIsNullException;
import com.ebay.dss.zds.exception.InvalidInputException;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.model.ZetaNotebook;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
public class ZetaNotebookRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Deprecated
    public int deleteByPathAndNt(String path, String nt) {
        String delete = ("DELETE FROM zeta_notebook WHERE path = ? AND nt = ?");
        return jdbcTemplate.update(delete, path, nt);
    }

    @Deprecated
    public int deleteRecursivelyByPathAndNt(String path, String nt, String sep) {
        String fuzzyComponent = path.endsWith(sep) ? "%" : "/%";
        String pathAsPrefixPattern = path + fuzzyComponent;
        String delete = ("DELETE FROM zeta_notebook WHERE (path = ? or path like ?) AND nt = ?");
        return jdbcTemplate.update(delete, path, pathAsPrefixPattern, nt);
    }

    public List<String> getNotebookIdRecursivelyByPathAndNt(String path, String nt, String sep) {
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        String fuzzyComponent = path.endsWith(sep) ? "%" : "/%";
        String pathAsPrefixPattern = path + fuzzyComponent;
        String sql = "SELECT ID FROM zeta_notebook WHERE (path = :path or path like :pattern) AND nt = :nt";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("nt", nt);
        paramMap.put("path", path);
        paramMap.put("pattern", pathAsPrefixPattern);
        return jdbc.queryForList(sql, paramMap, String.class);
    }

    public List<String> getNotebookIdByPathAndNt(String path, String nt) {
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        String sql = "DELETE FROM zeta_notebook WHERE path = :path AND nt = :path";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("nt", nt);
        paramMap.put("path", path);
        return jdbc.queryForList(sql, paramMap, String.class);
    }
    public int deleteNotebooksByNtAndIds(List<String> idList, String nt) {
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("nt", nt);
        paramMap.put("ids", idList);
        return jdbc.update("DELETE FROM zeta_notebook WHERE id in (:ids) AND nt = :nt",
            paramMap);
    }
    @Transactional
    public ZetaNotebook getNotebook(String notebookId) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIsNullException("Notebook ID cannot be empty when query!");
        }
//        jdbcTemplate.update("update zeta_notebook set opened=1 where id=?", notebookId);
        return jdbcTemplate.queryForObject("select * from zeta_notebook where id=?", new Object[]{notebookId}, new ZetaNotebookMapper());
    }

    @Transactional
    public ZetaNotebook getNotebook(String notebookId, Integer opened) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIsNullException("Notebook ID cannot be empty when query!");
        }
        if (opened != null) {
            jdbcTemplate.update("update zeta_notebook set opened=? where id=?", opened, notebookId);
        }
        return jdbcTemplate.queryForObject("select * from zeta_notebook where id=?", new Object[]{notebookId}, new ZetaNotebookMapper());
    }

    @Transactional
    public ZetaNotebook searchNotebook(String notebookId) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIsNullException("Notebook ID cannot be empty when query!");
        }
        return jdbcTemplate.queryForObject("select * from zeta_notebook where id=?", new Object[]{notebookId}, new ZetaNotebookMapper());
    }

    @Transactional
    public ZetaNotebook getNotebookByIdAndNt(String notebookId, String nt, Integer opened) {
        if (StringUtils.isEmpty(notebookId) || StringUtils.isEmpty(nt)) {
            throw new EntityIsNullException("Notebook ID or NT cannot be empty when query!");
        }
        if (opened != null) {
            jdbcTemplate.update("update zeta_notebook set opened=? where id=? and nt=?", opened, notebookId, nt);
        }

        return jdbcTemplate.queryForObject("select * from zeta_notebook where id=? and nt=?", new Object[]{notebookId, nt}, new ZetaNotebookMapper());
    }

    public List<ZetaNotebook> getNotebookBriefsByNt(String nt) {
        if (StringUtils.isEmpty(nt)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Nt cannot be empty!");
        }
        return jdbcTemplate.query("select id,title,path,update_dt,create_dt,platform,proxy_user" +
                        ",last_run_dt ,opened,seq, preference,packages,sha,git_repo,status,nb_type, collection_id, public_referred from zeta_notebook where nt=?",
                new Object[]{nt}, new ZetaNotebookBriefMapper());
    }

    public List<ZetaNotebook> getNotebooksByNt(String nt) {
        if (StringUtils.isEmpty(nt)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Nt cannot be empty!");
        }
        return jdbcTemplate.query("select * from zeta_notebook where nt=?",
                new Object[]{nt}, new ZetaNotebookMapper());
    }

    public List<Map<String, Object>> getNotebookContentByIds(String[] notebookIds) {
        if (Objects.isNull(notebookIds) || notebookIds.length == 0) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "notebookIds cannot be empty");
        }
        StringBuilder sqlParams = new StringBuilder();
        for (int i = 0; i < notebookIds.length; i++) {
            sqlParams.append("?,");
        }
        sqlParams.deleteCharAt(sqlParams.length() - 1);
        return jdbcTemplate.queryForList("select title, content from zeta_notebook where id in (" + sqlParams.toString() + ")", notebookIds);
    }

    public List<ZetaNotebook> getOpenedNotebook(String nt) {
        if (StringUtils.isEmpty(nt)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Nt cannot be empty!");
        }
        return jdbcTemplate.query("select * from zeta_notebook where nt=? and opened=1", new Object[]{nt}, new ZetaNotebookMapper());
    }

    public ZetaNotebook getNotebookByNtAndTitle(String nt, String title) {
        if (StringUtils.isEmpty(nt) || StringUtils.isEmpty(title)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Nt or title cannot be empty!");
        }
        return jdbcTemplate.queryForObject("select * from zeta_notebook where nt=? and title=?", new Object[]{nt, title}, new ZetaNotebookMapper());
    }

    public List<String> getNotebooksByNtAndPaths(String nt, List<String> paths) {
        if (StringUtils.isEmpty(nt) || CollectionUtils.isEmpty(paths)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "Nt or paths cannot be empty!");
        }
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("nt", nt);
        paramMap.put("paths", paths);
        return jdbc.queryForList(
                "select CONCAT(path,title)  from zeta_notebook WHERE nt = :nt and CONCAT(path,title) in (:paths);",
                paramMap, String.class);
    }

    public boolean hasNotebook(ZetaNotebook book) {
        String nt = book.getNt();
        String title = book.getTitle();
        String path = book.getPath();
        List<ZetaNotebook> nbs = jdbcTemplate.query("SELECT * FROM zeta_notebook WHERE title = ? AND nt = ? AND path = ?", new Object[]{title, nt, path}, new ZetaNotebookMapper());
        return nbs != null && nbs.size() > 0;
    }

    public boolean isNotebookTitleExist(ZetaNotebook book) {
        String nt = book.getNt();
        String title = book.getTitle();
        String path = book.getPath();
        String id = book.getId();
        List<ZetaNotebook> nbs = jdbcTemplate.query("SELECT * FROM zeta_notebook WHERE title = ? AND nt = ? AND path = ? AND id <> ?", new Object[]{title, nt, path, id}, new ZetaNotebookMapper());
        return nbs != null && nbs.size() > 0;
    }

    public ZetaNotebook addNotebook(ZetaNotebook book) {
        genId(book);
        //use default reference
        book.mergePreference(InterpreterConfiguration.DEFAULT_NOTEBOOK_PREFERENCE);
        jdbcTemplate.update("insert into zeta_notebook (id,nt,content,title, create_dt,update_dt,status, preference, path, platform, proxy_user, last_run_dt, opened,seq, nb_type, collection_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", book.getId(), book.getNt(), book.getContent(), book.getTitle(), book.getCreateDt(), book.getUpdateDt(), book.getStatus(), book.getPreference(), book.getPath(), book.getPlatform(), book.getProxyUser(), book.getLastRunDt(), 0, book.getSeq(), String.valueOf(book.getNbType()), book.getCollectionId());
        return book;
    }

    static ZetaNotebook genId(ZetaNotebook book) {
        if (book == null) {
            throw new EntityIsNullException("Cannot insert NULL entity into Notebook repository!");
        }
        book.setId(UUID.randomUUID().toString());
        Timestamp ts = new Timestamp(new DateTime().getMillis());
        book.setCreateDt(ts);
        book.setUpdateDt(ts);
        return book;
    }

    public void batchAddNotebook(List<ZetaNotebook> books) {
        String sql = "insert into zeta_notebook (id,nt,content,title, create_dt,update_dt,status, preference, path, platform, proxy_user, last_run_dt, opened,seq,sha,git_repo) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new NotebookAddBatchPreparedStatementSetter(books));
    }

    static class NotebookAddBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

        List<ZetaNotebook> books;

        NotebookAddBatchPreparedStatementSetter(List<ZetaNotebook> books) {
            this.books = books;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            ZetaNotebook book = books.get(i);
            genId(book);
            int count = 0;
            ps.setString(++count, book.getId());
            ps.setString(++count, book.getNt());
            ps.setString(++count, book.getContent());
            ps.setString(++count, book.getTitle());
            ps.setTimestamp(++count, book.getCreateDt());
            ps.setTimestamp(++count, book.getUpdateDt());
            ps.setString(++count, book.getStatus());
            ps.setString(++count, book.getPreference());
            ps.setString(++count, book.getPath());
            ps.setString(++count, book.getPlatform());
            ps.setString(++count, book.getProxyUser());
            ps.setTimestamp(++count, book.getLastRunDt());
            ps.setInt(++count, book.getOpened());
            ps.setInt(++count, book.getSeq());
            ps.setString(++count, book.getSha());
            ps.setString(++count, book.getGitRepo());
        }

        @Override
        public int getBatchSize() {
            return books.size();
        }
    }

    public void batchUpdateNotebook(List<ZetaNotebook> books) {
        String sql = "update zeta_notebook set content = ?,sha=?,update_dt=?,git_repo=? where nt = ? and title = ? and path = ?";
        jdbcTemplate.batchUpdate(sql, new NotebookUpdateBatchPreparedStatementSetter(books));
    }

    static class NotebookUpdateBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

        List<ZetaNotebook> books;

        NotebookUpdateBatchPreparedStatementSetter(List<ZetaNotebook> books) {
            this.books = books;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            ZetaNotebook book = books.get(i);
            if (book == null) {
                throw new EntityIsNullException("Cannot update NULL entity into Notebook repository!");
            }
            Timestamp ts = new Timestamp(new DateTime().getMillis());
            book.setUpdateDt(ts);
            int count = 0;
            ps.setString(++count, book.getContent());
            ps.setString(++count, book.getSha());
            ps.setTimestamp(++count, book.getUpdateDt());
            ps.setString(++count, book.getGitRepo());
            ps.setString(++count, book.getNt());
            ps.setString(++count, book.getTitle());
            ps.setString(++count, book.getPath());
        }

        @Override
        public int getBatchSize() {
            return books.size();
        }
    }

    public void batchUpdateNotebookGitInfo(String sha, String gitRepo, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new EntityIsNullException("Notebook id can't be empty!");
        }
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("gitRepo", gitRepo);
        paramMap.put("sha", sha);
        paramMap.put("ids", ids);
        jdbc.update("update zeta_notebook set sha=:sha,git_repo=:gitRepo where id in (:ids);",
                paramMap);
    }

    private static <T> T merge(T a, T b) {
        Field[] allFields = a.getClass().getDeclaredFields();
        for (Field field : allFields) {

            if (!field.isAccessible() && Modifier.isPrivate(field.getModifiers()))
                field.setAccessible(true);
            try {
                if (field.get(b) != null) {
                    field.set(a, field.get(b));
                }
            } catch (IllegalAccessException e) {
                /* Silently fail */
            }
        }
        return a;
    }

    public ZetaNotebook updateNotebook(ZetaNotebook book) {
        if (book == null || StringUtils.isEmpty(book.getId())) {
            throw new EntityIsNullException("Cannot update NULL entity from Notebook repository!");
        }

        ZetaNotebook oldBook = jdbcTemplate.queryForObject("select * from zeta_notebook where id=?", new Object[]{book.getId()}, new ZetaNotebookMapper());
        book.setUpdateDt(new Timestamp(new DateTime().getMillis()));
        book = merge(oldBook, book);

        jdbcTemplate.update("update zeta_notebook set content=?,title=?,status=?,update_dt=?,preference=?,path=?,platform=?,proxy_user=?,seq=? where id=? and nt=?", book.getContent(), book.getTitle(), book.getStatus(), book.getUpdateDt(), book.getPreference(), book.getPath(), book.getPlatform(), book.getProxyUser(), book.getSeq(), book.getId(), book.getNt());
        return book;
    }

    public ZetaNotebook moveNotebook(ZetaNotebook book) {
        if (book == null || StringUtils.isEmpty(book.getId())) {
            throw new EntityIsNullException("Cannot update NULL entity from Notebook repository!");
        }
        book.setUpdateDt(new Timestamp(new DateTime().getMillis()));

        jdbcTemplate.update("update zeta_notebook set title=?,update_dt=?,path=? where id=? and nt=?", book.getTitle(), book.getUpdateDt(), book.getPath(), book.getId(), book.getNt());
        return book;
    }
    public ZetaNotebook forceUpdateNotebook(ZetaNotebook book) {
        if (book == null || StringUtils.isEmpty(book.getId())) {
            throw new EntityIsNullException("Cannot update NULL entity from Notebook repository!");
        }

        jdbcTemplate.update("update zeta_notebook set content=?,title=?,status=?,update_dt=?,create_dt=?,preference=?,path=?,platform=?,proxy_user=?,seq=? where id=? and nt=?", book.getContent(), book.getTitle(), book.getStatus(), book.getUpdateDt(),book.getCreateDt(), book.getPreference(), book.getPath(), book.getPlatform(), book.getProxyUser(), book.getSeq(), book.getId(), book.getNt());
        return book;
    }

    public ZetaNotebook refreshLastDt(ZetaNotebook book) {
        if (book == null || StringUtils.isEmpty(book.getId())) {
            throw new EntityIsNullException("Cannot update NULL entity from Notebook repository!");
        }
        jdbcTemplate.update("update zeta_notebook set update_dt=?,last_run_dt=? where id=? and nt=?", book.getLastRunDt(), book.getLastRunDt(), book.getId(), book.getNt());
        return book;
    }

    public int updateNotebookStateByIdAndNt(String notebookId, String status, String nt) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }
        return jdbcTemplate.update("update zeta_notebook set status=?,update_dt=? where id=? and nt =?", status, new Timestamp(new DateTime().getMillis()), notebookId, nt);
    }

    public int updateNotebookPreferenceByIdAndNt(String notebookId, String preference, String nt) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }
        return jdbcTemplate.update("update zeta_notebook set preference=?, update_dt=? where id=? and nt=?", preference, new Timestamp(new DateTime().getMillis()), notebookId, nt);
    }

    public int updateNotebookPreferenceById(String notebookId, String preference) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }
        return jdbcTemplate.update("update zeta_notebook set preference=?, update_dt=? where id=? ", preference, new Timestamp(new DateTime().getMillis()), notebookId);
    }

    public int updateNotebookPackagesByIdAndNt(String notebookId, String packages,String nt) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }
        return jdbcTemplate.update("update zeta_notebook set packages=?, update_dt=? where id=? and nt=? ", packages, new Timestamp(new DateTime().getMillis()), notebookId,nt);
    }

    public int updateNotebookLastRunDtByIdAndNt(String notebookId, String nt) {
        return jdbcTemplate.update("update zeta_notebook set last_run_dt=? where id=? and nt=?", new Timestamp(new DateTime().getMillis()), notebookId, nt);
    }

    public int updateNotebookOpenedStatusByIdAsClose(String nt, String notebookId) {
        return jdbcTemplate.update("update zeta_notebook set opened=0,seq=-1 where id=? and nt=?", notebookId, nt);
    }

    public int updateNotebookSeqById(String nt, String notebookId, int seq) {
        return jdbcTemplate.update("update zeta_notebook set seq=? where id=? and nt=?", seq, notebookId, nt);
    }

    public List<Map<String, Object>> getNotebookHistoryByIdAndNt(String notebookId, String nt) {
        return getNotebookHistoryByIdAndNt(notebookId, nt, 200);
    }

    public List<Map<String, Object>> getNotebookHistoryByIdAndNt(String notebookId, String nt, int limit) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }

        return jdbcTemplate.queryForList("select st.id, st.request_id, st.statement, jr.livy_job_url, jr.preference, st.seq, st.start_dt, st.update_dt,st.status " +
                "from zeta_statement as st join zeta_job_request as jr on jr.id=st.request_id join zeta_notebook n on jr.notebook_id = n.id " +
                "where n.id=? order by st.update_dt desc limit " + limit, notebookId);
    }

    public List<Map<String, Object>> getNotebookHistoryByIdAndNt(String notebookId) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }

        return jdbcTemplate.queryForList("select st.id, st.request_id, st.statement, jr.livy_job_url, st.seq, st.start_dt, st.update_dt,st.status from zeta_statement as st join zeta_job_request as jr on jr.id=st.request_id join zeta_notebook n on jr.notebook_id = n.id where n.id=?", notebookId);
    }

    public List<Map<String, Object>> getMultiNotebookLastRequestByIdAndNt(String notebookId) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }
        return jdbcTemplate.queryForList("select * from (select jr.id, jr.notebook_id from  zeta_job_request jr join zeta_notebook n on jr.notebook_id = n.id where n.collection_id=? order by jr.id desc) a group by a.notebook_id", notebookId);
    }

    public List<Map<String, Object>> getNotebookHistoryByRequestId(String requestId) {
        if (StringUtils.isEmpty(requestId)) {
            throw new EntityIDIsEmptyException("Request ID is NULL");
        }

        return jdbcTemplate.queryForList("select st.id, st.request_id, st.statement, jr.livy_job_url, st.seq, st.start_dt, st.update_dt,st.status from zeta_statement as st join zeta_job_request as jr on jr.id=st.request_id join zeta_notebook n on jr.notebook_id = n.id where jr.id=?", requestId);
    }

    public List<Map<String, Object>> getNotebookFailedHistoryByRequestId(String requestId) {
        if (StringUtils.isEmpty(requestId)) {
            throw new EntityIDIsEmptyException("Request ID is NULL");
        }

        return jdbcTemplate.queryForList("select st.id, st.request_id, st.statement, st.result, jr.livy_job_url, " +
            "st.seq, st.start_dt, st.update_dt from zeta_statement as st join zeta_job_request as jr on jr.id=st.request_id " +
            "join zeta_notebook n on jr.notebook_id = n.id where st.status = 4001 and jr.id=? order by st.id desc", requestId);
    }

    public int deleteNotebookByIdAndNt(String notebookId, String nt) {
        if (StringUtils.isEmpty(notebookId)) {
            throw new EntityIDIsEmptyException("Notebook ID is NULL");
        }
        return jdbcTemplate.update("delete from zeta_notebook where id=? and nt=?", notebookId, nt);
    }

    public int deleteNotebookByNt(String nt) {
        return jdbcTemplate.update("delete from zeta_notebook where nt=?", nt);
    }


    public int deleteNotebooksByCollectionIdAndNt(String collectionId, String nt) {
        return jdbcTemplate.update("delete from zeta_notebook where collection_id = ? and nt = ?", collectionId, nt);
    }

    public List<ZetaNotebook> getSubNotebooksBydNt(String nt) {
        return jdbcTemplate.query("select * from zeta_notebook where nt = ? and nb_type='sub_nb'", new Object[]{nt}, new ZetaNotebookMapper());
    }

    public List<ZetaNotebook> getNotebooksByCollectionIdAndNt(String collectionId, String nt) {
        return jdbcTemplate.query("select * from zeta_notebook where collection_id = ? and nt = ?", new Object[]{collectionId, nt}, new ZetaNotebookMapper());
    }

    public List<ZetaNotebook> getFavoriteShareNotebook(String nt) {
        String sql = "select nb.* from zeta_notebook nb inner join zeta_favorite f on nb.id = f.id and f.favorite_type = 'share_nb' where f.nt = ? and f.favorite = '1'";
        return jdbcTemplate.query(sql, new Object[]{nt}, new ZetaNotebookMapper());
    }

    static class ZetaNotebookMapper implements RowMapper<ZetaNotebook> {

        @Override
        public ZetaNotebook mapRow(ResultSet rs, int i) throws SQLException {
            ZetaNotebook book = new ZetaNotebook();
            book.setId(rs.getString("id"));
            book.setNt(rs.getString("nt"));
            book.setContent(rs.getString("content"));
            book.setCreateDt(rs.getTimestamp("create_dt"));
            book.setUpdateDt(rs.getTimestamp("update_dt"));
            book.setTitle(rs.getString("title"));
            book.setStatus(rs.getString("status"));
            book.setPreference(rs.getString("preference"));
            book.setPackages(rs.getString("packages"));
            book.setPath(rs.getString("path"));
            book.setPlatform(rs.getString("platform"));
            book.setProxyUser(rs.getString("proxy_user"));
            book.setLastRunDt(rs.getTimestamp("last_run_dt"));
            book.setOpened(rs.getInt("opened"));
            book.setSeq(rs.getInt("seq"));
            book.setPublicRole(ZetaNotebook.PublicRole.valueOf(rs.getString("public_role")));
            book.setPublicReferred(rs.getString("public_referred"));
            book.setNbType(ZetaNotebook.NotebookType.valueOf(rs.getString("nb_type")));
            book.setCollectionId(rs.getString("collection_id"));
            return book;
        }
    }

    static class ZetaNotebookBriefMapper implements RowMapper<ZetaNotebook> {

        @Override
        public ZetaNotebook mapRow(ResultSet rs, int i) throws SQLException {
            ZetaNotebook book = new ZetaNotebook();
            book.setId(rs.getString("id"));
            book.setTitle(rs.getString("title"));
            book.setPath(rs.getString("path"));
            book.setCreateDt(rs.getTimestamp("create_dt"));
            book.setUpdateDt(rs.getTimestamp("update_dt"));
            book.setPlatform(rs.getString("platform"));
            book.setProxyUser(rs.getString("proxy_user"));
            book.setLastRunDt(rs.getTimestamp("last_run_dt"));
            book.setOpened(rs.getInt("opened"));
            book.setSeq(rs.getInt("seq"));
            book.setPreference(rs.getString("preference"));
            book.setPackages(rs.getString("packages"));
            book.setSha(rs.getString("sha"));
            book.setGitRepo(rs.getString("git_repo"));
            book.setStatus(rs.getString("status"));
            book.setNbType(ZetaNotebook.NotebookType.valueOf(rs.getString("nb_type")));
            book.setCollectionId(rs.getString("collection_id"));
            book.setPublicReferred(rs.getString(("public_referred")));
            return book;
        }
    }

}

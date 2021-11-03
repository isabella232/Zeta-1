package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.alation.AlationQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlationLogRepository {
    @Autowired
    private NamedParameterJdbcTemplate template;

    public void insertToLog(AlationQuery query) {
//        (`id`, `query_id`, `author`, `datasource_id`, `content`, `autosaved_content`, `crt_dt`)
        String sql = "INSERT INTO `alation_query_log` (`id`, `query_id`, `author`, `datasource_id`, `content`, `autosaved_content`, `crt_dt`, `published`,`description`) \n" +
            "VALUES\n" +
            "\t(NULL, :query_id, :author, :datasource_id, :content, :autosaved_content, now(), :published, :description);\n";
        MapSqlParameterSource ps = new MapSqlParameterSource();
        ps.addValue("query_id", query.getId())
            .addValue("author", query.getAuthor())
            .addValue("datasource_id", query.getDatasource_id())
            .addValue("content", query.getContent())
            .addValue("autosaved_content", query.getAutosave_content())
            .addValue("published", query.isPublished() ? 1 : 0)
            .addValue("description", query.getDescription());
        template.update(sql, ps);
    }
}

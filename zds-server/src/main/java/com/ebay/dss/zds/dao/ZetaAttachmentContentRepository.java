package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaAttachmentContent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ZetaAttachmentContentRepository {

    private JdbcTemplate jdbcTemplate;

    public ZetaAttachmentContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ZetaAttachmentContent> findContent(long id) {
        LobHandler lobHandler = new DefaultLobHandler();
        ZetaAttachmentContent content = jdbcTemplate.queryForObject(
                "select content from zeta_attachment where id = ?",
                new Object[]{id},
                (resultSet, i) -> {
                    ZetaAttachmentContent tmp = new ZetaAttachmentContent();
                    tmp.setInputStream(lobHandler.getBlobAsBinaryStream(resultSet, "content"));
                    return tmp;
                });
        return Optional.ofNullable(content);
    }

    public int save(ZetaAttachmentContent content) {
        return jdbcTemplate.update(
                "update zeta_attachment set content = ? where id = ?",
                ps -> {
                    ps.setBlob(1, content.getInputStream());
                    ps.setLong(2, content.getId());
                }
        );
    }
}

package com.ebay.dss.zds.endpoints;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.stereotype.Component;


@WebEndpoint(id = "datasource")
@Component
public class DataSourceController {

    private final static String zetaDscEntryName = "zetamyhost";
    private final HikariDataSource dataSource;

    public DataSourceController(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @WriteOperation
    public void updateDataSource(String dscEntryName,
                                 String url,
                                 String user,
                                 String password) {
        if (StringUtils.equals(zetaDscEntryName, dscEntryName) &&
                StringUtils.isNotBlank(url) &&
                StringUtils.isNotBlank(user) &&
                StringUtils.isNotBlank(password)) {
            dataSource.setJdbcUrl(url);
            dataSource.setUsername(user);
            dataSource.setPassword(password);
        }
    }

}

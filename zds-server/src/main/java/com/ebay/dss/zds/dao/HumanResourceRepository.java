package com.ebay.dss.zds.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class HumanResourceRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    static final String QUERY_USERS = "select nt_login from game.game_hr_t where nt_login in ";

    public List<String> getUsersInfo(Set<String> ntList) {
        String users = ntList.stream().filter(Objects::nonNull).map(user -> "'" + user + "'").collect(Collectors.joining(",", "(", ")"));
        return jdbcTemplate.queryForList(QUERY_USERS + users, String.class);
    }

}

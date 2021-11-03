/***
*SQLs to create Zeta tables in MySQL
*/

USE zetadb;
SET SQL_MODE=ANSI_QUOTES;

CREATE TABLE "zeta_user"
(
    "nt"                        varchar(100) NOT NULL,
    "name"                      varchar(200) NOT NULL,
    "token"                     varchar(500)          DEFAULT NULL,
    "preference"                mediumtext,
    "create_dt"                 datetime              DEFAULT NULL,
    "update_dt"                 datetime              DEFAULT NULL,
    "td_pass"                   varchar(1024)         DEFAULT NULL,
    "batch_accounts"            varchar(1024)         DEFAULT NULL,
    "admin"                     int(11)      NOT NULL DEFAULT '0' COMMENT 'select 101 mod 10 as `metadata_admin`, (111 div 10) mod 10 as `public_notebook_admin`',
    "github_token"              varchar(1024)         DEFAULT NULL,
    "windows_auto_account_pass" varchar(500)          DEFAULT NULL,
    "ace_admin"                 tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY ("nt")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_notebook"
(
    "id"              char(36) CHARACTER SET utf8                              NOT NULL,
    "nt"              varchar(100) CHARACTER SET utf8                          NOT NULL,
    "title"           varchar(200) COLLATE utf8_bin                            NOT NULL,
    "content"         mediumtext CHARACTER SET utf8,
    "create_dt"       datetime                                                          DEFAULT NULL,
    "update_dt"       datetime                                                          DEFAULT NULL,
    "status"          char(10) CHARACTER SET utf8                                       DEFAULT NULL,
    "preference"      mediumtext CHARACTER SET utf8,
    "packages"        mediumtext CHARACTER SET utf8,
    "path"            varchar(100) COLLATE utf8_bin                                     DEFAULT NULL,
    "platform"        varchar(100) CHARACTER SET utf8                                   DEFAULT NULL,
    "proxy_user"      varchar(100) CHARACTER SET utf8                                   DEFAULT NULL,
    "last_run_dt"     datetime                                                          DEFAULT NULL,
    "opened"          tinyint(1)                                                        DEFAULT NULL,
    "seq"             int(11)                                                           DEFAULT NULL,
    "public_role"     enum ('no_pub','pub','ref') CHARACTER SET utf8           NOT NULL DEFAULT 'no_pub' COMMENT '"no_pub" means this notebook is a private notebook; "pub" means this notebook is opened to all users and owned by current user, "ref" means this notebook refers a public notebook, the referred id is placed in "public_referred" column',
    "public_referred" char(36) CHARACTER SET utf8                                       DEFAULT NULL COMMENT 'logically should be null when "public_role" is "no_pub" or "pub", and when "ref" should contain an existed notebook id whose "public_role" is "pub"',
    "sha"             varchar(255) CHARACTER SET utf8                                   DEFAULT NULL,
    "git_repo"        varchar(255) CHARACTER SET utf8                                   DEFAULT NULL,
    "nb_type"         enum ('single','collection','sub_nb') CHARACTER SET utf8 NOT NULL DEFAULT 'single',
    "collection_id"   char(36) CHARACTER SET utf8                                       DEFAULT NULL,
    PRIMARY KEY ("id"),
    UNIQUE KEY "nt_title_path" ("nt", "title", "path"),
    KEY "public_referred" ("public_referred"),
    CONSTRAINT "zeta_notebook_ibfk_1" FOREIGN KEY ("nt") REFERENCES "zeta_user" ("nt") ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT "zeta_notebook_ibfk_2" FOREIGN KEY ("public_referred") REFERENCES "zeta_notebook" ("id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin;


CREATE TABLE "zeta_job_request"
(
    "id"              bigint(20) NOT NULL AUTO_INCREMENT,
    "notebook_id"     char(36)   NOT NULL,
    "content"         mediumtext NOT NULL,
    "create_dt"       datetime     DEFAULT NULL,
    "start_dt"        datetime     DEFAULT NULL,
    "update_dt"       datetime     DEFAULT NULL,
    "livy_session_id" int(11)      DEFAULT NULL,
    "livy_job_url"    varchar(500) DEFAULT NULL,
    "status"          char(4)      DEFAULT NULL,
    "preference"      mediumtext,
    "proxy_user"      varchar(100) DEFAULT NULL,
    "platform"        varchar(100) DEFAULT NULL,
    PRIMARY KEY ("id"),
    KEY "notebook_id_index" ("notebook_id"),
    CONSTRAINT "zeta_job_request_ibfk_1" FOREIGN KEY ("notebook_id") REFERENCES "zeta_notebook" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_statement"
(
    "id"                bigint(20)  NOT NULL AUTO_INCREMENT,
    "request_id"        bigint(20)  NOT NULL,
    "statement"         mediumtext  NOT NULL,
    "result"            mediumtext CHARACTER SET utf8mb4,
    "plot_config"       mediumtext,
    "create_dt"         datetime DEFAULT NULL,
    "start_dt"          datetime DEFAULT NULL,
    "update_dt"         datetime DEFAULT NULL,
    "seq"               smallint(6) NOT NULL,
    "status"            char(4)  DEFAULT NULL,
    "livy_session_id"   int(11)  DEFAULT NULL,
    "livy_statement_id" int(11)  DEFAULT NULL,
    "progress"          int(11)  DEFAULT '0',
    PRIMARY KEY ("id"),
    KEY "request_id_index" ("request_id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_dashboard"
(
    "id"            bigint(20)   NOT NULL AUTO_INCREMENT,
    "nt"            varchar(100) NOT NULL,
    "name"          varchar(255) NOT NULL,
    "path"          varchar(255) DEFAULT NULL,
    "layout_config" mediumtext,
    "create_dt"     datetime     DEFAULT NULL,
    "update_dt"     datetime     DEFAULT NULL,
    PRIMARY KEY ("id"),
    UNIQUE KEY "dashboard_name_x" ("nt", "name", "path"),
    KEY "dashboard_nt_index" ("nt")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "sql_convert_history"
(
    "id"       int(11) NOT NULL AUTO_INCREMENT,
    "nt"       varchar(255) DEFAULT NULL,
    "content"  mediumtext,
    "type"     int(11)      DEFAULT NULL COMMENT '0:table,1:sql',
    "cre_date" datetime     DEFAULT NULL,
    "platform" varchar(10)  DEFAULT NULL,
    "status"   varchar(10)  DEFAULT NULL,
    "error"    mediumtext,
    PRIMARY KEY ("id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "schedule_job"
(
    "id"                int(11)       NOT NULL AUTO_INCREMENT,
    "job_name"          varchar(255)           DEFAULT NULL,
    "nt"                varchar(255)           DEFAULT NULL,
    "schedule_time"     text,
    "cron_expression"   varchar(255)           DEFAULT NULL,
    "type"              varchar(255)           DEFAULT NULL,
    "task"              longtext,
    "dependency"        longtext,
    "dependency_signal" longtext,
    "status"            int(2)                 DEFAULT NULL COMMENT '0: inactive 1: active',
    "cc_addr"           longtext,
    "last_run_time"     datetime               DEFAULT NULL,
    "next_run_time"     datetime               DEFAULT NULL,
    "create_time"       datetime               DEFAULT NULL,
    "update_time"       datetime               DEFAULT NULL,
    "auth_info"         varchar(9999) NOT NULL DEFAULT '{"OWNERS":[],"READERS":[],"WRITERS":[]}',
    "mail_switch"       tinyint(4)    NOT NULL DEFAULT '1',
    PRIMARY KEY ("id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "schedule_history"
(
    "id"             int(11)                                                          NOT NULL AUTO_INCREMENT,
    "job_id"         int(11)                                                                   DEFAULT NULL,
    "job_history_id" int(11)                                                                   DEFAULT NULL,
    "log"            longtext,
    "start_time"     datetime                                                                  DEFAULT NULL,
    "end_time"       datetime                                                                  DEFAULT NULL,
    "job_run_status" enum ('PENDING','WAITING','RUNNING','CANCELED','SUCCESS','FAIL') NOT NULL DEFAULT 'PENDING',
    "job_operation"  enum ('SKIP','CANCEL','RUN')                                              DEFAULT NULL,
    "run_time"       datetime                                                                  DEFAULT NULL,
    PRIMARY KEY ("id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_cache"
(
    "key_name"  varchar(100) NOT NULL DEFAULT '',
    "value"     mediumtext   NOT NULL,
    "create_dt" datetime              DEFAULT NULL,
    "update_dt" datetime              DEFAULT NULL,
    PRIMARY KEY ("key_name")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_exception_rule"
(
    "id"           int(11) unsigned NOT NULL AUTO_INCREMENT,
    "order"        int(11) unsigned NOT NULL,
    "filter"       varchar(500)     NOT NULL,
    "message"      varchar(500)     NOT NULL,
    "error_code"   varchar(200)     NOT NULL,
    "create_dt"    datetime                  DEFAULT NULL,
    "update_dt"    datetime                  DEFAULT NULL,
    "regex"        text,
    "message_only" tinyint(1)       NOT NULL DEFAULT '1',
    PRIMARY KEY ("id"),
    UNIQUE KEY "excp_rule_order_x" ("order")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_email"
(
    "id"          int(11) unsigned NOT NULL AUTO_INCREMENT,
    "job_id"      varchar(100)     NOT NULL,
    "email_id"    int(11)     DEFAULT NULL,
    "status"      varchar(20)      NOT NULL,
    "error_log"   text,
    "template"    varchar(50) DEFAULT NULL,
    "create_time" datetime         NOT NULL,
    PRIMARY KEY ("id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_email_unsubscribe"
(
    "unsubscribe_id" varchar(100) NOT NULL DEFAULT '',
    "nt"             mediumtext   NOT NULL,
    "create_time"    datetime     NOT NULL,
    PRIMARY KEY ("unsubscribe_id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE "zeta_livy_event_fact"
(
    "id"                        bigint(20)   NOT NULL AUTO_INCREMENT,
    "nt"                        varchar(100) NOT NULL,
    "proxy_user"                varchar(100) NOT NULL,
    "cluster_id"                int(11)      NOT NULL COMMENT '2: areslvs, 3: apollophx, 10: herculeslvs, 11: herculessublvs, 14: apollorno',
    "targetQueue"               varchar(200) DEFAULT NULL,
    "endpoint"                  text,
    "notebook_id"               varchar(100) NOT NULL,
    "interpreter_class"         varchar(500) DEFAULT NULL,
    "usingConnectionQueue"      int(2)       NOT NULL COMMENT '0: false 1: true',
    "connectionQueueSuccess"    int(2)       DEFAULT NULL COMMENT '0: false 1: true',
    "connectionQueueFailReason" text,
    "connectionQueueCost"       bigint(20)   DEFAULT NULL,
    "totalCost"                 bigint(20)   NOT NULL,
    "success"                   int(2)       NOT NULL COMMENT '0: false 1: true',
    "reason"                    text,
    "create_dt"                 datetime     NOT NULL,
    PRIMARY KEY ("id")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


-- auto increment start at 10000, for id before it is reserved for build-in faq
CREATE TABLE "zeta_ace_question"
(
    "id"          int(11)       NOT NULL AUTO_INCREMENT,
    "submitter"   varchar(100)  NOT NULL,
    "create_time" datetime      NOT NULL,
    "update_time" datetime      NOT NULL,
    "title"       varchar(1024) NOT NULL,
    "content"     text,
    "editor"      varchar(100)           DEFAULT NULL,
    "edit_time"   datetime               DEFAULT NULL,
    "pick_up"     int(11)       NOT NULL DEFAULT '0',
    PRIMARY KEY ("id"),
    KEY "submitter" ("submitter"),
    KEY "editor" ("editor"),
    KEY "pick_up" ("pick_up"),
    CONSTRAINT "zeta_ace_question_ibfk_1" FOREIGN KEY ("submitter") REFERENCES "zeta_user" ("nt"),
    CONSTRAINT "zeta_ace_question_ibfk_2" FOREIGN KEY ("editor") REFERENCES "zeta_user" ("nt")
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  AUTO_INCREMENT = 10000;


CREATE TABLE "zeta_ace_question_post" (
                                          "id" int(11) NOT NULL AUTO_INCREMENT,
                                          "question_id" int(11) NOT NULL,
                                          "poster" varchar(100) NOT NULL,
                                          "reply_to" int(11) DEFAULT NULL,
                                          "comment" text CHARACTER SET utf8mb4 NOT NULL,
                                          "create_time" datetime NOT NULL,
                                          "update_time" datetime NOT NULL,
                                          "accepted" tinyint(1) NOT NULL DEFAULT '0',
                                          "editor" varchar(100) DEFAULT NULL,
                                          "edit_time" datetime DEFAULT NULL,
                                          PRIMARY KEY ("id"),
                                          KEY "question_id" ("question_id"),
                                          KEY "poster" ("poster"),
                                          KEY "reply_to" ("reply_to"),
                                          KEY "editor" ("editor"),
                                          CONSTRAINT "zeta_ace_question_post_ibfk_1" FOREIGN KEY ("question_id") REFERENCES "zeta_ace_question" ("id"),
                                          CONSTRAINT "zeta_ace_question_post_ibfk_2" FOREIGN KEY ("poster") REFERENCES "zeta_user" ("nt"),
                                          CONSTRAINT "zeta_ace_question_post_ibfk_3" FOREIGN KEY ("reply_to") REFERENCES "zeta_ace_question_post" ("id"),
                                          CONSTRAINT "zeta_ace_question_post_ibfk_4" FOREIGN KEY ("editor") REFERENCES "zeta_user" ("nt")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE "zeta_ace_question_like" (
                                          "id" bigint(20) NOT NULL AUTO_INCREMENT,
                                          "nt" varchar(100) NOT NULL,
                                          "question_id" int(11) NOT NULL,
                                          "flag" tinyint(4) NOT NULL DEFAULT '0',
                                          "create_time" datetime NOT NULL,
                                          "update_time" datetime NOT NULL,
                                          PRIMARY KEY ("id"),
                                          UNIQUE KEY "nt_2" ("nt","question_id"),
                                          KEY "nt" ("nt"),
                                          KEY "question_id" ("question_id"),
                                          CONSTRAINT "zeta_ace_question_like_ibfk_1" FOREIGN KEY ("nt") REFERENCES "zeta_user" ("nt"),
                                          CONSTRAINT "zeta_ace_question_like_ibfk_2" FOREIGN KEY ("question_id") REFERENCES "zeta_ace_question" ("id")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;;


CREATE TABLE "zeta_ace_post_like" (
                                      "id" bigint(20) NOT NULL AUTO_INCREMENT,
                                      "nt" varchar(100) NOT NULL,
                                      "question_id" int(11) NOT NULL,
                                      "post_id" int(11) NOT NULL,
                                      "flag" tinyint(4) NOT NULL DEFAULT '0',
                                      "create_time" datetime NOT NULL,
                                      "update_time" datetime NOT NULL,
                                      PRIMARY KEY ("id"),
                                      UNIQUE KEY "nt_2" ("nt","question_id","post_id"),
                                      KEY "nt" ("nt"),
                                      KEY "question_id" ("question_id"),
                                      KEY "post_id" ("post_id"),
                                      CONSTRAINT "zeta_ace_post_like_ibfk_1" FOREIGN KEY ("nt") REFERENCES "zeta_user" ("nt"),
                                      CONSTRAINT "zeta_ace_post_like_ibfk_2" FOREIGN KEY ("question_id") REFERENCES "zeta_ace_question" ("id"),
                                      CONSTRAINT "zeta_ace_post_like_ibfk_3" FOREIGN KEY ("post_id") REFERENCES "zeta_ace_question_post" ("id")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;;


CREATE TABLE "zeta_ace_tag" (
                                "id" int(11) NOT NULL AUTO_INCREMENT,
                                "name" varchar(35) NOT NULL,
                                "nt" varchar(100) NOT NULL,
                                "description" varchar(2048) DEFAULT NULL,
                                "create_time" datetime DEFAULT NULL,
                                "update_time" datetime NOT NULL DEFAULT '2020-01-01 00:00:00',
                                PRIMARY KEY ("id"),
                                UNIQUE KEY "name" ("name"),
                                KEY "nt" ("nt"),
                                KEY "create_time" ("create_time")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE "zeta_attachment" (
                                   "id" bigint(20) NOT NULL AUTO_INCREMENT,
                                   "content_type" varchar(32) NOT NULL,
                                   "file_name" varchar(32) NOT NULL,
                                   "file_extension" varchar(16) NOT NULL,
                                   "original_file_name" varchar(128) NOT NULL,
                                   "content" mediumblob,
                                   "compress_alg" varchar(32) NOT NULL,
                                   "decompress_alg" varchar(32) NOT NULL,
                                   "raw_size" bigint(20) NOT NULL,
                                   "checksum" varchar(64) NOT NULL,
                                   "checksum_alg" varchar(32) NOT NULL,
                                   "uploader" varchar(100) NOT NULL,
                                   "create_time" datetime NOT NULL,
                                   PRIMARY KEY ("id"),
                                   UNIQUE KEY "file_name" ("file_name","checksum"),
                                   KEY "uploader" ("uploader"),
                                   KEY "checksum" ("checksum")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


create table zeta_ace_tag
(
    id          int primary key auto_increment,
    name        varchar(35)  not null unique,
    nt          varchar(100) not null,
    description varchar(2048),
    create_time datetime,
    index (nt),
    index (create_time)
) auto_increment = 10000;


CREATE TABLE "zeta_ace_question_tag" (
                                         "question_id" int(11) NOT NULL,
                                         "tag_id" int(11) NOT NULL,
                                         "create_time" datetime DEFAULT NULL,
                                         PRIMARY KEY ("question_id","tag_id"),
                                         KEY "create_time" ("create_time")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE "zeta_favorite" (
                                 "id" varchar(100) NOT NULL DEFAULT '',
                                 "nt" varchar(100) NOT NULL,
                                 "favorite_type" enum('nb','pub_nb','share_nb','share_zpl_nb','schedule','statement') NOT NULL DEFAULT 'nb',
                                 "update_dt" datetime DEFAULT NULL,
                                 "favorite" enum('0','1') NOT NULL DEFAULT '1',
                                 PRIMARY KEY ("id","nt","favorite_type")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE "alation_integrate_meta" (
                                          "alation_query_id" int(11) unsigned NOT NULL,
                                          "author" varchar(50) NOT NULL DEFAULT '',
                                          "crt_dt" datetime DEFAULT NULL,
                                          "crt_by" varchar(50) DEFAULT '',
                                          "upd_dt" datetime DEFAULT NULL,
                                          "status" int(11) DEFAULT '0',
                                          "schedule_status" int(11) DEFAULT '0',
                                          "published_status" int(11) DEFAULT '0',
                                          PRIMARY KEY ("alation_query_id")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE "alation_query_map" (
                                     "query_id" int(36) NOT NULL,
                                     "notebook_id" char(36) NOT NULL,
                                     "schedule_id" int(11) DEFAULT NULL,
                                     PRIMARY KEY ("query_id","notebook_id")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE "zeta_meta_table" (
                                   "id" char(36) CHARACTER SET utf8mb4 NOT NULL,
                                   "meta_table_name" varchar(200) CHARACTER SET utf8mb4 NOT NULL,
                                   "nt" varchar(50) CHARACTER SET utf8mb4 NOT NULL,
                                   "platform" varchar(30) CHARACTER SET utf8mb4 DEFAULT NULL,
                                   "account" varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL,
                                   "db" varchar(100) CHARACTER SET utf8mb4 DEFAULT NULL,
                                   "tbl" varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL,
                                   "schema_info" longtext CHARACTER SET utf8mb4 NOT NULL,
                                   "hadoop_schema_info" longtext CHARACTER SET utf8mb4,
                                   "auth_info" longtext CHARACTER SET utf8mb4 NOT NULL,
                                   "meta_table_type" enum('HDM','PROD') CHARACTER SET utf8mb4 DEFAULT NULL,
                                   "cron" mediumtext CHARACTER SET utf8mb4,
                                   "meta_table_status" enum('CREATED','SYNCING','SUCCESS','REGISTER_FAIL','LOAD_FAIL','DELETED') CHARACTER SET utf8mb4 NOT NULL DEFAULT 'CREATED',
                                   "create_time" datetime NOT NULL,
                                   "update_time" datetime NOT NULL,
                                   "sync_time" datetime DEFAULT NULL,
                                   "fail_log" longtext CHARACTER SET utf8mb4,
                                   "last_modifier" varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL,
                                   "path" longtext COLLATE utf8_bin
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


CREATE TABLE "zeta_workspace_note" (
                                       "id" int(11) NOT NULL AUTO_INCREMENT,
                                       "nt" varchar(50) COLLATE utf8_bin NOT NULL,
                                       "note_id" varchar(50) COLLATE utf8_bin NOT NULL,
                                       "opened" tinyint(4) NOT NULL,
                                       "seq" int(11) NOT NULL,
                                       PRIMARY KEY ("id")
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


CREATE TABLE "zeta_workspace_instance" (
                                           "id" varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
                                           "seq" int(11) NOT NULL,
                                           "nt" varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
                                           "type" varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT 'NOTEBOOK',
                                           "create_time" datetime DEFAULT CURRENT_TIMESTAMP,
                                           "update_time" datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                           PRIMARY KEY ("id","nt","type")
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE "zeta_sheet_whitelist" (
                                        "id" tinyint(4) NOT NULL AUTO_INCREMENT,
                                        "nt" varchar(50) COLLATE utf8_bin NOT NULL,
                                        PRIMARY KEY ("id"),
                                        UNIQUE KEY "nt" ("nt")
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


CREATE TABLE "zeta_operation_log" (
                                      "id" int(11) NOT NULL AUTO_INCREMENT,
                                      "operation_id" varchar(50) COLLATE utf8_bin NOT NULL,
                                      "nt" varchar(50) COLLATE utf8_bin NOT NULL,
                                      "operation_type" enum('SCHEDULER') CHARACTER SET utf8mb4 NOT NULL DEFAULT 'SCHEDULER',
                                      "operation_interface" longtext CHARACTER SET utf8mb4,
                                      "description" longtext CHARACTER SET utf8mb4,
                                      "create_time" datetime DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY ("id")
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


CREATE TABLE "zeta_query_logs" (
                                   "id" bigint(20) DEFAULT NULL,
                                   "request_id" bigint(20) DEFAULT NULL,
                                   "statement" mediumtext CHARACTER SET utf8 NOT NULL,
                                   "create_dt" datetime DEFAULT NULL,
                                   "start_dt" datetime DEFAULT NULL,
                                   "update_dt" datetime DEFAULT NULL,
                                   "seq" smallint(6) NOT NULL,
                                   "status" char(4) CHARACTER SET utf8 DEFAULT NULL,
                                   "dt" date DEFAULT NULL,
                                   UNIQUE KEY "id" ("id"),
                                   KEY "query_id_index" ("id")
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

alter table schedule_job
    add column fail_times_to_block int(11) not null default '0';
alter table schedule_job
    add column auto_retry tinyint(1) not null default '0';

ALTER TABLE  `schedule_history`  MODIFY COLUMN `job_run_status`
enum('PENDING','WAITING','RUNNING','CANCELED','SUCCESS','FAIL','AUTORETRY','AUTORETRYWAITING') NOT NULL DEFAULT 'WAITING';
ALTER TABLE zeta_operation_log
    add column `comments` longtext CHARACTER SET utf8mb4;

# alation migration related
create table zeta_alation_article
(
    question_id int(11)                                                     not null,
    id          int(11) primary key                                         not null,
    url         varchar(1024)                                               not null,
    raw         mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci not null,
    index (url),
    unique (question_id)
)
;
create table zeta_alation_query
(
    id      int(11) primary key                                         not null,
    url     varchar(1024)                                               not null,
    content mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci not null,
    raw     mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci not null,
    index (url)
)
;

alter table zeta_ace_question
    modify content mediumtext
        CHARACTER SET utf8mb4
            COLLATE utf8mb4_unicode_ci;

insert into zeta_ace_tag(name, nt, description, create_time, update_time)
    value ('alation-article', 'tojin', 'Alation Article migration tag', now(), now());
# alation migration related

CREATE TABLE `jupyter_notebook_conf`
(
    `nt`                        varchar(50) NOT NULL,
    `conf_name`                      varchar(50) NOT NULL,
    `pyspark_conf`                mediumtext,
    PRIMARY KEY (`nt`,`conf_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

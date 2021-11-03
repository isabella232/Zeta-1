import CodeMirror from 'codemirror';
// these keywords are used by all SQL dialects (however, a mode can still overwrite it)
const sqlKeywords = 'alter and as asc between by count create delete desc distinct drop from group having in insert into is join like not on or order select set table union update values where limit ';

// turn a space-separated list into an array
function set (str: string) {
  const obj = {}, words = str.split(' ');
  for (let i = 0; i < words.length; ++i) obj[words[i]] = true;
  return obj;
}
function hookVar (stream) {
  // variables
  // @@prefix.varName $varName
  // varName can be quoted with ` or ' or "
  // ref: http://dev.mysql.com/doc/refman/5.5/en/user-variables.html
  if (stream.eat('@')) {
    stream.match(/^session\./);
    stream.match(/^local\./);
    stream.match(/^global\./);
  }

  if (stream.eat('\'')) {
    stream.match(/^.*'/);
    return 'variable-2';
  } else if (stream.eat('\{')) {
    stream.match(/^.*?}/);
    return 'variable-2';
  } else if (stream.eat('"')) {
    stream.match(/^.*"/);
    return 'variable-2';
  } else if (stream.eat('`')) {
    stream.match(/^.*`/);
    return 'variable-2';
  } else if (stream.match(/^[0-9a-zA-Z$\.\_]+/)) {
    return 'variable-2';
  }
  return null;
}
// short client keyword token
function hookClient (stream) {
  // \N means NULL
  // ref: http://dev.mysql.com/doc/refman/5.5/en/null-values.html
  if (stream.eat('N')) {
    return 'atom';
  }
  // \g, etc
  // ref: http://dev.mysql.com/doc/refman/5.5/en/mysql-commands.html
  return stream.match(/^[a-zA-Z.#!?]/) ? 'variable-2' : null;
}
function hookIdentifier (stream) {
  // MySQL/MariaDB identifiers
  // ref: http://dev.mysql.com/doc/refman/5.6/en/identifier-qualifiers.html
  let ch;
  while ((ch = stream.next()) != null) {
    if (ch == '`' && !stream.eat('`')) return 'variable-2';
  }
  stream.backUp(stream.current().length - 1);
  return stream.eatWhile(/\w/) ? 'variable-2' : null;
}
CodeMirror.defineMIME('text/z-x-sparksql', {
  name: 'sql',
  keywords: set('add after all alter analyze and anti archive array as asc at between bucket buckets by cache cascade case cast change clear cluster clustered codegen collection column columns comment commit compact compactions compute concatenate cost create cross cube current current_date current_timestamp database databases datata dbproperties defined delete delimited deny desc describe dfs directories distinct distribute drop else end escaped except exchange exists explain export extended external false fields fileformat first following for format formatted from full function functions global grant group grouping having if ignore import in index indexes inner inpath inputformat insert intersect interval into is items join keys last lateral lazy left like limit lines list load local location lock locks logical macro map minus msck natural no not null nulls of on optimize option options or order out outer outputformat over overwrite partition partitioned partitions percent preceding principals purge range recordreader recordwriter recover reduce refresh regexp rename repair replace reset restrict revoke right rlike role roles rollback rollup row rows schema schemas select semi separated serde serdeproperties set sets show skewed sort sorted start statistics stored stratify struct table tables tablesample tblproperties temp temporary terminated then to touch transaction transactions transform true truncate unarchive unbounded uncache union unlock unset use using values view when where window with %refresh_var'),
  builtin: set('tinyint smallint int bigint boolean float double string binary timestamp decimal array map struct uniontype delimited serde sequencefile textfile rcfile inputformat outputformat'),
  atoms: set('false true null'),
  operatorChars: /^[*+\-<>!=~&|^]/,
  dateSQL: set('date time timestamp'),
  support: set('ODBCdotTable doubleQuote zerolessFloat'),
  hooks: {
    '$':  hookVar,
  },
});

CodeMirror.defineMIME('text/z-x-hive', {
  name: 'sql',
  keywords: set('select alter $elem$ $key$ $value$ add after all analyze and archive as asc before between binary both bucket buckets by cascade case cast change cluster clustered clusterstatus collection column columns comment compute concatenate continue create cross cursor data database databases dbproperties deferred delete delimited desc describe directory disable distinct distribute drop else enable end escaped exclusive exists explain export extended external fetch fields fileformat first format formatted from full function functions grant group having hold_ddltime idxproperties if import in index indexes inpath inputdriver inputformat insert intersect into is items join keys lateral left like limit lines load local location lock locks mapjoin materialized minus msck no_drop nocompress not of offline on option or order out outer outputdriver outputformat overwrite partition partitioned partitions percent plus preserve procedure purge range rcfile read readonly reads rebuild recordreader recordwriter recover reduce regexp rename repair replace restrict revoke right rlike row schema schemas semi sequencefile serde serdeproperties set shared show show_database sort sorted ssl statistics stored streamtable table tables tablesample tblproperties temporary terminated textfile then tmp to touch transform trigger unarchive undo union uniquejoin unlock update use using utc utc_tmestamp view when where while with admin authorization char compact compactions conf cube current current_date current_timestamp day decimal defined dependency directories elem_type exchange file following for grouping hour ignore inner interval jar less logical macro minute month more none noscan over owner partialscan preceding pretty principals protection reload rewrite role roles rollup rows second server sets skewed transactions truncate unbounded unset uri user values window year %refresh_var'),
  builtin: set('bool boolean long timestamp tinyint smallint bigint int float double date datetime unsigned string array struct map uniontype key_type utctimestamp value_type varchar'),
  atoms: set('false true null unknown'),
  operatorChars: /^[*+\-<>!=]/,
  dateSQL: set('date timestamp'),
  support: set('ODBCdotTable doubleQuote binaryNumber hexNumber'),
  hooks: {
    '$':  hookVar,
  },
});

CodeMirror.defineMIME('text/z-x-mysql', {
  name: 'sql',
  client: set('charset clear connect edit ego exit go help nopager notee nowarning pager print prompt quit rehash source status system tee'),
  keywords: set(sqlKeywords + 'accessible action add after algorithm all analyze asensitive at authors auto_increment autocommit avg avg_row_length before binary binlog both btree cache call cascade cascaded case catalog_name chain change changed character check checkpoint checksum class_origin client_statistics close coalesce code collate collation collations column columns comment commit committed completion concurrent condition connection consistent constraint contains continue contributors convert cross current current_date current_time current_timestamp current_user cursor data database databases day_hour day_microsecond day_minute day_second deallocate dec declare default delay_key_write delayed delimiter des_key_file describe deterministic dev_pop dev_samp deviance diagnostics directory disable discard distinctrow div dual dumpfile each elseif enable enclosed end ends engine engines enum errors escape escaped even event events every execute exists exit explain extended fast fetch field fields first flush for force foreign found_rows full fulltext function general get global grant grants group group_concat handler hash help high_priority hosts hour_microsecond hour_minute hour_second if ignore ignore_server_ids import index index_statistics infile inner innodb inout insensitive insert_method install interval invoker isolation iterate key keys kill language last leading leave left level limit linear lines list load local localtime localtimestamp lock logs low_priority master master_heartbeat_period master_ssl_verify_server_cert masters match max max_rows maxvalue message_text middleint migrate min min_rows minute_microsecond minute_second mod mode modifies modify mutex mysql_errno natural next no no_write_to_binlog offline offset one online open optimize option optionally out outer outfile pack_keys parser partition partitions password phase plugin plugins prepare preserve prev primary privileges procedure processlist profile profiles purge query quick range read read_write reads real rebuild recover references regexp relaylog release remove rename reorganize repair repeatable replace require resignal restrict resume return returns revoke right rlike rollback rollup row row_format rtree savepoint schedule schema schema_name schemas second_microsecond security sensitive separator serializable server session share show signal slave slow smallint snapshot soname spatial specific sql sql_big_result sql_buffer_result sql_cache sql_calc_found_rows sql_no_cache sql_small_result sqlexception sqlstate sqlwarning ssl start starting starts status std stddev stddev_pop stddev_samp storage straight_join subclass_origin sum suspend table_name table_statistics tables tablespace temporary terminated to trailing transaction trigger triggers truncate uncommitted undo uninstall unique unlock upgrade usage use use_frm user user_resources user_statistics using utc_date utc_time utc_timestamp value variables varying view views warnings when while with work write xa xor year_month zerofill begin do then else loop repeat %refresh_var'),
  builtin: set('bool boolean bit blob decimal double float long longblob longtext medium mediumblob mediumint mediumtext time timestamp tinyblob tinyint tinytext text bigint int int1 int2 int3 int4 int8 integer float float4 float8 double char varbinary varchar varcharacter precision date datetime year unsigned signed numeric'),
  atoms: set('false true null unknown'),
  operatorChars: /^[*+\-<>!=&|^]/,
  dateSQL: set('date time timestamp'),
  support: set('ODBCdotTable decimallessFloat zerolessFloat binaryNumber hexNumber doubleQuote nCharCast charsetCast commentHash commentSpaceRequired'),
  hooks: {
    '@':   hookVar,
    '`':   hookIdentifier,
    '\\':  hookClient,
  },
});

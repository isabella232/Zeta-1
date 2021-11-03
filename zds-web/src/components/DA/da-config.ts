'use strict';
export default {
  email_from_addr: 'DL-eBay-Metadata@ebay.com',
  email_to_da_addr: process.env.VUE_APP_ENV == 'production' ? '' : '',
  email_cc_addr: process.env.VUE_APP_ENV == 'production' ? 'binsong@ebay.com;dojin@ebay.com;bjin@ebay.com;akolpakov@ebay.com;DL-eBay-DSS-DataArchitects@ebay.com;' : 'bjin@ebay.com;',
  email_default_subject: process.env.VUE_APP_ENV == 'production' ? 'DOE - Data Model notification' : 'DOE - Data Model notification (QA/DEV test)',
  platform: [
    { label: 'Mozart', value: 'numozart' },
    { label: 'Ares', value: 'ares' },
    { label: 'Apollo_rno', value: 'apollo_rno' },
    { label: 'Hercules', value: 'hercules' }
  ],
  spark_sql_header: 'CREATE TABLE $table ',
  spark_sql_footer: '\nUSING parquet\nOPTIONS (\n    compression \'snappy\',\n    serialization.format \'1\',\n    path \'$path\'\n);',
  td_multiset_header: 'CREATE MULTISET TABLE $table, NO FALLBACK,\nNO BEFORE JOURNAL,\nNO AFTER JOURNAL,\nCHECKSUM = DEFAULT,\nDEFAULT MERGEBLOCKRATIO ',
  td_multiset_footer: ';',
  hadoop_platform: ['Apollo_rno', 'Hercules', 'Ares', 'Apollo'],
  platformGroup: [
    {
      label: 'td',
      options: [{
        value: 'numozart',
        label: 'Mozart'
      }]
    }, {
      label: 'spark',
      options: [{
        value: 'ares',
        label: 'Ares'
      }, {
        value: 'apollo_rno',
        label: 'Apollo_rno'
      }, {
        value: 'hercules',
        label: 'Hercules'
      }]
    }
  ],
  excel_template: [
    { title: 'Business Element Name', key: 'columnname' },
    { title: 'Data Type and length', key: 'columntype' },
    { title: 'Element Description (Please have proper business descriptions in complete sentences)', key: 'desc' },
    { title: 'Primary Business Identifier Y/N', key: 'pk_flag' },
    { title: 'Please indicate UPI or NUPI for index on Attribute', key: 'pi_flag' },
    { title: 'Null/Not Null (Y for Not null, N or Blank for Null)', key: 'mndtry_flag' },
    { title: 'PII Column (for security hiding) Y/N Only', key: 'pii_flag' },
    { title: 'PPI', key: 'ppi_flag' },
  ]
};
/* eslint-disable @typescript-eslint/camelcase */
'use strict';
export default {
  tabInfo: [
    { label: 'All', value: 'all', allowDo: false },
    { label: 'Hopper Migration', value: 'hopperMigration', allowDo: false },
    { label: 'Mozart Migration', value: 'mozartMigration', allowDo: false },
    { label: 'Change Log', value: 'changeLog', allowDo: false }
  ],
  all: [
    {
      label: 'Table',
      property: 'table_name',
      display: true,
      minWidth: '200',
      format: 'table_name',
      type: 'search-filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Database',
      property: 'db_name',
      display: true,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false,
      groupCol: true,
    },
    {
      label: 'Platform',
      property: 'pltfrm_name',
      display: true,
      minWidth: '150',
      format: 'upperfirst-platform',
      type: 'filter',
      sortable: true,
      edit: false,
      groupCol: true,
    },
    {
      label: 'Subject Area',
      property: 'sbjct_area',
      display: true,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false
    },
    { label: 'Team', property: 'team_name', display: false, minWidth: '150', type: 'filter', edit: false },
    {
      label: 'Owner',
      property: 'owner_name',
      display: true,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    { label: 'Organization', property: 'organization', display: false, minWidth: '150', type: 'filter', edit: false },
    { label: 'Hopper Table Size(GB)', property: 'table_size', display: false, minWidth: '150', edit: false },
    { label: 'Hopper Unique User 90D', property: 'unique_user_90d', display: false, minWidth: '150', edit: false },
    { label: 'Hopper Unique Batch 90D', property: 'unique_batch_90d', display: false, minWidth: '150', edit: false }
  ],
  hopperMigration: [
    {
      label: 'Table',
      property: 'table_name',
      display: true,
      minWidth: '200',
      format: 'table_name',
      type: 'search-filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Database',
      property: 'hopper_db_name',
      display: true,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false,
      groupCol: true
    },
    {
      label: 'Platform',
      property: 'pltfrm_name',
      display: false,
      minWidth: '150',
      format: 'upperfirst-platform',
      type: 'filter',
      defaultSelected: 'Hopper',
      sortable: false,
      edit: false,
      groupCol: true
    },
    {
      label: 'Subject Area',
      property: 'sbjct_area',
      display: true,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Team',
      property: 'team_name',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Owner',
      property: 'owner_name',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Migration Owner',
      property: 'migration_owner',
      display: true,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Migration Status',
      property: 'apollo_migration_status',
      display: true,
      minWidth: '150',
      format: 'apollo_migration_status',
      type: 'filter',
      sortable: true,
      edit: true,
      editType: 'filter',
      defaultSelected: [
        'Done',
        'Hand Over',
        'In_Progress',
        'To_Be_Started',
      ].join(','),
      filterOptions: [
        { apollo_migration_status: 'Done' },
        { apollo_migration_status: 'Hand Over' },
        { apollo_migration_status: 'In_Progress' },
        { apollo_migration_status: 'To_Be_Started' },
        { apollo_migration_status: 'Won\'t_Do' }
      ],
      editOptions: [
        { label: '', value: '' },
        { label: 'Done', value: 'Done' },
        { label: 'Hand Over', value: 'Hand Over' },
        { label: 'In_Progress', value: 'In_Progress' },
        { label: 'To_Be_Started', value: 'To_Be_Started' },
        { label: 'Won\'t_Do', value: 'Won\\\'t_Do' }
      ]
    },
    { label: 'Comment', property: 'comment', display: false, minWidth: '200', edit: true, editType: 'textarea' },
    {
      label: 'Apollo ETA',
      property: 'apollo_eta',
      display: true,
      minWidth: '120',
      type: 'date-filter',
      sortable: true,
      edit: true,
      editType: 'date',
      valueFormat: 'M/dd/yyyy'
    },
    {
      label: 'Migration Scope',
      property: 'migration_scope',
      display: true,
      minWidth: '150',
      format: 'migration_scope',
      type: 'filter',
      defaultSelected: 'Y',
      sortable: true,
      edit: true,
      editType: 'filter',
      editOptions: [{ label: '', value: '' }, { label: 'Y', value: 'Y' }, { label: 'N', value: 'N' }]
    },
    {
      label: 'Apollo Metadata',
      property: 'apollo_metadata',
      display: true,
      minWidth: '150',
      format: 'apollo_metadata',
      type: 'filter',
      sortable: true,
      edit: false,
      filterOptions: [
        { apollo_metadata: 'Match' },
        { apollo_metadata: 'Unmatch' },
        { apollo_metadata: 'Unmatch & Ignore' }
      ]
    },
    {
      label: 'Organization',
      property: 'organization',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Priority',
      property: 'priority',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: true,
      editType: 'filter',
      editOptions: [{ label: '', value: '' }, { label: 'P1', value: 'P1' }]
    },
    { label: 'Table Size(GB)', property: 'table_size', display: false, minWidth: '150', sortable: true, edit: false },
    {
      label: 'Unique User 90D',
      property: 'unique_user_90d',
      display: false,
      minWidth: '150',
      sortable: true,
      edit: false
    },
    {
      label: 'Unique Batch 90D',
      property: 'unique_batch_90d',
      display: false,
      minWidth: '150',
      sortable: true,
      edit: false
    },
    {
      label: 'Apollo View',
      property: 'apollo_view',
      display: true,
      minWidth: '125',
      type: 'filter',
      sortable: true,
      edit: false,
      filterOptions: [{ apollo_view: 'Yes' }, { apollo_view: 'No' }, { apollo_view: { key: 'Ignore', label: 'No & Ignore' } }]
    },
    {
      label: 'Apollo DQ',
      property: 'apollo_dq',
      display: true,
      minWidth: '120',
      format: 'apollo_dq',
      type: 'filter',
      sortable: true,
      edit: false,
      filterOptions: [{ apollo_dq: { key: '', label: 'No DQ rule' } }, { apollo_dq: 'Fail' }, { apollo_dq: 'Fail & Ignore' }, { apollo_dq: 'Pass' }]
    },
    { label: 'DQ Rule', property: 'dq_rule', display: true, minWidth: '100', edit: true, sortable: true }
  ],
  mozartMigration: [
    {
      label: 'Table',
      property: 'table_name',
      display: true,
      minWidth: '200',
      format: 'table_name',
      type: 'search-filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Database',
      property: 'mozart_db_name',
      display: true,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false,
      groupCol: true
    },
    {
      label: 'Platform',
      property: 'pltfrm_name',
      display: false,
      minWidth: '150',
      format: 'upperfirst-platform',
      type: 'filter',
      defaultSelected: 'Numozart',
      sortable: false,
      edit: false,
      groupCol: true
    },
    {
      label: 'Subject Area',
      property: 'sbjct_area',
      display: false,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Team',
      property: 'team_name',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Table Owner',
      property: 'owner_name',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Migration Owner',
      property: 'mozart_migration_owner',
      display: true,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Migration Status',
      property: 'mozart_migration_status',
      display: true,
      minWidth: '150',
      format: 'mozart_migration_status',
      type: 'filter',
      sortable: true,
      edit: true,
      editType: 'filter',
      // defaultSelected: [
      //   'Done',
      //   'Hand Over',
      //   'In_Progress',
      //   'To_Be_Started',
      // ].join(','),
      filterOptions: [
        { mozart_migration_status: 'Done' },
        { mozart_migration_status: 'HandOver_Done' },
        { mozart_migration_status: 'In_Progress' },
        { mozart_migration_status: 'To_Be_Started' },
        { mozart_migration_status: 'Migrated_2019' }
      ],
      editOptions: [
        { label: '', value: '' },
        { label: 'Done', value: 'Done' },
        { label: 'HandOver_Done', value: 'HandOver_Done' },
        { label: 'In_Progress', value: 'In_Progress' },
        { label: 'To_Be_Started', value: 'To_Be_Started' },
        { label: 'Migrated_2019', value: 'Migrated_2019' }
      ]
    },
    {
      label: 'Migration Decision',
      property: 'mozart_migration_decision',
      display: false,
      minWidth: '150',
      format: 'mozart_migration_decision',
      type: 'filter',
      sortable: true,
      edit: true,
      editType: 'filter',
      filterOptions: [
        { mozart_migration_decision: 'Won\'t_Do' },
        { mozart_migration_decision: 'Retire' },
        { mozart_migration_decision: 'Migration' },
        { mozart_migration_decision: 'Migration_DistCP' },
        { mozart_migration_decision: 'Done' },
        { mozart_migration_decision: 'Migration_ADPO' }
      ],
      editOptions: [
        { label: '', value: '' },
        { label: 'Won\'t_Do', value: 'Won\\\'t_Do' },
        { label: 'Retire', value: 'Retire' },
        { label: 'Migration', value: 'Migration' },
        { label: 'Migration_DistCP', value: 'Migration_DistCP' },
        { label: 'Done', value: 'Done' },
        { label: 'Migration_ADPO', value: 'Migration_ADPO' }
      ]
    },
    { label: 'Comment', property: 'comment', display: false, minWidth: '200', edit: true, editType: 'textarea' },
    {
      label: ' Apollo ETA',
      property: 'mozart_eta',
      display: true,
      minWidth: '120',
      type: 'date-filter',
      sortable: true,
      edit: true,
      editType: 'date',
      valueFormat: 'yyyy-MM-dd'
    },
    {
      label: ' Migration Scope',
      property: 'mozart_migration_scope',
      display: false,
      minWidth: '150',
      format: 'mozart_migration_scope',
      type: 'filter',
      filterOptions: [
        { mozart_migration_scope: 'Yes' },
        { mozart_migration_scope: 'No' }
      ],
      defaultSelected: 'Yes',
      sortable: true,
      edit: true,
      editType: 'filter',
      editOptions: [{ label: '', value: '' }, { label: 'Yes', value: 'Yes' }, { label: 'No', value: 'No' }]
    },
    {
      label: ' Apollo Metadata',
      property: 'mozart_metadata',
      display: false,
      minWidth: '150',
      format: 'mozart_metadata',
      type: 'filter',
      sortable: true,
      edit: false,
      filterOptions: [
        { mozart_metadata: 'Match' },
        { mozart_metadata: 'Unmatch' },
        { mozart_metadata: 'Unmatch & Ignore' }
      ]
    },
    {
      label: ' Organization',
      property: 'mozart_organization',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: ' Priority',
      property: 'mozart_priority',
      display: false,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: true,
      editType: 'filter',
      editOptions: [{ label: '', value: '' }, { label: 'P1', value: 'P1' }]
    },
    { label: 'Table Size(GB)', property: 'mozart_table_size', display: false, minWidth: '150', sortable: true, edit: false },
    {
      label: 'Unique User 90D',
      property: 'mozart_unique_user_90d',
      display: false,
      minWidth: '150',
      sortable: true,
      edit: false
    },
    {
      label: 'Unique Batch 90D',
      property: 'mozart_unique_batch_90d',
      display: false,
      minWidth: '150',
      sortable: true,
      edit: false
    },
    {
      label: ' Apollo View',
      property: 'mozart_view',
      display: false,
      minWidth: '125',
      type: 'filter',
      sortable: true,
      edit: false,
      filterOptions: [{ mozart_view: 'Yes' }, { mozart_view: 'No' }, { mozart_view: { key: 'Ignore', label: 'No & Ignore' } }]
    },
    {
      label: ' Apollo DQ',
      property: 'mozart_dq',
      display: false,
      minWidth: '120',
      format: 'mozart_dq',
      type: 'filter',
      sortable: true,
      edit: false,
      filterOptions: [{ mozart_dq: { key: '', label: 'No DQ rule' } }, { mozart_dq: 'Fail' }, { mozart_dq: 'Fail & Ignore' }, { mozart_dq: 'Pass' }]
    },
    { label: 'DQ Rule', property: 'mozart_dq_rule', display: false, minWidth: '100', edit: true, sortable: true }
  ],
  changeLog: [
    {
      label: 'Table',
      property: 'table_name',
      display: true,
      width: '200',
      format: 'uppercase',
      type: 'search-filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Recent Change',
      property: 'recent_change',
      display: true,
      minWidth: '500',
      type: 'date-filter',
      format: 'html',
      sortable: false,
      edit: false
    },
    {
      label: 'Database',
      property: 'db_name',
      display: true,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false,
      groupCol: true,
    },
    {
      label: 'Subject Area',
      property: 'sbjct_area',
      display: true,
      minWidth: '150',
      format: 'uppercase',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Owner',
      property: 'owner_name',
      display: true,
      minWidth: '150',
      type: 'filter',
      sortable: true,
      edit: false
    },
    {
      label: 'Migration Status',
      property: 'apollo_migration_status',
      display: true,
      minWidth: '190',
      format: 'apollo_migration_status',
      type: 'filter',
      sortable: true,
      edit: false,
      filterOptions: [
        { apollo_migration_status: 'Done' },
        { apollo_migration_status: 'Hand Over' },
        { apollo_migration_status: 'In_Progress' },
        { apollo_migration_status: 'To_Be_Started' },
        { apollo_migration_status: 'Won\'t_Do' }
      ]
    },
    {
      label: 'Migration Scope',
      property: 'migration_scope',
      display: true,
      minWidth: '150',
      format: 'migration_scope',
      type: 'filter',
      defaultSelected: 'Y',
      sortable: true,
      edit: false
    }
  ],
  editColumn: [
    { label: 'Migration Status', property: 'apollo_migration_status' },
    { label: 'Migration Status', property: 'mozart_migration_status' },
    { label: 'Migration Scope', property: 'migration_scope' },
    { label: 'Migration Scope', property: 'mozart_igration_scope' },
    { label: 'Comment', property: 'comment' },
    { label: 'Apollo ETA', property: 'apollo_eta' },
    { label: 'Apollo ETA', property: 'mozart_eta' },
    { label: 'Priority', property: 'mozart_priority' },
    { label: 'DQ Rule', property: 'dq_rule' },
    { label: 'DQ Rule', property: 'mozart_dq_rule' },
    { label: 'Migration Decision', property: 'mozart_migration_decision' },
  ],
  admin: ['bjin', 'binsong', 'zxiu', 'weiywang', 'jlei1', 'jiajsun', 'xixliu', 'halong', 'hchen6', 'ndhanpal']
};

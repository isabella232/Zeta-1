import suggestion from './suggestion';

interface SourceColumn {
  s_name: string;
  s_type: string;
  s_desc: string;
  s_isEic: string;
  s_isPCI: boolean;
  s_isPI: boolean;
  s_isMandatory: boolean;
}
interface TargetColumn extends Partial<FormColumn>{
  t_name: string;
  t_type: string;
  t_desc: string;
  t_isPK: boolean;
  t_isPII: boolean;
  t_partition: boolean;
  t_isIncremental: boolean;
  t_inDDL?: boolean | null;
  isAuditCol?: boolean;
}
const AuditColAttr = {
  t_desc: 'audit column',
  t_isPK: false,
  t_partition: false,
  t_isIncremental: false,
  t_inDDL: true,
  t_isPII: false,
  isAuditCol: true,
};
interface FormColumn {
  suggestedName: string;
  suggestedType: string;
}

type Flag = 'Y' | 'N';
interface SourceColMeta {
  column_name: string;
  data_type: string;
  pk_flag: Flag;
  prtn_flag: Flag;
  pii_flag: Flag;
  pci_flag: Flag;
  eic_flag: string; // this is not Y/N flag
  mndtry_flag: Flag;
  column_desc: string;
  src_column: string;
  src_type: string;
  incr_index_flag: Flag;
  needed_flag: Flag;
}
export type Column = Partial<SourceColumn> & TargetColumn;

const AppendColumns: TargetColumn[] = [
  {
    t_name: 'cre_date',
    t_type: 'date',
    ...AuditColAttr,
  },
  {
    t_name: 'cre_user',
    t_type: 'string',
    ...AuditColAttr,
  },
  {
    t_name: 'upd_date',
    t_type: 'timestamp',
    ...AuditColAttr,
  },
  {
    t_name: 'upd_user',
    t_type: 'string',
    ...AuditColAttr,
  },
];

export const AppendColumnNames = new Set(AppendColumns.map(c => c.t_name));

export async function buildInitColumns(sourceColumns: SourceColMeta[] = []): Promise<Column[]> {
  const sourceNames = sourceColumns.map(col => col.column_name);
  const sourceTypes = sourceColumns.map(col => col.data_type);
  const suggestedNames = await suggestion.getSuggestedName(sourceNames);
  const suggestedTypes = await suggestion.getSuggestedType(sourceTypes);
  function mapper(col: SourceColMeta, suggestedNames = {}, suggestedTypes = {}): Column {
    const suggestedName = suggestedNames[col.column_name];
    const suggestedType = suggestedTypes[col.data_type];
    return {
      s_name: col.column_name,
      s_type: col.data_type,
      s_desc: col.column_desc,
      s_isEic: col.eic_flag,
      s_isPCI: col.pci_flag === 'Y',
      s_isMandatory: col.mndtry_flag === 'Y',
      t_name: suggestedName,
      t_type: suggestedType,
      t_desc: col.column_desc,
      t_isPII: col.pii_flag === 'Y',
      t_isPK: col.pk_flag === 'Y',
      t_isIncremental: false,
      t_partition: col.prtn_flag === 'Y',
      t_inDDL: true,
      suggestedName,
      suggestedType,
    }; 
  }
  return [
    ...sourceColumns.map(col => mapper(col, suggestedNames, suggestedTypes)),
    ...AppendColumns,
  ];
}

export async function buildColumnsByDataModel(modelColumns: SourceColMeta[] = [], sourceColumns: SourceColMeta[] = []) {
  const sourceNames = sourceColumns.map(col => col.column_name);
  const sourceTypes = sourceColumns.map(col => col.data_type);
  const suggestedNames = await suggestion.getSuggestedName(sourceNames);
  const suggestedTypes = await suggestion.getSuggestedType(sourceTypes);
  function mapper(col: SourceColMeta, suggestedNames = {}, suggestedTypes = {}): Column {
    const modelCol = modelColumns.find(c => c.src_column === col.column_name) || col;
    const suggestedName = suggestedNames[col.column_name];
    const suggestedType = suggestedTypes[col.data_type];
    return {
      s_name: col.column_name,
      s_type: col.data_type,
      s_desc: col.column_desc,
      s_isEic: col.eic_flag,
      s_isPCI: col.pci_flag === 'Y',
      s_isMandatory: col.mndtry_flag === 'Y',
      t_name: modelCol.column_name,
      t_type: modelCol.data_type,
      t_desc: modelCol.column_desc,
      t_isPK: modelCol.pk_flag === 'Y',
      t_isPII: modelCol.pii_flag === 'Y',
      t_partition: modelCol.prtn_flag === 'Y',
      t_isIncremental: modelCol.incr_index_flag === 'Y',
      t_inDDL: modelCol.needed_flag === 'Y',
      suggestedName,
      suggestedType,
    }; 
  }
  return [
    ...sourceColumns.map(col => mapper(col, suggestedNames, suggestedTypes)),
    ...AppendColumns,
  ];
}

export function needApprove(col: Column): boolean {
  return col.suggestedName !== col.t_name || col.suggestedType !== col.t_type || !col.t_desc;
}

export async function isDataModelNeedApproval(modelColumns: SourceColMeta[] = []) {
  const srcNames: string[] = [];
  const srcTypes: string[] = [];
  modelColumns.forEach(c => {
    srcNames.push(c.src_column);
    srcTypes.push(c.src_type);
  });
  const [suggestedNames, suggestedTypes ] = await Promise.all([suggestion.getSuggestedName(srcNames), suggestion.getSuggestedType(srcTypes)]);

  return modelColumns.filter(c => !AppendColumnNames.has(c.column_name)).some(col => {
    return col.data_type !== suggestedTypes[col.src_type] || col.column_name !== suggestedNames[col.src_column] || !col.column_desc.trim(); 
  });
}
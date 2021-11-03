export interface UDFMetadata {
  name: string;
  db_name: string;
  owner: string;
  team_dl: string;
  platform: string | string[];
  class_name: string;
  description: string;
  parameters: string;
  example: string;
}

export type EditableField = 'owner' | 'team_dl' | 'description' | 'parameters' | 'example';

export type EditableUDFMetadata = Pick<UDFMetadata, EditableField>;

export interface Preference {
  [key: string]: any;
}
export interface UserInfo {
  name: string;
  tdPass?: string | null;
  githubToken?: string | null;
  winPass?: string| null;
  nt: string;
  preference: Preference | string;
}
export interface Profile {
  nt_login: string;
  firstName: string;
  lastName: string;
}
export const DefaultPreference: Preference = {
  'editor-font-size': '14px'
};

export interface SaDetail {
  sbjct_area: string;
  sbjct_area_name: string;
  lctn_desc: string;
  sa_desc: string;
  dev_mngr: string;
  dev_mngr_nt: string;
  prdct_owner: string;
  prdct_owner_nt: string;
  pm_mngr: string;
  pm_mngr_nt: string;
  prmry_da: string;
  prmry_da_nt: string;
  data_owner: User[];
  start_dt?: any;
  end_dt: string;
  cre_date: string;
  cre_user: string;
  upd_date: string;
  upd_user: string;
  domain: string;
  sub_domain: string;
  sa_code: string;
  batch_acct: string;
  target_db: string;
  target_working_db: string;
  bsa: User[];
  dev: User[];
}

export interface User {
  nt: string;
  name: string;
}

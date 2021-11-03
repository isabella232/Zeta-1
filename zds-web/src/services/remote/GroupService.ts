import config from '@/config/config';
import BaseRemoteService from '@/services/remote/BaseRemoteService';

const BASE_URL = config.doe.doeService;
// const BASE_URL = 'http://localhost:8080/';

export interface Group {
  id: string;
  desc: string;
  items: GroupItem[];
}

export interface GroupItem {
  children?: GroupItem[];
  id: string;
  level?: number;
  groupId?: string;
  type?: 'GROUP' | 'DL' | 'TBD';
  desc: string;
}

export default class GroupService extends BaseRemoteService {
  static readonly instance = new GroupService();

  createGroup (id: string, desc= ''): Promise<Group> {
    return this.post(`${BASE_URL}groups`, {
      id, desc,
    }).then(resp => resp.data.data as Group);
  }

  deleteGroup (id: string) {
    return this.delete(`${BASE_URL}groups/${id}`);
  }

  getGroup (id: string, expand = true): Promise<Group> {
    const params = { expand };
    return this.get(`${BASE_URL}groups/${id}`, { params }).then(resp => resp.data.data as Group);
  }

  getGroups (params: { id?: string[]; q?: string; iq?: string}): Promise<Group[]> {
    return this.get(`${BASE_URL}groups`, { params }).then(resp => resp.data.data as Group[]);
  }

  addGroupItem (groupId: string, items: GroupItem[]) {
    return this.post(`${BASE_URL}groups/${groupId}/items`, items);
  }

  deleteGroupItem (groupId: string, key: string) {
    return this.delete(`${BASE_URL}groups/${groupId}/items/${key}`);
  }

}
/* eslint-disable @typescript-eslint/interface-name-prefix */
import { PlotConfig } from '@/components/common/pivot-table';
import { WorkspaceBase, WorkspaceDecorator, WorkSpaceType } from './workspace.internal';
class Rectangle {
  x: number;
  y: number;
  /* width */
  w: number;
  /* height */
  h: number;
  constructor(x = 0, y = 0, w = 0, h = 0) {
	  this.x = x;
	  this.y = y;
	  this.w = w;
	  this.h = h;
  }
  get left(): number {
	  return this.w < 0 ? this.x + this.w : this.x;
  }
  get top() {
	  return this.h < 0 ? this.y + this.h : this.y;
  }
  get width(): number {
	  return Math.abs(this.w);
  }
  get height(): number {
	  return Math.abs(this.h);
  }
  onDraw(x: number, y: number, width = 0, height = 0) {
	  this.x = x;
	  this.y = y;
	  this.w = width;
	  this.w = height;
  }
  onDrag(width = 0, height = 0) {
	  this.w = width;
	  this.h = height;
  }
}
type LayoutType = 'table' | 'plot' | 'text';
type textConfig = {
  fontSize: number;
  color: string;
};
class LayoutConfig {
  i: number;
  x: number;
  y: number;
  w: number;
  h: number;
  type: LayoutType;
  showEditor?: boolean = false;
  displayConfig?: any;
  value?: string;
  statementId?: number;
  constructor(index: number, type: LayoutType) {
	  this.i = index;
	  this.type = type;
	  this.x = -1;
	  this.y = -1;
	  // this.w = type === 'text' ? 0 : 6;
	  this.w = 8;
	  this.h = type === 'text' ? 0 : 10;
  }
}
interface IDashboardFile {
  id: string;
  nt: string;
  name: string;
  createTime: number;
  updateTime?: number;
  layoutConfigs: LayoutConfig[];
}

export interface DashboardQuery {
  headers: string[];
  rows: Array<string[]>;
  statementId: number;
}
@WorkspaceDecorator({
  type: WorkSpaceType.DASHBOARD,
  iconClass: 'dashboard',
})
export class Dashboard extends WorkspaceBase {
  inited: boolean;
  layoutConfigs: LayoutConfig[];
  queryMap?: Dict<DashboardQuery>;
  plotConfigMap?: Dict<PlotConfig | undefined>;
  dataMap?: Dict<any[]>;
}

export { Rectangle, LayoutConfig, IDashboardFile, LayoutType };

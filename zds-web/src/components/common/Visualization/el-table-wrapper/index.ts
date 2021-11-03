export interface ColumnConfig {
	label: string;
	prop: string;
	className?: string;
	render?: (scope: any) => string;
}
import ElTableWrapper from './el-table-wrapper.vue';
export { ElTableWrapper };

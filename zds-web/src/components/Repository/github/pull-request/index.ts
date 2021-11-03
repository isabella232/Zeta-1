import PullRequest from './pull-request.vue';
import { GithubFile } from '../types'
export default PullRequest;
export { GithubFile } from '../types'
export interface FileSelector {
    getSelectedNodes: () => GithubFile[]
    clear: () => void
}
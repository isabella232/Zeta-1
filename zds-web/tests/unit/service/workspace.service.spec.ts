import { WorkspaceSrv } from '@/services/Workspace.service'
import { IWorkspace } from '@/types/workspace';
import { expect } from 'chai';
describe('workspace.service', () => {
    it('generate tab seq', () => {
        let workspace:Dict<IWorkspace> = {};
        let seq = WorkspaceSrv.getWorkspaceSeq(workspace);
        expect(seq).to.be.equal(0);
        workspace['w_1'] = <IWorkspace> {
            seq: 1,
        };
        workspace['w_2'] = <IWorkspace> {
            seq: 3,
        }
        workspace['w_3'] = <IWorkspace> {
            seq: 2,
        }
        workspace['w_4'] = <IWorkspace> {
            seq: 6,
        }
        seq = WorkspaceSrv.getWorkspaceSeq(workspace);
        expect(seq).to.be.equal(7);
    })
})
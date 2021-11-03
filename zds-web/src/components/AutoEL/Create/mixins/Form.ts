import { attempt } from '@drewxiu/utils';
import { Component, Vue, Watch } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import { Actions } from '../../store';

@Component
export class FormStatus extends Vue {
  @Action(Actions.CheckIsDa) checkIsDA;
  @Action(Actions.CheckIsSAOwner) checkIsSAOwner;
  @State(state => state.AutoEL.task || {}) task;
  @State(state => state.AutoEL.isDA) isDA;
  @State(state => state.AutoEL.isSAOwner) isSAOwner;

  preview = false;

  get formRef () {
    return this.$refs['form'] as any;
  }
  get needApproval () {
    return this.needSAOwnerApproval || this.needDAApproval;
  }
  get needDAApproval () {
    return attempt(() => this.task.approvalStatus.pendingApprovalsFrom.includes('DA'), false);
  }
  get needSAOwnerApproval () {
    return attempt(() => this.task.approvalStatus.pendingApprovalsFrom.includes('SA_OWNER'), false);
  }
  get disabled (){
    if (this.preview) {
      return true;
    }
    if (this.task) {
      return !this.task.editable;
    }
    return false;
  }

  @Watch('$route.query.preview', { immediate: true })
  setPreview (preview) {
    this.preview = preview === 'true';
  }

  @Watch('task', { immediate: true })
  checkIsDAWhenPending () {
    if (!this.task || !this.task.approvalStatus) {
      return;
    }
    if (this.needDAApproval) {
      this.checkIsDA(this.task.sa_name);
    }
    if (this.needSAOwnerApproval) {
      this.checkIsSAOwner(this.task.sa_name);
    }
  }

}
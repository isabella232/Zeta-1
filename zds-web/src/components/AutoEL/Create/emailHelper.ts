import { sendMail, MetadataDL } from '@/components/Metadata/SubjectArea/helper.ts';
import Util from '@/services/Util.service';

export function sentToDAForApproval (daEmail: string, cc: string[], task: any, duplicatedName = false) {
  const subject = '[AutoEL]Data Model of a Task Needs Review';
  const toAddr = [daEmail];
  const dataModelURL = `${location.origin}/${Util.getPath()}#/autoel/task/${task.id}/datamodel?preview=true`;
  const content = {
    name: 'Data Architect',
    msg: `
      Data model of table ${task.tgt_db}.${task.tgt_table} needs review. Please click <a href='${dataModelURL}'>this link</a> for detail.
      <br />
      ${ duplicatedName ? `<br />Please Note: This table already exists on Hadoop platform ${task.tgt_platform}. This Auto-EL task will <strong>drop and recreate</strong> the table.` : ''}
      <br />
      Zeta Team
      <br />
    `,
  };
  return sendMail(subject, content, MetadataDL, toAddr, cc);
}

export function sentToSAOwnerForApproval (sa: string[], cc: string[], task: any) {
  const subject = '[AutoEL]Data Model of a Task Needs Review';
  const dataModelURL = `${location.origin}/${Util.getPath()}#/autoel/task/${task.id}/datamodel?preview=true`;
  const content = {
    name: 'SA Owner',
    msg: `
      Table ${task.tgt_db}.${task.tgt_table} already exists on Hadoop platform ${task.tgt_platform}. This Auto-EL task will <strong>drop and recreate</strong> the table.
      <br />
      Approve it if you could confirm no production issue to overwrite existing table. Please click <a href='${dataModelURL}'>this link</a> for detail.
      <br />
      <br />
      Zeta Team
      <br />
    `,
  };
  return sendMail(subject, content, MetadataDL, sa, cc);
}
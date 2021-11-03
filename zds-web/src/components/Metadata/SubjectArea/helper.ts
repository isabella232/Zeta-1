import DoeRemoteService from '@/services/remote/DoeRemoteService';
import Util from '@/services/Util.service';
import { SaDetail } from '@/types/metadata/sa';
import { isEqual, sortBy } from 'lodash';

export const MetadataDL = 'DL-eBay-Metadata@ebay.com';
const SADetailUrl = `${location.protocol}//${location.host}/${Util.getPath()}#/metadata/sa`;

export const DOEMembers = [
  'zxiu@ebay.com',
  'binsong@ebay.com',
  'qxu1@ebay.com',
];

interface Content {
  name: string;
  msg: string;
}

export function sendMail (subject: string, content: Content, from: string, to: string[], cc: string[] = []) {

  if (process.env.VUE_APP_ENV !== 'production') {
    to = DOEMembers;
    subject = '[TEST]' + subject;
    cc = [];
  }
  const param = {
    fromAddr: from,
    subject,
    content: JSON.stringify(content),
    ccAddr: cc.join(';'),
    toAddr: to.join(';'),
    template: 'ZetaNotification',
    type: 3,
  };
  return DoeRemoteService.instance.createEmail(param);
}

interface Change {
  name: string;
  oldVal: string | string[];
  newVal: string | string[];
}

export function getSAChanges (oldSA: SaDetail, newSA: SaDetail): Change[] {
  const roleFields = [
    { field: 'dev_mngr_nt', name: 'Dev Manager' },
    { field: 'prdct_owner_nt', name: 'Product Owner' },
    { field: 'pm_mngr_nt', name: 'Project Manager' },
    { field: 'prmry_da_nt', name: 'Primary DA' },
    { field: 'data_owner', name: 'Data Steward' },
    { field: 'dev', name: 'DEV SAE' },
    { field: 'bsa', name: 'BSA SAE' },
  ];
  const res: Change[] = [];
  roleFields.forEach(role => {
    let oldVal = oldSA[role.field] || '(empty)';
    let newVal = newSA[role.field] || '(empty)';
    if (Array.isArray(oldVal)) {
      oldVal = sortBy(oldVal.map(i => i.nt));
      newVal = sortBy(newVal.map(i => i.nt));
    }
    if (!isEqual(oldVal, newVal)) {
      res.push({
        name: role.name,
        oldVal: oldVal,
        newVal: newVal,
      });
    } 
  });
  return res;
}

export function sendEmailForRoleChange (authGroup: string[], changes: Change[], sa) {
  const currentNt = Util.getNt();
  const subject = `Role Changes of Subject Area: ${sa} by ${currentNt}.`;
  const content = {
    name: `Owner of ${sa}`,
    msg: `The roles of Subject Area: <strong>${sa}</strong> has been changed by <strong>${currentNt}</strong>.
    <br>
    <br>
    ${changes.map(c => `<strong>${c.name}</strong>:  <span style="text-decoration: line-through;">${c.oldVal}</span>  ->  ${c.newVal}<br>`).join('')}
    <br>
    Please visit <a href="${SADetailUrl}/${sa}">${SADetailUrl}/${sa}</a> or contact ${currentNt}@ebay.com for more information.`,
  };
  return sendMail(subject, content, MetadataDL, authGroup.map(nt => `${nt}@ebay.com`), DOEMembers);
}

export function sendEmailForAddNewAuthMembers (nt: string[], sa) {
  const currentNt = Util.getNt();
  const subject = `You have been added to the Auth Group of Subject Area: ${sa} by ${currentNt}.`;
  const content = {
    name: nt.join(','),
    msg: `You have been added to the Auth Group of Subject Area: ${sa} by ${currentNt}. Please visit ${SADetailUrl}/${sa} or contact ${currentNt}@ebay.com for more information`,
  };
  return sendMail(subject, content, MetadataDL, nt.map(nt => `${nt}@ebay.com`), DOEMembers);
}

export function sendEmailToDeletedUser (nt: string, sa: string) {
  const currentNt = Util.getNt();
  const subject = `You have been deleted from the Auth Group of Subject Area: ${sa} by ${currentNt}.`;
  const content = {
    name: nt,
    msg: `You have been deleted from the Auth Group of Subject Area: ${sa} by ${currentNt}. Please visit ${SADetailUrl}/${sa} or contact ${currentNt}@ebay.com for more information`,
  };
  return sendMail(subject, content, MetadataDL, [`${nt}@ebay.com`], DOEMembers);
}
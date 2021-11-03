import config from '@/config/config';
import ZetaAceRemoteService from '@/services/remote/ZetaAce';
import Util from '@/services/Util.service';
const baseUrl = config.zeta.base;

export class MyUploadAdapter {
  loader: any;
  xhr: any;
  protocol = 'https:';
  zetaAceRemoteService = new ZetaAceRemoteService();
  constructor (loader: any) {
    // The file loader instance to use during the upload.
    this.loader = loader;
    this.protocol = window.location.protocol;
  }

  // Starts the upload process.
  upload () {
    return this.loader.file
      .then( (file: any) => new Promise( ( resolve, reject ) => {
        this.uploadImg(file, resolve, reject);
      } ) );
  }

  uploadImg (file: any, resolve: any, reject: any){
    this.zetaAceRemoteService.attachments(file, (progressEvent: ProgressEvent) => {
      if (progressEvent.lengthComputable) {
        this.loader.uploadTotal = progressEvent.total;
        this.loader.uploaded = progressEvent.loaded;
      }
    }).then(res =>{
      const url = res&& res.data? res.data.fileName+'.'+ res.data.fileExtension : '';
      resolve({
        default: `${this.protocol}${baseUrl}attachments/${url}`,
      });
    }).catch(err => {
      reject(err && err.message ? err.message: 'Fail upload');
    });
  }
}

export function editorConfig (config: Dict<any>, plugin: any[]) {
  return {
    ...config,
    extraPlugins: plugin,
    removePlugins: [ 'Autoformat' ],
    // placeholder: 'Here are some more details.',
    toolbar: [
      'bold',
      'italic',
      'blockQuote',
      'fontSize',
      'fontColor',
      'fontBackgroundColor',
      'link',
      '|',
      'codeBlock',
      'imageUpload',
      // 'insertTable',
      // 'mediaEmbed',
      '|',
      'undo',
      'redo',
    ],
    link: {
      addTargetToExternalLinks: true,
    },
    image: {
      upload: {
        types: ['png', 'jpeg', 'jpg'],
      },
      image: {
        toolbar: [
          'imageStyle:alignLeft',
          'imageStyle:full',
          'imageStyle',
        ],
      },
    },
    fontSize:{
      options:[
        10, 12,  'default',  16, 18, 20, 22, 24,
      ],
    },
    fontColor:{
      colors: [
        {
          color: 'hsl(0, 0%, 0%)',
          label: 'Black',
        },
        {
          color: 'hsl(0, 0%, 30%)',
          label: 'Dim grey',
        },
        {
          color: 'hsl(0, 0%, 60%)',
          label: 'Grey',
        },
        {
          color: 'hsl(0, 0%, 90%)',
          label: 'Light grey',
        },
        {
          color: 'hsl(0, 0%, 100%)',
          label: 'White',
          hasBorder: true,
        },
        {
          color: 'hsl(0, 75%, 60%)',
          label: 'Red',
        },
        {
          color: 'hsl(30, 75%, 60%)',
          label: 'Orange',
        },
        {
          color: 'hsl(60, 75%, 60%)',
          label: 'Yellow',
        },
        {
          color: 'hsl(90, 75%, 60%)',
          label: 'Light green',
        },
        {
          color: 'hsl(120, 75%, 60%)',
          label: 'Green',
        },
        {
          color: 'hsl(150, 75%, 60%)',
          label: 'Aquamarine',
        },
        {
          color: 'hsl(180, 75%, 60%)',
          label: 'Turquoise',
        },
        {
          color: 'hsl(210, 75%, 60%)',
          label: 'Light blue',
        },
        {
          color: 'hsl(240, 75%, 60%)',
          label: 'Blue',
        },
        {
          color: 'hsl(270, 75%, 60%)',
          label: 'Purple',
        },
      ],
    },
    fontBackgroundColor: {
      colors: [
        {
          color: 'hsl(0, 75%, 60%)',
          label: 'Red',
        },
        {
          color: 'hsl(30, 75%, 60%)',
          label: 'Orange',
        },
        {
          color: 'hsl(60, 75%, 60%)',
          label: 'Yellow',
        },
        {
          color: 'hsl(90, 75%, 60%)',
          label: 'Light green',
        },
        {
          color: 'hsl(120, 75%, 60%)',
          label: 'Green',
        },

      ],
    },
    // codeBlock:{
    //   languages:[
    //     { language: 'plaintext', label: 'Plain text', class: '' },
    //     { language: 'java', label: 'Java', class: 'java' },
    //     { language: 'javascript', label: 'JavaScript', class: 'js' },
    //     { language: 'python', label: 'Python' }
    //   ]
    // }
  };
}
function getPostEmail (post: Array<any>){
  const email: Array<string> = [];
  post.map(item => {
    email.push(item.poster+'@ebay.com');
  });
  return email;
}
function getEmailLink (qid: number): string {
  return `${location.protocol}//${location.host}/${Util.getPath()}#/FAQDetail?id=${qid}`;
}
export function generalMail (type: string, questionObj: any, answer?: string) {
  //TODO
  const link = getEmailLink(questionObj.id);
  const nts = [Util.getNt()+'@ebay.com'];
  const answerTemp = `@${Util.getNt()} added a new answer.
                  <br/><br/>
                  Question:
                  <br/>
                  ${questionObj.title}
                  <br/><br/>
                  Answer:
                  <br/>
                  ${answer}
                  <br/><br/>
                  Please <a href=${link}>Click</a>`;
  const questionTemp = `@${Util.getNt()} added a new question.
                    <br/><br/>
                    Question:
                    <br/>
                    ${questionObj.title}
                    <br/><br/>
                    Please <a href=${link} >Click</a>`;
  if (type === 'answer'){
    const submitter = questionObj.submitter;
    nts.push(submitter+'@ebay.com');
    nts.concat(getPostEmail(questionObj.posts));
  }else {
    nts.push('DL-eBay-ZETA-ACE@ebay.com');
  }

  const content: any = {
    name: 'All',
    msg: type ==='answer' ? answerTemp : questionTemp,
  };

  const params: any = {
    'fromAddr': 'DL-eBay-ZETA@ebay.com',
    'toAddr': '',
    'subject': 'Zeta Aces',
    'content': JSON.stringify(content),
    'template': 'ZetaNotification',
    'ccAddr': '',
    'type': 3, //1: html; 2: txt
  };
  params.toAddr = nts.join(';');

  return params;
}

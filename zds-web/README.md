# web part of zeta dev suite

## env files

.env.development => dev(npm run serve)
.env.staging => build in staging (npm run build)
.env.production => build in prod (npm run build)
try to create `.env.development.local` when run in local

## integrate with vue-plotting

path: src/components/common/pivot-table

## Dev Environment Setup

1. Install nodejs. (version >8.9.4) https://nodejs.org/en/
2. Install npm (>5.6.0) https://www.npmjs.com/get-npm
3. Install node modules

```
cd zds-web
npm install
```

4. start server locally
optional env

| name        | desc           | default  |
| ------------- | ------------- | ----- |
| DEBUG_MODE | DEBUG_MODE == false ----> production <br> DEBUG_MODE == 1     ---->  frontend-oss off, backend-oss off <br> DEBUG_MODE == 2     ---->  frontend-oss on, backend-oss off <br> DEBUG_MODE == 3     ---->  frontend-oss on, backend-oss on| true |
| VUE_APP_PREDEFINE_NT | pre-defined NT_LOGIN <br>working when DEBUG_MODE is 2 or 3 | undefined |
```
# run in local directly to server
npm run serve:non-oss
```

5. access index page: http://localhost.corp.ebay.com

6. If you start server at localhost.corp.ebay.com, you need to add the below entry to /etc/hosts.

```
127.0.0.1     localhost.corp.ebay.com
```

## bootstrap flow

### 1. init sso

### 2. create Vue components

### 3. init store/users' info

2. get batch acct info => repo info => dashboard => notebook => registerEventListener

## ~~e2e Test~~
### [cypress](https://docs.cypress.io)

### cypress configuration(./cypress.json)
1. baseUrl default url for website
2. nt_login
3. token


## internal plugin/component
### TRACKING(zds-logger)
#### Count/Gauge Metrics via code
```
import * as Logger from '@/plugins/logs/zds-logger';
...
Logger.counter('LOGIN');
```

### Count/Guage Metrics via vue-plugin
register in `main.ts`
```
import ZdsMetricsPlugin from '@/plugins/logs/metric';
Vue.use(ZdsMetricsPlugin)
```

tracing in template
```
<!-- v-click-metric:${event_name}="${tag}" -->
<div v-click-metric:NAV_CLICK="{name: n.name}"> </div>

```
### Avatar via oss service
#### function: avatarUrl
```
this.$avatarUrl('tianrsun');
```
#### avatar compoent
Source: src/components/common/avatar.vue

sample:
```
<dss-avatar
  inline
  size="small" // avatar size: 'default' || 'small' || 24 || 35
  cycle // border-radius: 50%
  :nt="question.submitter"
/>
```


## ESLint in zds-web
Run below script to auto-fix. After auto-fix, there still might have some issues need to be fixed manually.
```
npm run lint
```
## VS Code plugin(Vetur + ESLint)
workspace setting in my side
```
{
    "editor.tabSize": 2,
    "editor.detectIndentation": false,
    "eslint.alwaysShowStatus": true,
    "eslint.validate": [
      { "language": "typescript", "autoFix": true },
      { "language": "vue", "autoFix": true },
    ],
}
```

## Known Issues
### Rules **vue/no-use-v-if-with-v-for** will cause plugin Error
When you occurring a Error in Vetur, please manually check your vue file
Root cause:
`v-if` and `v-for` couldn't bind in same element.

### get error **Parsing error: '}' expected.eslint** in .vue files
Maybe the Vue file cannot pass the rule `@typescript-eslint/consistent-type-assertions`.
And ESLint plugin in VS Code won't work normally, before this issue been fixed.
Please use as XXX instead of <XXX>.
```
// wrong
const a = <XXX> {
    id: 1,
};

// correct
const a = {
    id: 1,
} as XXX;
```

## TODO
1. add eslint-loader in webpack
2. enable prettier.

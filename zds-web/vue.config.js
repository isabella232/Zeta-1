/* eslint-disable @typescript-eslint/no-var-requires */
const moment = require('moment');
const path = require('path');
const CKEditorWebpackPlugin = require( '@ckeditor/ckeditor5-dev-webpack-plugin' );
const { styles } = require( '@ckeditor/ckeditor5-dev-utils' );
const baseUrl = `/${process.env.BASE_URL || 'zeta'}/`;
const resolveClientEnv = require('@vue/cli-service/lib/util/resolveClientEnv');
const variables = resolveClientEnv(process.env.NODE_ENV === 'production' ? baseUrl : '/');
const WorkboxPlugin = require('workbox-webpack-plugin');
const argv = require('optimist').argv;

/**
 * @param DEBUG_MODE
 * @default false
 * * DEBUG_MODE == false ----> production
 * * DEBUG_MODE == 1     ---->  frontend-oss off, backend-oss off
 * * DEBUG_MODE == 2     ---->  frontend-oss on, backend-oss off
 */
const debugMode = eval(argv.debug || 'false');
const nt_login = argv.user || variables['process.env'].VUE_APP_PREDEFINE_NT ||'';
const webOSSOn = (!debugMode || debugMode != 1) ? true : false;

if (debugMode == 1) {
  if (!nt_login) {
    console.error('Cannot find NT_LOGIN in DEBUG MODE 1');
    process.exit(0);
  } else {
    variables['process.env'].VUE_APP_PREDEFINE_NT = '"' + nt_login + '"';
  }
}
/**
 * env in profile
 * 'production' | 'staging' | 'development'
 */
const BUILD_FILE_ENV = eval(variables['process.env'].VUE_APP_ENV);
module.exports = {
  lintOnSave: false,
  productionSourceMap: BUILD_FILE_ENV !== 'production',
  publicPath: baseUrl,
  pages:{
    index:{
      entry:'src/main.ts',
      template:'public/index.html',
      filename:'index.html'
    },
    share:{
      entry:'src/share-main.ts',
      template:'public/share/index.html',
      filename:'share/index.html'
    }

  },
  transpileDependencies: [
    /ckeditor5-[^/\\]+[/\\]src[/\\].+\.js$/,
  ],
  configureWebpack: {
    plugins: [
      // CKEditor needs its own plugin to be built using webpack.
      new CKEditorWebpackPlugin( {
        // See https://ckeditor.com/docs/ckeditor5/latest/features/ui-language.html
        language: 'en'
      } ),
      // new WorkboxPlugin.GenerateSW({
      //   // these options encourage the ServiceWorkers to get in there fast
      //   // and not allow any straggling "old" SWs to hang around
      //   // include: [/\.html$/, /\.js$/,/\.css$/,/\.png$/,/\.svg$/],
      //   exclude: [/share.*/,/img\/icons.*/,/assets.*/,/tables\.json/,/columns\.json/],
      //   clientsClaim: true,
      //   skipWaiting: true,
      //   sourcemap: false,
      //   maximumFileSizeToCacheInBytes: 50 * 1024 *1024,
      //   runtimeCaching: [
      //     {
      //       urlPattern: /notebook\/users/,
      //       handler: 'StaleWhileRevalidate',
      //       options: {
      //         cacheName: 'zeta-api-cache',
      //         broadcastUpdate:{
      //           channelName: 'api-updates'
      //         }
      //       }
      //     },
      //     {
      //       urlPattern: /(columns|tables).(?:json)$/,
      //       handler: 'StaleWhileRevalidate',
      //       options: {
      //         cacheName: 'zeta-api-cache',
      //         broadcastUpdate:{
      //           channelName: 'json-updates',
      //         }
      //       }
      //     }
      //   ]
      // })
    ]
  },
  chainWebpack: webpackConfig => {
    // webpackConfig.devtool('source-map')
    // define const variables
    variables['process.env'].BUILD_TIME = JSON.stringify(moment().format('x'));
    variables['process.env'].WEB_OSS_ON = webOSSOn;
    webpackConfig
      .plugin('define')
      .use(require('webpack/lib/DefinePlugin'), [
        variables
      ]);
    if (BUILD_FILE_ENV === 'development') {
      webpackConfig.optimization.splitChunks({
        maxInitialRequests: 5,
        cacheGroups: {
          vendors: {
            name: 'chunk-vendors',
            test: /[\\\/]node_modules[\\\/]/,
            priority: -10,
            chunks: 'initial'
          },
        }
      });
    }

    if (webOSSOn) {
      webpackConfig.plugin('html-index')
        .tap(args => {
          args[0].oss = true;
          return args;
        });

      webpackConfig.plugin('html-share')
        .tap(args => {
          args[0].oss = true;
          return args;
        });
    }

    // build dev env
    // cp static files to dist
    // if (process.env.npm_lifecycle_event === 'build-dev') {
    //   webpackConfig
    //     .plugin('copy')
    //     .use(require('copy-webpack-plugin'), [[{
    //       from: path.resolve('public'),
    //       to: path.resolve('dist'),
    //       ignore: ['index.html', '.DS_Store']
    //     }]]);
    // }
    // webpackConfig.plugin('Analyzer').use(require('webpack-bundle-analyzer').BundleAnalyzerPlugin);


    // webpackConfig.plugin('fork-ts-checker')
    // .tap(args => {
    // 	args[0].memoryLimit = 8192;
    // 	return args
    // });
    // (1.) To handle editor icons, get the default rule for *.svg files first:
    const svgRule = webpackConfig.module.rule( 'svg' );

    // Then you can either:
    //
    // * clear all loaders for existing 'svg' rule:
    //
    //		svgRule.uses.clear();
    //
    // * or exclude ckeditor directory from node_modules:
    svgRule.exclude.add( path.join( __dirname, 'node_modules', '@ckeditor' ) );

    // Add an entry for *.svg files belonging to CKEditor. You can either:
    //
    // * modify the existing 'svg' rule:
    //
    //		svgRule.use( 'raw-loader' ).loader( 'raw-loader' );
    //
    // * or add a new one:
    webpackConfig.module
      .rule( 'cke-svg' )
      .test( /ckeditor5-[^/\\]+[/\\]theme[/\\]icons[/\\][^/\\]+\.svg$/ )
      .use( 'raw-loader' )
      .loader( 'raw-loader' );

    // (2.) Transpile the .css files imported by the editor using PostCSS.
    // Make sure only the CSS belonging to ckeditor5-* packages is processed this way.
    webpackConfig.module
      .rule( 'cke-css' )
      .test( /ckeditor5-[^/\\]+[/\\].+\.css$/ )
      .use( 'postcss-loader' )
      .loader( 'postcss-loader' )
      .tap( () => {
        return styles.getPostCssConfig( {
          themeImporter: {
            themePath: require.resolve( '@ckeditor/ckeditor5-theme-lark' ),
          },
          minify: true
        } );
      } );

  },
  devServer: {
    open: process.platform === 'darwin',
    host: 'localhost',
    port: 8000,
    inline: true,
    https: false,
    hotOnly: false,
    // See https://github.com/vuejs/vue-cli/blob/dev/docs/cli-service.md#configuring-proxy
    proxy: null, // string | Object
    disableHostCheck: true,
    before: app => { }
  },
};

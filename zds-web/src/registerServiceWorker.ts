/* eslint-disable no-console */
import Util from '@/services/Util.service';
import { register } from 'register-service-worker';
import _ from 'lodash';
import { ZetaException, ExceptionPacket, ExceptionType } from '@/types/exception';

if (process.env.NODE_ENV === 'production') {
  register('./service-worker.js?', {
    ready () {
      console.log(
        'App is being served from cache by a service worker.\n' +
        'For more details, visit https://goo.gl/AFskqB'
      )
    },
    registered () {
      console.log('Service worker has been registered.')
    },
    cached () {
      console.log('Content has been cached for offline use.')
    },
    updated (registration) {
      console.log('New content is available; please refresh.')
      // console.log('Registration:',registration);
      // Util.refreshPopup('Zeta has been upgraded', 'please refresh this page');
      const ex = new ZetaException(<ExceptionPacket>
      {
        type: ExceptionType.WARNING,
        code: 'Refresh',
        errorDetail: {
          message: 'Zeta has been upgraded'
        }
      },
      {
        path: 'zeta'
      }
      );
      Util.getApp().$store.dispatch('addException', { exception: ex });
    },
    offline () {
      console.log('No internet connection found. App is running in offline mode.')
    },
    error (error) {
      console.error('Error during service worker registration:', error)
    }
  })
}
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.addEventListener('message', async (event) => {
    // Optional: ensure the message came from workbox-broadcast-update
    if (event.data.meta === 'workbox-broadcast-update') {
      const {cacheName, updatedURL} = event.data.payload;

      const jsonFile = _.last(_.split(updatedURL, "/"));
      if (jsonFile == "tables.json" || jsonFile == "columns.json") {
        Util.getApp().$store.dispatch('updateJson', jsonFile);
      }
    }
  });
}
import config from "@/config/config";

export function DSSLogin () {
  return new Promise((resolve, reject) => {
    if (!config.webOSSOn) {
      resolve(process.env.VUE_APP_PREDEFINE_NT);
    } else {
      const dac = window.dssAuthClient;
      if (!dac) {
        reject(new Error('Cannot find DAC components'));
      }
      dac.checkLogin(() => {
        const nt = dac.getNT();
        resolve(nt);
      }, () => {
        dac.redirectToLogin();
      });
    }
  }) as Promise<string>;
}

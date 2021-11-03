import config from '@/config/config';
const jdbcConfig = config.zeta.notebook.connection.jdbcConfig;
export function isHermes (clusterId: string | number) {
  const id = clusterId + '';
  return Object.keys(jdbcConfig).map(cId => cId + '').indexOf(id) >= 0;
}
export function getHermesCnnConfig (clusterId: string | number): {host: string; interpreter: string; jdbcType: string; pricipal: string} {
  const id = parseInt(clusterId + '');
  if (!jdbcConfig[id]) {
    throw new Error('Cannot find Hermes config by clusterId: ' + clusterId);
  }
  return jdbcConfig[id];
}

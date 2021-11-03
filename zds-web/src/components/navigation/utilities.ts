export function getMainAppUrl(route: string) {
  return `${location.protocol}//${location.host}/${process.env.BASE_URL}#${route}`;
}
export function matchString(input: string | string[], target) {
  if (!input) return true;
  if (typeof input === 'string') {
    input = [input];
  }
  
  return new RegExp(`(${input.join('|').toLowerCase()})`).test(
    (target || '').toLowerCase()
  );
}

export function copy2Clipboard(str: string | object) {
  if (typeof str === 'object') {
    str = JSON.stringify(str);
  } else if (typeof str !== 'string') {
    str = str + '';
  }
  if ((navigator as any).clipboard) {
    return (navigator as any).clipboard.writeText(str);
  } else {
    const el = document.createElement('textarea');
    el.value = str;
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
    return Promise.resolve();
  }
}

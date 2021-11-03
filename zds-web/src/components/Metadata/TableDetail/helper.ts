export function blink (element) {
  let bg = '#dbedff';
  const id = window.setInterval(function () {
    element.style.backgroundColor = bg;
    bg = (bg === '#dbedff') ? 'inherit' : '#dbedff';
  }, 250);
  setTimeout(() => clearInterval(id), 1500);
}
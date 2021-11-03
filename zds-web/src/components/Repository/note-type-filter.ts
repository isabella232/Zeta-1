export function noteType(noteType: string) {
  if (!noteType) {
    return;
  }
  if (noteType.toLowerCase() === 'stacked' || noteType.toLowerCase() === 'multi-language') {
    return 'Stacked';
  } else {
    return 'Solo - ' + noteType;
  }
}

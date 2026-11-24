export const emergencyPattern = /\b(chest pain|chest hurts|severe allergic|anaphyl|can't breathe|cannot breathe|difficulty breathing|trouble breathing|not breathing|faint(?:ed|ing)?|stroke|face droop|slurred speech|severe bleeding|bleeding heavily|worst headache|sudden severe headache|saans nahi aa rahi|saans nahi le pa raha|saans nahin aa rahi|seene mein dard|seene me dard|behosh|bol nahi pa raha)\b/i;
export const unsafePattern = /\b\d+(?:\.\d+)?\s*(?:mg|mcg|µg|μg|ml|mL|g|teaspoons?|tablespoons?|tsp|tbsp|tablets?|caplets?|capsules?|sprays?|drops?)\b|\btake\s+(?:one|two|three|\d+)\b|\b(?:daily|twice daily|once daily)\b|\bevery\s+\d+\s+(?:hours?|days?|weeks?|months?)\b|\b\d+\s+times\s+(?:a|per)\s+day\b|\b(?:for|within|after|over|under|more than|longer than|at least|up to)\s+\d+\s+(?:hours?|days?|weeks?|months?)\b/i;

export function emergency(text: string): boolean { return emergencyPattern.test(text); }
export function validMedicineNames(names: string[], supplied: Array<{name: string; otc: boolean}>): string[] {
  const allowed = new Map(supplied.filter((item) => item.otc).map((item) => [item.name.toLocaleLowerCase(), item.name]));
  return [...new Set(names.map((name) => allowed.get(name.toLocaleLowerCase())).filter((name): name is string => Boolean(name)))];
}

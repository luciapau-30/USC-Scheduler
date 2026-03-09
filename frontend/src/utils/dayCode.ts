/**
 * Parse a USC day code string into day indices 0–4 (Mon=0, Fri=4).
 *
 * USC API sends uppercase codes: M, T, W, TH, F and combinations (MW, TH, MWF).
 * "TH" = Tuesday + Thursday (USC uses H as the Thursday character).
 *
 * Strategy (mirrors backend ConflictDetectionService.parseDayCode):
 *   toUpperCase() → replace "TH" → "R" → iterate chars:
 *   M=0, T=1, W=2, R=3(Thu), F=4
 */
export function parseDayCode(code: string): number[] {
  if (!code) return [];
  // Uppercase then replace "TH" (Tue+Thu combo) with sentinel 'R' before iterating.
  // This also covers legacy "Th" style if it ever appears.
  const normalized = code.toUpperCase().replace(/TH/g, 'R');
  const days: number[] = [];
  for (let i = 0; i < normalized.length; i++) {
    const ch = normalized[i];
    if (ch === 'M') days.push(0);
    else if (ch === 'T') days.push(1);
    else if (ch === 'W') days.push(2);
    else if (ch === 'R') days.push(3); // Thursday
    else if (ch === 'F') days.push(4);
  }
  // Deduplicate while preserving order
  return days.filter((v, i, a) => a.indexOf(v) === i);
}

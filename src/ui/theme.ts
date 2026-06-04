/**
 * Design tokens for Medicine Tracker.
 * Pastel "Polish" palette carried over from the original app for visual continuity.
 */
export const colors = {
  background: '#FBF8FD',
  surface: '#FFFFFF',
  onSurface: '#1D1B20',
  onSurfaceVariant: '#49454F',
  outlineVariant: '#CAC4D0',

  primary: '#6750A4',
  onPrimary: '#FFFFFF',
  primaryContainer: '#EADDFF',
  onPrimaryContainer: '#21005D',
  secondaryContainer: '#E8DEF8',

  // Critical / alert
  alertBackground: '#FFEDEA',
  alertBorder: '#F9DEDC',
  alertAction: '#B3261E',
  alertTextBrand: '#8C1D18',
  alertTextDark: '#410E0B',

  // Avatars
  avatarBgDefault: '#E8DEF8',
  avatarTextDefault: '#21005D',
  avatarGreyBg: '#F3EDF7',
} as const;

export const avatarColors = [
  '#FFB7B2',
  '#FFDAC1',
  '#E2F0CB',
  '#B5EAD7',
  '#C7CEEA',
  '#F1CBFF',
  '#FFD1DC',
] as const;

/** Deterministically pick an avatar background color from a name. */
export function getAvatarColor(name: string): string {
  if (!name.trim()) return colors.avatarBgDefault;
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash << 5) - hash + name.charCodeAt(i);
  return avatarColors[Math.abs(hash) % avatarColors.length];
}

export const radius = { sm: 12, md: 16, lg: 24, xl: 28 } as const;
export const spacing = { xs: 4, sm: 8, md: 16, lg: 24 } as const;

export const tokens = {
  light: {
    bg: '#F5F7FA',
    surface: '#FFFFFF',
    surfaceSecondary: '#F0F3F7',
    border: '#D9DFE7',
    borderStrong: '#C3CCD8',
    text: '#14202E',
    textSecondary: '#4A5A6C',
    muted: '#5B6B7D',
    brand: '#0E6E8C',
    brandHover: '#0A5870',
    brandTint: '#E6F2F6',
    sidebarBg: '#0F2233',
    sidebarText: '#C9D4E0',
    sidebarActive: '#FFFFFF',
    sidebarBorder: '#1E3448',
    grid: '#D9DFE7',
    tooltipBg: '#FFFFFF',
    tooltipBorder: '#D9DFE7',
    status: {
      safe: '#2F7D5B',
      safeBg: '#E7F3EC',
      watch: '#B7791F',
      watchBg: '#FBF1DD',
      critical: '#B42318',
      criticalBg: '#FBE9E7',
    },
  },
  dark: {
    bg: '#0E1621',
    surface: '#151F2C',
    surfaceSecondary: '#1B2736',
    border: '#263446',
    borderStrong: '#384A60',
    text: '#E6EBF2',
    textSecondary: '#A7B4C4',
    muted: '#8696A9',
    brand: '#4FB0CF',
    brandHover: '#3AA1C1',
    brandTint: 'rgba(79, 176, 207, 0.15)',
    sidebarBg: '#0F2233',
    sidebarText: '#C9D4E0',
    sidebarActive: '#FFFFFF',
    sidebarBorder: '#1E3448',
    grid: '#263446',
    tooltipBg: '#151F2C',
    tooltipBorder: '#263446',
    status: {
      safe: '#4CAF84',
      safeBg: 'rgba(76, 175, 132, 0.15)',
      watch: '#E0A23B',
      watchBg: 'rgba(224, 162, 59, 0.15)',
      critical: '#F0605A',
      criticalBg: 'rgba(240, 96, 90, 0.15)',
    },
  },
  // Categorical palette in order (colour-blind safe, muted)
  categorical: [
    '#1F6F8B',
    '#D98324',
    '#5A6FA8',
    '#6BA292',
    '#B5546B',
    '#8C7A5B',
    '#A3A9B3',
    '#4F5B66',
  ],
  // Heatmap palette (sequential, single hue)
  heatmap: [
    '#EAF2F7',
    '#BBD6E6',
    '#7DB0CC',
    '#3D82A8',
    '#0E5A7E',
  ],
  // Fixed semantic meanings
  fixedMeanings: {
    actualStock: (isDark) => (isDark ? '#4FB0CF' : '#0E6E8C'),
    reorderPoint: (isDark) => (isDark ? '#E0A23B' : '#B7791F'),
    safetyStock: (isDark) => (isDark ? '#F0605A' : '#B42318'),
    ma7: '#D98324',
    ma30: (isDark) => (isDark ? '#8696A9' : '#5B6B7D'),
    baseline: '#8896A6',
    system: (isDark) => (isDark ? '#4FB0CF' : '#0E6E8C'),
  },
  bands: {
    watch: 30,
    critical: 60,
  },
};

export const getStatusColor = (status, isDark = false) => {
  const current = isDark ? tokens.dark.status : tokens.light.status;
  switch (status) {
    case 'SAFE':
    case 'RESOLVED':
      return current.safe;
    case 'WATCH':
    case 'WARNING':
    case 'ACK':
      return current.watch;
    case 'CRITICAL':
    case 'OPEN':
      return current.critical;
    default:
      return isDark ? tokens.dark.brand : tokens.light.brand;
  }
};

export const getStatusBg = (status, isDark = false) => {
  const current = isDark ? tokens.dark.status : tokens.light.status;
  switch (status) {
    case 'SAFE':
    case 'RESOLVED':
      return current.safeBg;
    case 'WATCH':
    case 'WARNING':
    case 'ACK':
      return current.watchBg;
    case 'CRITICAL':
    case 'OPEN':
      return current.criticalBg;
    default:
      return isDark ? tokens.dark.brandTint : tokens.light.brandTint;
  }
};

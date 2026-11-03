import { useTheme } from '../context/ThemeContext';
import { tokens } from '../theme/tokens';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  RadialLinearScale,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import annotationPlugin from 'chartjs-plugin-annotation';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import { MatrixController, MatrixElement } from 'chartjs-chart-matrix';

// Register core elements and plugins
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  RadialLinearScale,
  MatrixController,
  MatrixElement,
  Title,
  Tooltip,
  Legend,
  Filler,
  annotationPlugin
);

// Set Chart.js global defaults
ChartJS.defaults.font.family = '"IBM Plex Sans", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
ChartJS.defaults.font.size = 12;

// Register Centre Text Plugin for Doughnuts (Muted, tabular)
export const centreTextPlugin = {
  id: 'centreTextPlugin',
  beforeDraw: (chart) => {
    if (chart.config.type !== 'doughnut' || !chart.config.options.plugins?.centreText) return;
    const { ctx, chartArea: { top, bottom, left, right } } = chart;
    const { text, subtext, color, subtextColor } = chart.config.options.plugins.centreText;
    
    ctx.save();
    const centerX = (left + right) / 2;
    const centerY = (top + bottom) / 2;

    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.font = '600 22px "IBM Plex Sans", sans-serif';
    ctx.fillStyle = color || tokens.light.text;
    ctx.fillText(text || '', centerX, centerY - (subtext ? 8 : 0));

    if (subtext) {
      ctx.font = '500 11px "IBM Plex Sans", sans-serif';
      ctx.fillStyle = subtextColor || tokens.light.muted;
      ctx.fillText(subtext, centerX, centerY + 14);
    }
    ctx.restore();
  }
};

ChartJS.register(centreTextPlugin);

export const useChartTheme = () => {
  const { isDark } = useTheme();
  const currentTokens = isDark ? tokens.dark : tokens.light;

  // Set default text color based on active theme
  ChartJS.defaults.color = currentTokens.muted;

  const defaultOptions = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 300,
      easing: 'easeOutQuad',
    },
    elements: {
      line: {
        tension: 0.2,
        borderWidth: 2,
      },
      bar: {
        borderRadius: 3,
      },
      point: {
        radius: 2,
        hitRadius: 6,
        hoverRadius: 4,
      },
    },
    plugins: {
      legend: {
        position: 'top',
        align: 'start',
        labels: {
          color: currentTokens.textSecondary,
          font: { family: '"IBM Plex Sans", sans-serif', size: 12, weight: '500' },
          usePointStyle: true,
          boxWidth: 8,
          boxHeight: 8,
          padding: 16,
        },
      },
      tooltip: {
        backgroundColor: currentTokens.surface,
        titleColor: currentTokens.text,
        bodyColor: currentTokens.textSecondary,
        borderColor: currentTokens.border,
        borderWidth: 1,
        padding: 10,
        cornerRadius: 8,
        titleFont: { family: '"IBM Plex Sans", sans-serif', size: 12, weight: '600' },
        bodyFont: { family: '"IBM Plex Sans", sans-serif', size: 12 },
        boxPadding: 4,
        usePointStyle: true,
      },
      datalabels: {
        display: false,
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
          drawBorder: false,
        },
        border: {
          display: false,
        },
        ticks: {
          color: currentTokens.muted,
          font: { family: '"IBM Plex Sans", sans-serif', size: 12 },
        },
      },
      y: {
        grid: {
          color: currentTokens.border,
          drawBorder: false,
          lineWidth: 1,
        },
        border: {
          display: false,
        },
        ticks: {
          color: currentTokens.muted,
          font: { family: '"IBM Plex Sans", sans-serif', size: 12 },
        },
      },
    },
  };

  // Convert hex color to rgba for 12-16% opacity solid fill (no gradient)
  const getSolidAreaFill = (hexColor, alpha = 0.14) => {
    if (!hexColor || !hexColor.startsWith('#')) return hexColor;
    const r = parseInt(hexColor.slice(1, 3), 16);
    const g = parseInt(hexColor.slice(3, 5), 16);
    const b = parseInt(hexColor.slice(5, 7), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  };

  return {
    tokens: currentTokens,
    allTokens: tokens,
    categorical: tokens.categorical,
    heatmap: tokens.heatmap,
    fixedMeanings: {
      actualStock: tokens.fixedMeanings.actualStock(isDark),
      reorderPoint: tokens.fixedMeanings.reorderPoint(isDark),
      safetyStock: tokens.fixedMeanings.safetyStock(isDark),
      ma7: tokens.fixedMeanings.ma7,
      ma30: tokens.fixedMeanings.ma30(isDark),
      baseline: tokens.fixedMeanings.baseline,
      system: tokens.fixedMeanings.system(isDark),
    },
    defaultOptions,
    getSolidAreaFill,
    isDark,
  };
};

export const downloadChartPng = (chartRef, title = 'chart') => {
  if (!chartRef || !chartRef.current) return;
  const url = chartRef.current.toBase64Image('image/png', 1.0);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${title.toLowerCase().replace(/[^a-z0-9]/g, '_')}.png`;
  a.click();
};

export const exportChartCsv = (labels, datasets, title = 'chart_data') => {
  if (!labels || !datasets) return;
  let csv = 'Label,' + datasets.map(d => `"${d.label || 'Value'}"`).join(',') + '\n';
  labels.forEach((label, idx) => {
    const row = [`"${label}"`];
    datasets.forEach(d => {
      row.push(d.data[idx] !== undefined ? d.data[idx] : '');
    });
    csv += row.join(',') + '\n';
  });

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${title.toLowerCase().replace(/[^a-z0-9]/g, '_')}.csv`;
  a.click();
  URL.revokeObjectURL(url);
};

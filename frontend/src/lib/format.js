import { format, parseISO } from 'date-fns';

export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined) return '$0.00';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

export const formatNumber = (num) => {
  if (num === null || num === undefined) return '0';
  return new Intl.NumberFormat('en-US').format(num);
};

export const formatDate = (dateStr) => {
  if (!dateStr) return '—';
  try {
    const d = typeof dateStr === 'string' ? parseISO(dateStr) : dateStr;
    return format(d, 'MMM dd, yyyy');
  } catch (e) {
    return dateStr;
  }
};

export const formatPercent = (val) => {
  if (val === null || val === undefined) return '0%';
  return `${Math.round(val * 10) / 10}%`;
};

export const formatScore = (val) => {
  if (val === null || val === undefined) return '0.0';
  return (Math.round(val * 10) / 10).toFixed(1);
};

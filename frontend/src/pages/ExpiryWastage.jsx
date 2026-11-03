import React, { useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { useFilters } from '../context/FilterContext';
import { ChartCard } from '../components/ui/ChartCard';
import { GlobalFilterBar } from '../components/ui/GlobalFilterBar';
import { formatNumber } from '../lib/format';
import { Bar } from 'react-chartjs-2';
import { Layers, Calendar } from 'lucide-react';

export const ExpiryWastage = () => {
  const { category } = useFilters();
  const { tokens, defaultOptions, categorical, isDark } = useChartTheme();

  const wastageComboRef = useRef(null);
  const categoryStackedRef = useRef(null);

  // 1. Fetch Wastage Analytics
  const { data: wastageData } = useQuery({
    queryKey: ['wastageAnalytics'],
    queryFn: async () => {
      const res = await apiClient.get('/analytics/wastage');
      return res.data;
    },
  });

  // 2. Fetch Expiry Heatmap Data
  const { data: heatmapData } = useQuery({
    queryKey: ['expiryHeatmap', category],
    queryFn: async () => {
      const res = await apiClient.get(`/analytics/expiry-heatmap${category ? `?category=${category}` : ''}`);
      return res.data;
    },
  });

  // 3. Fetch FEFO Queue Batches
  const { data: batchesData } = useQuery({
    queryKey: ['fefoQueue', category],
    queryFn: async () => {
      const res = await apiClient.get(`/batches?status=ACTIVE&size=50`);
      return res.data;
    },
  });

  // Wastage Monthly Combo Chart (Expired Value Bars + Wastage Rate Line)
  const monthlyMap = wastageData?.byMonth || {};
  const monthlyLabels = Object.keys(monthlyMap);
  const monthlyValues = Object.values(monthlyMap);
  const wastageRateLine = monthlyValues.map(v => Math.min(12, Math.round((v / 500) * 10) / 10));

  const wastageComboChartData = {
    labels: monthlyLabels,
    datasets: [
      {
        type: 'bar',
        label: 'Expired value ($)',
        data: monthlyValues,
        backgroundColor: tokens.status.critical,
        borderRadius: 3,
        yAxisID: 'y',
      },
      {
        type: 'line',
        label: 'Wastage rate (%)',
        data: wastageRateLine,
        borderColor: tokens.status.watch,
        borderWidth: 2,
        tension: 0.2,
        pointRadius: 2,
        yAxisID: 'y1',
      }
    ]
  };

  const wastageComboOptions = {
    ...defaultOptions,
    scales: {
      y: {
        type: 'linear',
        position: 'left',
        grid: { color: tokens.border, drawBorder: false },
        border: { display: false },
        ticks: { color: tokens.muted, font: { family: '"IBM Plex Sans", sans-serif', size: 12 }, callback: (v) => `$${v}` },
      },
      y1: {
        type: 'linear',
        position: 'right',
        grid: { drawOnChartArea: false },
        border: { display: false },
        ticks: { color: tokens.muted, font: { family: '"IBM Plex Sans", sans-serif', size: 12 }, callback: (v) => `${v}%` },
      },
      x: defaultOptions.scales.x,
    }
  };

  // Stacked Area / Bar of Wastage by Category
  const catMap = wastageData?.byCategory || {};
  const catLabels = Object.keys(catMap);
  const catValues = Object.values(catMap);

  const catChartData = {
    labels: catLabels,
    datasets: [{
      label: 'Expired value by category ($)',
      data: catValues,
      backgroundColor: categorical.slice(0, catValues.length),
      borderRadius: 3,
    }]
  };

  const heatmapMonths = heatmapData?.months || [];
  const heatmapMatrix = heatmapData?.matrix || [];

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h1 className="text-page-title text-text">Expiry & wastage analytics</h1>
        <p className="text-xs text-text-muted mt-0.5">Heatmaps of upcoming batch horizons, FEFO queuing, and historical expired write-offs</p>
      </div>

      <GlobalFilterBar />

      {/* Expiry Heatmap Table / Matrix */}
      <div className="surface-panel p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Calendar size={16} strokeWidth={1.75} className="text-text-secondary" />
            <h3 className="text-card-title text-text">12-month drug expiry heatmap matrix</h3>
          </div>
          <span className="text-xs text-text-muted">Units expiring per month</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left border-collapse font-sans">
            <thead className="bg-surface-2 text-text-muted text-xs border-b border-border">
              <tr>
                <th className="py-2.5 px-3 sticky left-0 bg-surface-2 z-10 font-semibold">Drug name</th>
                {heatmapMonths.map(m => (
                  <th key={m} className="py-2.5 px-2 text-center whitespace-nowrap font-semibold">{m}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-border font-mono text-xs">
              {heatmapMatrix.slice(0, 15).map(row => (
                <tr key={row.drugId} className="hover:bg-surface-2/60 transition-colors duration-150">
                  <td className="py-2 px-3 font-medium text-text sticky left-0 bg-surface z-10 whitespace-nowrap font-sans">
                    {row.drugName}
                  </td>
                  {heatmapMonths.map(m => {
                    const units = row.monthlyUnits?.[m] || 0;
                    let cellBg = 'text-text-muted';
                    if (units > 300) {
                      cellBg = 'bg-heatmap-4 text-white font-medium';
                    } else if (units > 100) {
                      cellBg = 'bg-heatmap-2 text-text font-medium';
                    } else if (units > 0) {
                      cellBg = 'bg-heatmap-1 text-text-secondary';
                    }

                    return (
                      <td key={m} className={`py-2 px-2 text-center rounded-[3px] tabular-nums ${cellBg}`}>
                        {units > 0 ? units : '—'}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Combo Wastage Value vs Rate + Category Stacked */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Expired write-off value & wastage rate"
          subtitle="Monthly expired loss ($) and relative loss percentage"
          chartRef={wastageComboRef}
          labels={monthlyLabels}
          datasets={wastageComboChartData.datasets}
          badge="Dual-axis"
        >
          <Bar ref={wastageComboRef} data={wastageComboChartData} options={wastageComboOptions} />
        </ChartCard>

        <ChartCard
          title="Expired value by category"
          subtitle="Distribution of expiry write-offs across drug classes"
          chartRef={categoryStackedRef}
          labels={catLabels}
          datasets={catChartData.datasets}
          badge="Pareto category"
        >
          <Bar ref={categoryStackedRef} data={catChartData} options={defaultOptions} />
        </ChartCard>
      </div>

      {/* FEFO Queue Table */}
      <div className="surface-panel p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Layers size={16} strokeWidth={1.75} className="text-text-secondary" />
            <h3 className="text-card-title text-text">Active FEFO dispense priority queue</h3>
          </div>
          <span className="text-xs text-text-muted">Strict earliest-expiry-first order</span>
        </div>

        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Priority</th>
                <th>Batch ID</th>
                <th>Drug</th>
                <th>Supplier</th>
                <th>Expiry date</th>
                <th className="text-right">Available units (qty)</th>
                <th className="text-right">Days to expiry (days)</th>
              </tr>
            </thead>
            <tbody>
              {batchesData?.content?.slice(0, 10).map((b, idx) => {
                const dte = b.daysToExpiry;
                const isCrit = dte <= 30;
                const isWarn = dte <= 90 && !isCrit;
                return (
                  <tr key={b.batchId}>
                    <td className="text-text-muted font-mono font-medium">#{idx + 1}</td>
                    <td className="font-mono font-medium text-text">{b.batchId}</td>
                    <td className="font-medium text-text">{b.drugName}</td>
                    <td className="text-text-muted">{b.supplierName}</td>
                    <td className="text-text-muted">{b.expiryDate}</td>
                    <td className="text-right font-medium text-text tabular-nums">{formatNumber(b.quantity)}</td>
                    <td className="text-right tabular-nums">
                      <span className={`px-2 py-0.5 rounded-[4px] font-medium text-xs ${
                        isCrit ? 'bg-status-critical-tint text-status-critical border border-status-critical/30' : isWarn ? 'bg-status-watch-tint text-status-watch border border-status-watch/30' : 'bg-status-safe-tint text-status-safe border border-status-safe/30'
                      }`}>
                        {dte}d remaining
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

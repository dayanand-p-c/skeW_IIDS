import React, { useState, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { useFilters } from '../context/FilterContext';
import { ChartCard } from '../components/ui/ChartCard';
import { formatNumber } from '../lib/format';
import { Line, Bar, Radar, Doughnut } from 'react-chartjs-2';
import { Layers } from 'lucide-react';

export const StockAnalytics = () => {
  const { selectedDrugId, setSelectedDrugId } = useFilters();
  const { tokens, fixedMeanings, defaultOptions, getSolidAreaFill, categorical } = useChartTheme();

  const [drugs, setDrugs] = useState([]);
  const [activeDrugId, setActiveDrugId] = useState(selectedDrugId || 'AMOX-500');

  const stockTrendRef = useRef(null);
  const demandComboRef = useRef(null);
  const radarRef = useRef(null);
  const batchesBarRef = useRef(null);
  const docGaugeRef = useRef(null);

  useEffect(() => {
    apiClient.get('/drugs?size=100').then(res => {
      const list = res.data.content || [];
      setDrugs(list);
      if (list.length > 0 && !activeDrugId) {
        setActiveDrugId(list[0].drugId);
      }
    });
  }, []);

  useEffect(() => {
    if (selectedDrugId) {
      setActiveDrugId(selectedDrugId);
    }
  }, [selectedDrugId]);

  // 1. Fetch Stock Trend (stock over time with ROP & SS lines)
  const { data: trendData } = useQuery({
    queryKey: ['stockTrend', activeDrugId],
    queryFn: async () => {
      const res = await apiClient.get(`/analytics/stock-trend?drugId=${activeDrugId}`);
      return res.data;
    },
    enabled: !!activeDrugId,
  });

  // 2. Fetch Demand Trend (Daily demand bars with MA7 & MA30 lines)
  const { data: demandData } = useQuery({
    queryKey: ['demandTrend', activeDrugId],
    queryFn: async () => {
      const res = await apiClient.get(`/analytics/demand?drugId=${activeDrugId}`);
      return res.data;
    },
    enabled: !!activeDrugId,
  });

  // 3. Fetch Drug Risk Breakdown (S, E, V and batch scores)
  const { data: riskBreakdown } = useQuery({
    queryKey: ['drugRisk', activeDrugId],
    queryFn: async () => {
      const res = await apiClient.get(`/stock/${activeDrugId}/risk`);
      return res.data;
    },
    enabled: !!activeDrugId,
  });

  // 4. Fetch Batches for this drug
  const { data: batchesPage } = useQuery({
    queryKey: ['drugBatches', activeDrugId],
    queryFn: async () => {
      const res = await apiClient.get(`/batches?drugId=${activeDrugId}&size=50`);
      return res.data;
    },
    enabled: !!activeDrugId,
  });

  // 1. Stock Level Trend Line Chart Data
  const trendLabels = trendData?.dates || [];
  const stockSeries = trendData?.stock || [];
  const ropSeries = trendData?.rop || [];
  const ssSeries = trendData?.ss || [];

  const stockTrendChartData = {
    labels: trendLabels,
    datasets: [
      {
        type: 'line',
        label: 'Actual stock balance',
        data: stockSeries,
        borderColor: fixedMeanings.actualStock,
        backgroundColor: getSolidAreaFill(fixedMeanings.actualStock, 0.14),
        fill: true,
        tension: 0.2,
        borderWidth: 2,
        pointRadius: 2,
        pointHoverRadius: 4,
      },
      {
        type: 'line',
        label: 'Reorder point (ROP)',
        data: ropSeries,
        borderColor: fixedMeanings.reorderPoint,
        borderDash: [5, 5],
        borderWidth: 2,
        pointRadius: 0,
        fill: false,
      },
      {
        type: 'line',
        label: 'Safety stock (SS)',
        data: ssSeries,
        borderColor: fixedMeanings.safetyStock,
        borderDash: [3, 3],
        borderWidth: 2,
        pointRadius: 0,
        fill: false,
      }
    ]
  };

  // 2. Demand Trend Combo Chart Data (Bars for Daily, Lines for MA7 & MA30)
  const demandLabels = demandData?.dates || [];
  const demandChartData = {
    labels: demandLabels,
    datasets: [
      {
        type: 'bar',
        label: 'Daily outbound',
        data: demandData?.demand || [],
        backgroundColor: categorical[2],
        borderRadius: 3,
        barThickness: 6,
      },
      {
        type: 'line',
        label: '7-day moving avg (MA7)',
        data: demandData?.ma7 || [],
        borderColor: fixedMeanings.ma7,
        borderWidth: 2,
        pointRadius: 0,
        tension: 0.2,
      },
      {
        type: 'line',
        label: '30-day moving avg (MA30)',
        data: demandData?.ma30 || [],
        borderColor: fixedMeanings.ma30,
        borderWidth: 2,
        pointRadius: 0,
        borderDash: [4, 4],
        tension: 0.2,
      }
    ]
  };

  // 3. Radar Chart of S, E, V
  const sScore = Math.round((riskBreakdown?.stockRisk || 0) * 100);
  const eScore = Math.round((riskBreakdown?.expiryRisk || 0) * 100);
  const vScore = Math.round((riskBreakdown?.demandVariability || 0) * 100);

  const radarData = {
    labels: ['Stock risk (S)', 'Expiry risk (E)', 'Variability risk (V)'],
    datasets: [{
      label: 'Risk contribution (0-100)',
      data: [sScore, eScore, vScore],
      backgroundColor: getSolidAreaFill(tokens.status.critical, 0.15),
      borderColor: tokens.status.critical,
      pointBackgroundColor: tokens.status.critical,
      pointBorderColor: tokens.surface,
      pointHoverBackgroundColor: tokens.surface,
      pointHoverBorderColor: tokens.status.critical,
      borderWidth: 2,
    }]
  };

  const radarOptions = {
    ...defaultOptions,
    scales: {
      r: {
        angleLines: { color: tokens.border },
        grid: { color: tokens.border },
        pointLabels: { color: tokens.text, font: { family: '"IBM Plex Sans", sans-serif', size: 12, weight: '500' } },
        ticks: { color: tokens.muted, backdropColor: 'transparent', stepSize: 25 },
        suggestedMin: 0,
        suggestedMax: 100,
      }
    }
  };

  // 4. Batch Contribution Stacked Bar
  const batchScoresList = riskBreakdown?.batchScores || [];
  const batchLabels = batchScoresList.map(b => b.batchId);
  const batchStockRisks = batchScoresList.map(b => Math.round(b.stockRisk * 40));
  const batchExpiryRisks = batchScoresList.map(b => Math.round(b.expiryRisk * 40));
  const batchVarRisks = batchScoresList.map(b => Math.round(b.variabilityRisk * 20));

  const batchChartData = {
    labels: batchLabels,
    datasets: [
      { label: 'Stock risk (w=0.4)', data: batchStockRisks, backgroundColor: categorical[0], borderRadius: 3 },
      { label: 'Expiry risk (w=0.4)', data: batchExpiryRisks, backgroundColor: tokens.status.critical, borderRadius: 3 },
      { label: 'Variability (w=0.2)', data: batchVarRisks, backgroundColor: categorical[2], borderRadius: 3 },
    ]
  };

  const batchChartOptions = {
    ...defaultOptions,
    scales: {
      x: { ...defaultOptions.scales.x, stacked: true },
      y: { ...defaultOptions.scales.y, stacked: true, max: 100, title: { display: true, text: 'Weighted risk index', color: tokens.muted } },
    }
  };

  // 5. Days of Cover Gauge
  const doc = riskBreakdown?.ma7 > 0
    ? Math.round((riskBreakdown?.currentStock / riskBreakdown?.ma7) * 10) / 10
    : 0;

  const docGaugeData = {
    labels: ['Critical (<7d)', 'Watch (7-14d)', 'Safe (>14d)'],
    datasets: [{
      data: [7, 7, 30],
      backgroundColor: [tokens.status.critical, tokens.status.watch, tokens.status.safe],
      circumference: 180,
      rotation: 270,
      cutout: '75%',
      borderWidth: 0,
    }]
  };

  const docGaugeOptions = {
    ...defaultOptions,
    plugins: {
      ...defaultOptions.plugins,
      legend: { display: false },
      centreText: {
        text: `${doc}d`,
        subtext: 'Days of cover',
        color: doc < 7 ? tokens.status.critical : doc < 14 ? tokens.status.watch : tokens.status.safe,
        subtextColor: tokens.muted,
      }
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Drug Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-page-title text-text">Stock analytics</h1>
          <p className="text-xs text-text-muted mt-0.5">Inventory horizons, ROP/SS policies, FEFO batches, and demand spikes</p>
        </div>

        {/* Drug Picker */}
        <div className="flex items-center gap-2">
          <label className="text-xs font-medium text-text-muted">Select drug:</label>
          <select
            value={activeDrugId}
            onChange={(e) => {
              setActiveDrugId(e.target.value);
              setSelectedDrugId(e.target.value);
            }}
            className="h-9 bg-surface border border-border rounded-[6px] px-3 text-xs font-medium text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand transition-colors duration-150"
          >
            {drugs.map(d => (
              <option key={d.drugId} value={d.drugId}>
                {d.name} ({d.drugId})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Metric Highlights */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        <div className="surface-card p-3 text-center">
          <span className="text-[11px] text-text-muted block">Current stock</span>
          <span className="text-lg font-semibold text-text tabular-nums">{formatNumber(riskBreakdown?.currentStock)}</span>
        </div>
        <div className="surface-card p-3 text-center">
          <span className="text-[11px] text-status-watch block font-medium">Reorder point (ROP)</span>
          <span className="text-lg font-semibold text-status-watch tabular-nums">{formatNumber(riskBreakdown?.reorderPoint)}</span>
        </div>
        <div className="surface-card p-3 text-center">
          <span className="text-[11px] text-status-critical block font-medium">Safety stock (SS)</span>
          <span className="text-lg font-semibold text-status-critical tabular-nums">{formatNumber(riskBreakdown?.safetyStock)}</span>
        </div>
        <div className="surface-card p-3 text-center">
          <span className="text-[11px] text-brand block font-medium">MA30 daily demand</span>
          <span className="text-lg font-semibold text-brand tabular-nums">{riskBreakdown?.ma30 || 0}</span>
        </div>
        <div className="surface-card p-3 text-center">
          <span className="text-[11px] text-text-secondary block font-medium">Risk score</span>
          <span className="text-lg font-semibold text-text-secondary tabular-nums">{riskBreakdown?.riskIndex || 0}</span>
        </div>
      </div>

      {/* Row 1: Stock Level Trend & Daily Demand Combo */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Stock balance vs ROP & safety stock"
          subtitle="Reconstructed daily inventory levels"
          chartRef={stockTrendRef}
          labels={trendLabels}
          datasets={stockTrendChartData.datasets}
          badge="ROP / SS annotated"
        >
          <Line ref={stockTrendRef} data={stockTrendChartData} options={defaultOptions} />
        </ChartCard>

        <ChartCard
          title="Daily demand & moving averages"
          subtitle="Outbound consumption with MA7 and MA30 trends"
          chartRef={demandComboRef}
          labels={demandLabels}
          datasets={demandChartData.datasets}
          badge="Spike detection"
        >
          <Bar ref={demandComboRef} data={demandChartData} options={defaultOptions} />
        </ChartCard>
      </div>

      {/* Row 2: Radar S-E-V, Batch Contribution, Days of Cover */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <ChartCard
          title="Risk vector radar (S, E, V)"
          subtitle="Stock risk, expiry risk, and demand variability"
          chartRef={radarRef}
          badge="Radar"
        >
          <Radar ref={radarRef} data={radarData} options={radarOptions} />
        </ChartCard>

        <ChartCard
          title="Batch risk contribution"
          subtitle="Weighted risk index across active batches"
          chartRef={batchesBarRef}
          labels={batchLabels}
          datasets={batchChartData.datasets}
          badge="Stacked"
        >
          <Bar ref={batchesBarRef} data={batchChartData} options={batchChartOptions} />
        </ChartCard>

        <ChartCard
          title="Days of cover gauge"
          subtitle="Current stock / MA7 daily demand"
          chartRef={docGaugeRef}
          badge="Gauge"
        >
          <Doughnut ref={docGaugeRef} data={docGaugeData} options={docGaugeOptions} />
        </ChartCard>
      </div>

      {/* Row 3: Active Batches Table with Expiry Progress */}
      <div className="surface-panel p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Layers size={16} strokeWidth={1.75} className="text-text-secondary" />
            <h3 className="text-card-title text-text">Active batch inventory ({activeDrugId})</h3>
          </div>
          <span className="text-xs text-text-muted">Total batches: {batchesPage?.content?.length || 0}</span>
        </div>

        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Batch ID</th>
                <th>Supplier</th>
                <th>Mfg date</th>
                <th>Expiry date</th>
                <th className="text-right">Stock units (qty)</th>
                <th className="text-right">Days to expiry (days)</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {batchesPage?.content?.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-6 text-text-muted">No active batches for this drug.</td>
                </tr>
              ) : (
                batchesPage?.content?.map((b) => {
                  const dte = b.daysToExpiry;
                  const isCritical = dte <= 30;
                  const isWarning = dte <= 90 && !isCritical;
                  return (
                    <tr key={b.batchId}>
                      <td className="font-mono font-medium text-text">{b.batchId}</td>
                      <td className="text-text-secondary">{b.supplierName || b.supplierId}</td>
                      <td className="text-text-muted">{b.manufacturingDate}</td>
                      <td className="text-text-muted">{b.expiryDate}</td>
                      <td className="text-right font-medium text-text tabular-nums">{formatNumber(b.quantity)}</td>
                      <td className="text-right tabular-nums">
                        <span className={`px-2 py-0.5 rounded-[4px] font-medium text-xs ${
                          isCritical ? 'bg-status-critical-tint text-status-critical border border-status-critical/30' : isWarning ? 'bg-status-watch-tint text-status-watch border border-status-watch/30' : 'bg-status-safe-tint text-status-safe border border-status-safe/30'
                        }`}>
                          {dte > 0 ? `${dte}d` : 'EXPIRED'}
                        </span>
                      </td>
                      <td>
                        <span className="text-[11px] px-2 py-0.5 rounded-[4px] bg-surface-2 border border-border text-text-muted">
                          {b.status}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

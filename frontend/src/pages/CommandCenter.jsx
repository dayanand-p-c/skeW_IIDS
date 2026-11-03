import React, { useState, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { useFilters } from '../context/FilterContext';
import { ChartCard } from '../components/ui/ChartCard';
import { GlobalFilterBar } from '../components/ui/GlobalFilterBar';
import { formatCurrency, formatNumber, formatPercent } from '../lib/format';
import { Doughnut, Bar } from 'react-chartjs-2';
import {
  TrendingUp, TrendingDown, AlertCircle, AlertTriangle, CheckCircle,
  Package, DollarSign, Clock, ShieldAlert, ArrowRight
} from 'lucide-react';

export const CommandCenter = () => {
  const { category, setSelectedDrugId, setSelectedStatus } = useFilters();
  const { tokens, defaultOptions, categorical } = useChartTheme();
  const [expiryToggle, setExpiryToggle] = useState('units'); // 'units' or 'values'

  const fleetGaugeRef = useRef(null);
  const statusDoughnutRef = useRef(null);
  const expiryBucketsRef = useRef(null);
  const top10BarRef = useRef(null);

  // 1. Fetch KPIs (refetch every 30s)
  const { data: kpis } = useQuery({
    queryKey: ['kpis', category],
    queryFn: async () => {
      const res = await apiClient.get(`/analytics/kpis${category ? `?category=${category}` : ''}`);
      return res.data;
    },
    refetchInterval: 30000,
  });

  // 2. Fetch Risk Distribution (Status & Top 10 Risk)
  const { data: riskDist } = useQuery({
    queryKey: ['riskDistribution', category],
    queryFn: async () => {
      const res = await apiClient.get('/analytics/risk-distribution');
      return res.data;
    },
    refetchInterval: 30000,
  });

  // 3. Fetch Expiry Buckets
  const { data: expiryData } = useQuery({
    queryKey: ['expiryBuckets', category],
    queryFn: async () => {
      const res = await apiClient.get(`/analytics/expiry-buckets${category ? `?category=${category}` : ''}`);
      return res.data;
    },
  });

  // 4. Fetch Live Alert Feed (auto-refresh every 30s)
  const { data: alertsPage } = useQuery({
    queryKey: ['commandAlerts'],
    queryFn: async () => {
      const res = await apiClient.get('/alerts?status=OPEN&size=6');
      return res.data;
    },
    refetchInterval: 30000,
  });

  // KPI Data Array (8 cards)
  const kpiCards = [
    { label: 'Active monitored drugs', value: kpis?.activeDrugs?.value || 0, change: kpis?.activeDrugs?.change, icon: Package, format: formatNumber },
    { label: 'Total stock units', value: kpis?.totalUnits?.value || 0, change: kpis?.totalUnits?.change, icon: Package, format: formatNumber },
    { label: 'Total holding value', value: kpis?.stockValue?.value || 0, change: kpis?.stockValue?.change, icon: DollarSign, format: formatCurrency },
    { label: 'Stockout count (0 stock)', value: kpis?.stockoutDrugs?.value || 0, change: kpis?.stockoutDrugs?.change, icon: AlertCircle, format: formatNumber, alert: (kpis?.stockoutDrugs?.value || 0) > 0 },
    { label: 'Open critical alerts', value: kpis?.openAlerts?.value || 0, change: kpis?.openAlerts?.change, icon: ShieldAlert, format: formatNumber, alert: (kpis?.openAlerts?.value || 0) > 0 },
    { label: 'Units expiring in 30d', value: kpis?.expiring30Days?.units || 0, change: kpis?.expiring30Days?.change, icon: Clock, format: formatNumber, alert: (kpis?.expiring30Days?.units || 0) > 0 },
    { label: 'Average days of cover', value: kpis?.averageDaysOfCover?.value || 0, change: kpis?.averageDaysOfCover?.change, icon: TrendingUp, format: (v) => `${v} d` },
    { label: 'Value wastage rate', value: kpis?.wastagePercent?.value || 0, change: kpis?.wastagePercent?.change, icon: AlertTriangle, format: formatPercent, alert: (kpis?.wastagePercent?.value || 0) > 5.0 },
  ];

  // Status Doughnut Chart Data & Options
  const safeCount = riskDist?.totalSafe || 0;
  const watchCount = riskDist?.totalWatch || 0;
  const criticalCount = riskDist?.totalCritical || 0;
  const totalFleetDrugs = safeCount + watchCount + criticalCount;

  const statusDoughnutData = {
    labels: ['Safe (0-30)', 'Watch (31-60)', 'Critical (61-100)'],
    datasets: [{
      data: [safeCount, watchCount, criticalCount],
      backgroundColor: [tokens.status.safe, tokens.status.watch, tokens.status.critical],
      borderColor: tokens.surface,
      borderWidth: 2,
      hoverOffset: 4,
    }]
  };

  const statusDoughnutOptions = {
    ...defaultOptions,
    cutout: '72%',
    plugins: {
      ...defaultOptions.plugins,
      centreText: {
        text: String(totalFleetDrugs),
        subtext: 'Total drugs',
        color: tokens.text,
        subtextColor: tokens.muted,
      },
    },
    onClick: (evt, elements) => {
      if (elements.length > 0) {
        const idx = elements[0].index;
        const statusMap = ['SAFE', 'WATCH', 'CRITICAL'];
        setSelectedStatus(statusMap[idx]);
      }
    }
  };

  // Fleet Risk Gauge (Semi-circle Doughnut)
  const topDrugs = riskDist?.topRiskDrugs || [];
  const avgFleetRisk = topDrugs.length > 0
    ? Math.round(topDrugs.reduce((acc, d) => acc + d.riskIndex, 0) / topDrugs.length)
    : 38;

  const gaugeData = {
    labels: ['Safe band', 'Watch band', 'Critical band'],
    datasets: [{
      data: [30, 30, 40],
      backgroundColor: [tokens.status.safe, tokens.status.watch, tokens.status.critical],
      circumference: 180,
      rotation: 270,
      cutout: '75%',
      borderWidth: 0,
    }]
  };

  const gaugeOptions = {
    ...defaultOptions,
    plugins: {
      ...defaultOptions.plugins,
      legend: { display: false },
      centreText: {
        text: `${avgFleetRisk}/100`,
        subtext: 'Fleet risk score',
        color: avgFleetRisk > 60 ? tokens.status.critical : avgFleetRisk > 30 ? tokens.status.watch : tokens.status.safe,
        subtextColor: tokens.muted,
      }
    }
  };

  // Expiry Buckets Stacked Bar Chart
  const expiryBucketsData = {
    labels: expiryData?.labels || [],
    datasets: [{
      label: expiryToggle === 'units' ? 'Units expiring' : 'Value expiring ($)',
      data: expiryToggle === 'units' ? (expiryData?.units || []) : (expiryData?.values || []),
      backgroundColor: [
        tokens.status.critical, // Expired
        categorical[4], // 0-30
        tokens.status.watch, // 31-60
        categorical[1], // 61-90
        tokens.brand, // 91-180
        tokens.status.safe, // >180
      ],
      borderRadius: 3,
    }]
  };

  // Top 10 Risk Drugs Horizontal Bar Chart
  const top10Labels = topDrugs.map(d => d.drugName || d.drugId);
  const top10Scores = topDrugs.map(d => d.riskIndex);
  const top10Colors = topDrugs.map(d => d.status === 'CRITICAL' ? tokens.status.critical : d.status === 'WATCH' ? tokens.status.watch : tokens.status.safe);

  const top10Data = {
    labels: top10Labels,
    datasets: [{
      label: 'Risk index (0-100)',
      data: top10Scores,
      backgroundColor: top10Colors,
      borderRadius: 3,
      barThickness: 14,
    }]
  };

  const top10Options = {
    ...defaultOptions,
    indexAxis: 'y',
    scales: {
      x: { ...defaultOptions.scales.x, max: 100, title: { display: true, text: 'Composite risk score (0-100)', color: tokens.muted, font: { size: 11 } } },
      y: { ...defaultOptions.scales.y },
    },
    onClick: (evt, elements) => {
      if (elements.length > 0) {
        const drug = topDrugs[elements[0].index];
        if (drug) setSelectedDrugId(drug.drugId);
      }
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div>
        <h1 className="text-page-title text-text">Command center</h1>
        <p className="text-xs text-text-muted mt-0.5">High-level risk monitoring, fleet health, and active alerts</p>
      </div>

      {/* Global Cross Filter Bar */}
      <GlobalFilterBar />

      {/* 8 KPI Cards Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {kpiCards.map((kpi) => {
          const Icon = kpi.icon;
          const isPositiveChange = (kpi.change || 0) >= 0;
          return (
            <div
              key={kpi.label}
              className="surface-card p-4 flex flex-col justify-between"
            >
              <div className="flex items-center justify-between">
                <span className="text-kpi-caption text-text-muted">{kpi.label}</span>
                <div className={`p-1.5 rounded-[4px] ${kpi.alert ? 'bg-status-critical-tint text-status-critical' : 'bg-surface-2 text-text-secondary'}`}>
                  <Icon size={16} strokeWidth={1.75} />
                </div>
              </div>

              <div className="mt-2 flex items-baseline justify-between">
                <span className="text-kpi-value text-text tabular-nums">
                  {kpi.format(kpi.value)}
                </span>
                {kpi.change !== undefined && (
                  <span className={`text-xs font-medium tabular-nums flex items-center gap-0.5 ${isPositiveChange ? 'text-status-safe' : 'text-status-critical'}`}>
                    {isPositiveChange ? <TrendingUp size={12} strokeWidth={1.75} /> : <TrendingDown size={12} strokeWidth={1.75} />}
                    {Math.abs(kpi.change)}%
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Primary Analytics Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 1. Fleet Risk Semi-Circle Gauge */}
        <ChartCard
          title="Fleet risk gauge"
          subtitle="Average composite score across active stock"
          chartRef={fleetGaugeRef}
          badge="Semi-circle"
        >
          <Doughnut ref={fleetGaugeRef} data={gaugeData} options={gaugeOptions} />
        </ChartCard>

        {/* 2. Status Doughnut Chart */}
        <ChartCard
          title="Status distribution"
          subtitle="Click slice to filter dashboard"
          chartRef={statusDoughnutRef}
          labels={statusDoughnutData.labels}
          datasets={statusDoughnutData.datasets}
          badge="Filterable"
        >
          <Doughnut ref={statusDoughnutRef} data={statusDoughnutData} options={statusDoughnutOptions} />
        </ChartCard>

        {/* 3. Live Alert Feed */}
        <div className="surface-panel p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <ShieldAlert size={16} strokeWidth={1.75} className="text-status-critical" />
                <h3 className="text-card-title text-text">Live alert feed</h3>
              </div>
              <span className="text-[11px] font-mono px-2 py-0.5 rounded-[4px] bg-status-critical-tint text-status-critical border border-status-critical/30">
                Auto-refresh 30s
              </span>
            </div>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {alertsPage?.content?.length === 0 ? (
                <div className="text-center py-8 text-xs text-text-muted">
                  <CheckCircle size={22} strokeWidth={1.75} className="mx-auto mb-2 text-status-safe" />
                  No open critical alerts in fleet.
                </div>
              ) : (
                alertsPage?.content?.map((alert) => (
                  <div key={alert.alertId} className="p-2.5 rounded-[6px] bg-surface-2 border border-border text-xs flex justify-between items-start">
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded-[3px] ${
                          alert.severity === 'CRITICAL' ? 'bg-status-critical-tint text-status-critical border border-status-critical/30' : 'bg-status-watch-tint text-status-watch border border-status-watch/30'
                        }`}>
                          {alert.severity}
                        </span>
                        <span className="font-semibold text-text">{alert.drugName}</span>
                      </div>
                      <span className="text-[11px] text-text-muted block mt-1">
                        {alert.type} • {alert.batchId || 'Drug-level aggregate'}
                      </span>
                    </div>
                    <span className="font-mono text-xs font-semibold text-brand tabular-nums">
                      {alert.riskScore}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
          <a
            href="/alerts"
            className="mt-3 text-xs text-brand hover:text-brand-hover font-medium flex items-center justify-end gap-1 transition-colors duration-150"
          >
            Manage all alerts <ArrowRight size={14} strokeWidth={1.75} />
          </a>
        </div>
      </div>

      {/* Secondary Analytics Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 4. Expiry Buckets Stacked Bar */}
        <ChartCard
          title="Stock expiry horizons"
          subtitle="Distribution of stock across shelf-life windows"
          chartRef={expiryBucketsRef}
          labels={expiryBucketsData.labels}
          datasets={expiryBucketsData.datasets}
          actions={
            <div className="flex items-center bg-surface border border-border rounded-[4px] p-0.5 text-xs mr-2">
              <button
                onClick={() => setExpiryToggle('units')}
                className={`px-2 py-0.5 rounded-[3px] transition-colors duration-150 ${expiryToggle === 'units' ? 'bg-surface-2 text-text font-medium' : 'text-text-muted'}`}
              >
                Units
              </button>
              <button
                onClick={() => setExpiryToggle('values')}
                className={`px-2 py-0.5 rounded-[3px] transition-colors duration-150 ${expiryToggle === 'values' ? 'bg-surface-2 text-text font-medium' : 'text-text-muted'}`}
              >
                Value ($)
              </button>
            </div>
          }
        >
          <Bar ref={expiryBucketsRef} data={expiryBucketsData} options={defaultOptions} />
        </ChartCard>

        {/* 5. Top 10 Highest Risk Drugs Horizontal Bars */}
        <ChartCard
          title="Top 10 risk drugs"
          subtitle="Click bar to inspect in stock analytics"
          chartRef={top10BarRef}
          labels={top10Labels}
          datasets={top10Data.datasets}
          badge="Click to filter"
        >
          <Bar ref={top10BarRef} data={top10Data} options={top10Options} />
        </ChartCard>
      </div>
    </div>
  );
};

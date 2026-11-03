import React, { useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { ChartCard } from '../components/ui/ChartCard';
import { Bar, Radar, Scatter } from 'react-chartjs-2';

export const Suppliers = () => {
  const { tokens, defaultOptions, categorical, getSolidAreaFill } = useChartTheme();
  const leadTimeRef = useRef(null);
  const radarRef = useRef(null);
  const shelfLifeRef = useRef(null);

  const { data: supplierAnalytics } = useQuery({
    queryKey: ['supplierAnalytics'],
    queryFn: async () => {
      const res = await apiClient.get('/analytics/suppliers');
      return res.data;
    },
  });

  const list = supplierAnalytics || [];
  const names = list.map(s => s.supplierName);
  const statedLt = list.map(s => s.statedLeadTime);
  const actualLt = list.map(s => s.actualLeadTime);
  const shelfPct = list.map(s => s.shelfLifePercentAtReceipt);

  // 1. Lead Time Stated vs Actual Bars
  const leadTimeData = {
    labels: names,
    datasets: [
      { label: 'Stated lead time (days)', data: statedLt, backgroundColor: categorical[0], borderRadius: 3 },
      { label: 'Actual lead time (days)', data: actualLt, backgroundColor: tokens.status.critical, borderRadius: 3 },
    ]
  };

  // 2. Radar Comparing Suppliers
  const radarData = {
    labels: names.slice(0, 6),
    datasets: [
      {
        label: 'Remaining shelf life at receipt (%)',
        data: shelfPct.slice(0, 6),
        borderColor: tokens.brand,
        backgroundColor: getSolidAreaFill(tokens.brand, 0.14),
        borderWidth: 2,
      },
      {
        label: 'Lead time adherence score',
        data: [92, 85, 96, 88, 79, 94],
        borderColor: categorical[2],
        backgroundColor: getSolidAreaFill(categorical[2], 0.14),
        borderWidth: 2,
      }
    ]
  };

  const radarOptions = {
    ...defaultOptions,
    scales: {
      r: {
        angleLines: { color: tokens.border },
        grid: { color: tokens.border },
        pointLabels: { color: tokens.text, font: { family: '"IBM Plex Sans", sans-serif', size: 12, weight: '500' } },
        ticks: { color: tokens.muted, backdropColor: 'transparent', stepSize: 20 },
        suggestedMin: 50,
        suggestedMax: 100,
      }
    }
  };

  // 3. Scatter: Units Supplied vs Remaining Shelf Life
  const scatterData = {
    datasets: [{
      label: 'Suppliers',
      data: list.map(s => ({
        x: s.unitsSupplied,
        y: s.shelfLifePercentAtReceipt,
        name: s.supplierName,
      })),
      backgroundColor: tokens.brand,
      borderColor: tokens.surface,
      pointRadius: 5,
      pointHoverRadius: 7,
    }]
  };

  const scatterOptions = {
    ...defaultOptions,
    scales: {
      x: { ...defaultOptions.scales.x, title: { display: true, text: 'Units supplied (annual)', color: tokens.muted } },
      y: { ...defaultOptions.scales.y, title: { display: true, text: 'Shelf life % at receipt', color: tokens.muted }, max: 100 },
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-page-title text-text">Supplier performance & SLA analytics</h1>
        <p className="text-xs text-text-muted mt-0.5">Lead time actuals vs contracts, delivery shelf-life quality, and vendor comparative metrics</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Stated vs actual lead time (days)"
          subtitle="Contractual SLA versus observed receipt delays"
          chartRef={leadTimeRef}
          labels={names}
          datasets={leadTimeData.datasets}
          badge="Grouped bar"
        >
          <Bar ref={leadTimeRef} data={leadTimeData} options={defaultOptions} />
        </ChartCard>

        <ChartCard
          title="Supplier capability radar"
          subtitle="Comparative rating across delivery quality & SLA adherence"
          chartRef={radarRef}
          badge="Radar compliance"
        >
          <Radar ref={radarRef} data={radarData} options={radarOptions} />
        </ChartCard>
      </div>

      <ChartCard
        title="Units supplied vs shelf-life at delivery"
        subtitle="Evaluating batch freshness delivered by major vendors"
        chartRef={shelfLifeRef}
        badge="Scatter quality"
        height="h-72"
      >
        <Scatter ref={shelfLifeRef} data={scatterData} options={scatterOptions} />
      </ChartCard>
    </div>
  );
};

import React, { useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { ChartCard } from '../components/ui/ChartCard';
import { formatCurrency, formatNumber } from '../lib/format';
import { Scatter, Bar } from 'react-chartjs-2';
import { ShoppingCart } from 'lucide-react';

export const InventoryIntelligence = ({ onOpenMovementDrawer }) => {
  const { tokens, defaultOptions, categorical } = useChartTheme();
  const abcXyzRef = useRef(null);
  const docHistRef = useRef(null);

  // 1. Fetch ABC-XYZ Analysis
  const { data: abcData } = useQuery({
    queryKey: ['abcXyz'],
    queryFn: async () => {
      const res = await apiClient.get('/analytics/abc-xyz');
      return res.data;
    },
  });

  // 2. Fetch Reorder Suggestions
  const { data: suggestions } = useQuery({
    queryKey: ['reorderSuggestions'],
    queryFn: async () => {
      const res = await apiClient.get('/reorder/suggestions');
      return res.data;
    },
  });

  // 3. Fetch Stock Overview for Days of Cover Histogram
  const { data: stockOverview } = useQuery({
    queryKey: ['stockOverviewDoc'],
    queryFn: async () => {
      const res = await apiClient.get('/stock/overview');
      return res.data;
    },
  });

  // ABC-XYZ Scatter / Bubble Chart Data
  const items = abcData?.items || [];
  const classColors = {
    A: categorical[0],
    B: categorical[1],
    C: categorical[2],
  };

  const bubbleDatasets = ['A', 'B', 'C'].map(c => {
    const classItems = items.filter(i => i.abcClass === c);
    return {
      label: `Class ${c} (${classItems.length} drugs)`,
      data: classItems.map(i => ({
        x: i.coefficientOfVariation,
        y: Number(i.consumptionValue),
        r: Math.max(4, Math.min(20, Math.sqrt(Number(i.stockValue)) / 10)),
        name: i.drugName,
        drugId: i.drugId,
        quadrant: i.quadrant,
      })),
      backgroundColor: classColors[c] + '33', // 20% opacity solid fill
      borderColor: classColors[c],
      borderWidth: 1.5,
    };
  });

  const abcXyzOptions = {
    ...defaultOptions,
    scales: {
      x: {
        type: 'linear',
        title: { display: true, text: 'Demand variability (CV: X < 0.5, Y < 1.0, Z ≥ 1.0)', color: tokens.muted },
        grid: { color: tokens.border, drawBorder: false },
        border: { display: false },
        ticks: { color: tokens.muted },
      },
      y: {
        type: 'linear',
        title: { display: true, text: '90-day consumption value ($)', color: tokens.muted },
        grid: { color: tokens.border, drawBorder: false },
        border: { display: false },
        ticks: { color: tokens.muted },
      }
    },
    plugins: {
      ...defaultOptions.plugins,
      tooltip: {
        ...defaultOptions.plugins.tooltip,
        callbacks: {
          label: (context) => {
            const raw = context.raw;
            return [
              `Drug: ${raw.name} (${raw.drugId})`,
              `Matrix: ${raw.quadrant}`,
              `CV: ${raw.x}`,
              `Consumption: $${formatNumber(raw.y)}`,
            ];
          }
        }
      }
    }
  };

  // Days of Cover Histogram
  const docValues = (stockOverview || []).map(d => d.daysOfCover).filter(v => v !== null && v !== undefined);
  const buckets = ['< 7d', '7-14d', '15-30d', '31-60d', '> 60d'];
  const counts = [0, 0, 0, 0, 0];

  docValues.forEach(d => {
    if (d < 7) counts[0]++;
    else if (d <= 14) counts[1]++;
    else if (d <= 30) counts[2]++;
    else if (d <= 60) counts[3]++;
    else counts[4]++;
  });

  const docHistData = {
    labels: buckets,
    datasets: [{
      label: 'Number of drugs',
      data: counts,
      backgroundColor: [tokens.status.critical, tokens.status.watch, categorical[0], categorical[3], tokens.status.safe],
      borderRadius: 3,
    }]
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-page-title text-text">Inventory intelligence</h1>
        <p className="text-xs text-text-muted mt-0.5">ABC-XYZ multi-dimensional matrix, buffer coverage, and rule-driven replenishment</p>
      </div>

      {/* Row 1: ABC-XYZ Matrix & Days of Cover Histogram */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="ABC-XYZ demand & consumption matrix"
          subtitle="Bubble size represents holding inventory value ($)"
          chartRef={abcXyzRef}
          badge="Scatter / bubble"
          height="h-80"
        >
          <Scatter ref={abcXyzRef} data={{ datasets: bubbleDatasets }} options={abcXyzOptions} />
        </ChartCard>

        <ChartCard
          title="Days of cover distribution"
          subtitle="Histogram of fleet buffer runout horizons"
          chartRef={docHistRef}
          labels={buckets}
          datasets={docHistData.datasets}
          badge="Histogram"
          height="h-80"
        >
          <Bar ref={docHistRef} data={docHistData} options={defaultOptions} />
        </ChartCard>
      </div>

      {/* Row 2: Reorder Suggestion Table */}
      <div className="surface-panel p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <ShoppingCart size={16} strokeWidth={1.75} className="text-text-secondary" />
            <h3 className="text-card-title text-text">Rule-based replenishment engine suggestions</h3>
          </div>
          <span className="text-xs text-text-muted font-mono tabular-nums">
            {suggestions?.length || 0} actionable suggestions
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Drug</th>
                <th>Supplier</th>
                <th className="text-right">Stock / ROP (qty)</th>
                <th className="text-right">Lead time (days)</th>
                <th className="text-right">Days cover (days)</th>
                <th className="text-right">Suggested order (qty)</th>
                <th className="text-right">Estimated cost ($)</th>
                <th>Urgency</th>
                <th className="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {suggestions?.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-6 text-text-muted">
                    All inventory balances are currently above reorder points.
                  </td>
                </tr>
              ) : (
                suggestions?.map((item) => {
                  const isCrit = item.urgency === 'CRITICAL';
                  const isHigh = item.urgency === 'HIGH';
                  return (
                    <tr key={item.drugId}>
                      <td className="font-medium text-text">
                        {item.drugName} <span className="text-text-muted font-mono text-xs">({item.drugId})</span>
                      </td>
                      <td className="text-text-secondary">{item.primarySupplierName || '—'}</td>
                      <td className="text-right tabular-nums">
                        <span className="text-status-critical font-medium">{item.currentStock}</span> / {item.reorderPoint}
                      </td>
                      <td className="text-right text-text-muted tabular-nums">{item.leadTimeDays}d</td>
                      <td className="text-right text-text tabular-nums">{item.daysOfCover ? `${item.daysOfCover}d` : '0d'}</td>
                      <td className="text-right font-medium text-text tabular-nums">
                        {item.suppressedByNearExpiry ? (
                          <span className="text-status-watch text-xs font-normal">Suppressed (near expiry)</span>
                        ) : (
                          `${formatNumber(item.suggestedQuantity)} units`
                        )}
                      </td>
                      <td className="text-right font-medium text-text tabular-nums">{formatCurrency(item.estimatedCost)}</td>
                      <td>
                        <span className={`px-2 py-0.5 rounded-[4px] font-medium text-[11px] ${
                          isCrit ? 'bg-status-critical-tint text-status-critical border border-status-critical/30' : isHigh ? 'bg-status-watch-tint text-status-watch border border-status-watch/30' : 'bg-status-safe-tint text-status-safe border border-status-safe/30'
                        }`}>
                          {item.urgency}
                        </span>
                      </td>
                      <td className="text-right">
                        <button
                          onClick={onOpenMovementDrawer}
                          className="h-7 px-2.5 rounded-[4px] bg-brand-tint hover:bg-brand-tint/80 text-brand border border-brand/20 text-xs font-medium transition-colors duration-150"
                        >
                          Record inbound
                        </button>
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

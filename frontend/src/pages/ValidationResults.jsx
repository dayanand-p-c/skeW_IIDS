import React, { useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { ChartCard } from '../components/ui/ChartCard';
import { Bar } from 'react-chartjs-2';
import { Award, TrendingDown } from 'lucide-react';
import { formatNumber } from '../lib/format';

export const ValidationResults = () => {
  const { tokens, fixedMeanings, defaultOptions } = useChartTheme();
  const validationBarRef = useRef(null);

  const { data: validationData } = useQuery({
    queryKey: ['validationResults'],
    queryFn: async () => {
      const res = await apiClient.get('/validation/results');
      return res.data;
    },
  });

  const metrics = validationData?.metrics || [];
  const labels = metrics.map(m => m.metric);
  const baselineMeans = metrics.map(m => m.baseline.mean);
  const systemMeans = metrics.map(m => m.system.mean);

  // Custom 95% Confidence Interval Error Bars Plugin
  const errorBarsPlugin = {
    id: 'errorBarsPlugin',
    afterDatasetsDraw: (chart) => {
      const { ctx, scales: { y } } = chart;
      if (!metrics || metrics.length === 0) return;

      ctx.save();
      ctx.lineWidth = 1.5;
      ctx.strokeStyle = tokens.text;

      metrics.forEach((m, idx) => {
        // Draw baseline error bar
        const meta0 = chart.getDatasetMeta(0);
        if (meta0.data[idx]) {
          const bar0 = meta0.data[idx];
          const yLower0 = y.getPixelForValue(m.baseline.ciLower);
          const yUpper0 = y.getPixelForValue(m.baseline.ciUpper);
          const xPos0 = bar0.x;

          ctx.beginPath();
          ctx.moveTo(xPos0, yLower0);
          ctx.lineTo(xPos0, yUpper0);
          // Caps
          ctx.moveTo(xPos0 - 3, yLower0);
          ctx.lineTo(xPos0 + 3, yLower0);
          ctx.moveTo(xPos0 - 3, yUpper0);
          ctx.lineTo(xPos0 + 3, yUpper0);
          ctx.stroke();
        }

        // Draw system error bar
        const meta1 = chart.getDatasetMeta(1);
        if (meta1.data[idx]) {
          const bar1 = meta1.data[idx];
          const yLower1 = y.getPixelForValue(m.system.ciLower);
          const yUpper1 = y.getPixelForValue(m.system.ciUpper);
          const xPos1 = bar1.x;

          ctx.beginPath();
          ctx.moveTo(xPos1, yLower1);
          ctx.lineTo(xPos1, yUpper1);
          // Caps
          ctx.moveTo(xPos1 - 3, yLower1);
          ctx.lineTo(xPos1 + 3, yLower1);
          ctx.moveTo(xPos1 - 3, yUpper1);
          ctx.lineTo(xPos1 + 3, yUpper1);
          ctx.stroke();
        }
      });

      ctx.restore();
    }
  };

  const chartData = {
    labels,
    datasets: [
      {
        label: 'Baseline (fixed reorder policy)',
        data: baselineMeans,
        backgroundColor: fixedMeanings.baseline, // baseline grey
        borderRadius: 3,
      },
      {
        label: 'DIS system (rule-based & FEFO)',
        data: systemMeans,
        backgroundColor: fixedMeanings.system, // brand
        borderRadius: 3,
      }
    ]
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <div className="flex items-center gap-2">
          <Award size={18} strokeWidth={1.75} className="text-brand" />
          <h1 className="text-page-title text-text">Replay experiment validation results</h1>
        </div>
        <p className="text-xs text-text-muted mt-0.5">
          {validationData?.experiment} • 95% Confidence interval (α = 0.05)
        </p>
      </div>

      {/* Comparative Grouped Bar Chart with CI Error Bars */}
      <ChartCard
        title="Comparative performance: baseline vs DIS system"
        subtitle="Grouped bars with 95% confidence-interval error bars"
        chartRef={validationBarRef}
        labels={labels}
        datasets={chartData.datasets}
        badge="95% CI error bars"
        height="h-80"
      >
        <Bar
          ref={validationBarRef}
          data={chartData}
          options={defaultOptions}
          plugins={[errorBarsPlugin]}
        />
      </ChartCard>

      {/* Statistical Outcome Metrics Table with Reduction Badges */}
      <div className="surface-panel p-5">
        <h3 className="text-card-title text-text mb-4">Detailed experimental metrics & significance</h3>
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Evaluation metric</th>
                <th>Unit</th>
                <th className="text-right">Baseline mean [95% CI]</th>
                <th className="text-right">DIS system mean [95% CI]</th>
                <th>Relative impact</th>
                <th className="text-right">p-Value</th>
              </tr>
            </thead>
            <tbody>
              {metrics.map((m) => {
                const isImprovement = m.reductionPercent > 0;
                return (
                  <tr key={m.metric}>
                    <td className="font-medium text-text">{m.metric}</td>
                    <td className="text-text-muted text-xs">{m.unit}</td>
                    <td className="text-right tabular-nums text-text-secondary">
                      {formatNumber(m.baseline.mean)} <span className="text-text-muted text-[11px]">[{m.baseline.ciLower} - {m.baseline.ciUpper}]</span>
                    </td>
                    <td className="text-right tabular-nums font-medium text-brand">
                      {formatNumber(m.system.mean)} <span className="text-text-muted text-[11px]">[{m.system.ciLower} - {m.system.ciUpper}]</span>
                    </td>
                    <td>
                      <span className={`px-2 py-0.5 rounded-[4px] text-[11px] font-medium inline-flex items-center gap-1 ${
                        isImprovement ? 'bg-status-safe-tint text-status-safe border border-status-safe/30' : 'bg-brand-tint text-brand border border-brand/20'
                      }`}>
                        {isImprovement ? <TrendingDown size={12} strokeWidth={1.75} /> : null}
                        {Math.abs(m.reductionPercent)}% {isImprovement ? 'reduction' : 'gain'}
                      </span>
                    </td>
                    <td className="text-right text-text-muted text-xs tabular-nums">
                      p &lt; {m.pValue} (Significant)
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

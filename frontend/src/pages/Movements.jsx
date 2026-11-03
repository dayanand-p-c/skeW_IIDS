import React, { useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { ChartCard } from '../components/ui/ChartCard';
import { formatDate, formatNumber } from '../lib/format';
import { Bar } from 'react-chartjs-2';
import { ArrowLeftRight, Search, PlusCircle } from 'lucide-react';

export const Movements = ({ onOpenMovementDrawer }) => {
  const { tokens, defaultOptions, categorical } = useChartTheme();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');

  const movementsChartRef = useRef(null);

  // 1. Fetch Movements Trend Chart Data
  const { data: movementsChart } = useQuery({
    queryKey: ['movementsChart'],
    queryFn: async () => {
      const res = await apiClient.get('/analytics/movements?period=day');
      return res.data;
    },
  });

  // 2. Fetch Searchable Transactions
  const { data: transactionsPage } = useQuery({
    queryKey: ['transactionsList', searchTerm, filterType],
    queryFn: async () => {
      let url = '/transactions?size=50';
      if (filterType) url += `&type=${filterType}`;
      if (searchTerm) url += `&q=${searchTerm}`;
      const res = await apiClient.get(url);
      return res.data;
    },
  });

  // Combo Chart: Inbound, Outbound, Write-Off
  const chartLabels = movementsChart?.labels || [];
  const movementsChartData = {
    labels: chartLabels,
    datasets: [
      {
        label: 'Inbound stock (+)',
        data: movementsChart?.inbound || [],
        backgroundColor: tokens.brand,
        borderRadius: 3,
      },
      {
        label: 'Outbound dispenses (-)',
        data: movementsChart?.outbound || [],
        backgroundColor: categorical[2],
        borderRadius: 3,
      },
      {
        label: 'Expired write-offs (-)',
        data: movementsChart?.writeOff || [],
        backgroundColor: tokens.status.critical,
        borderRadius: 3,
      }
    ]
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-page-title text-text">Stock movements & transactions</h1>
          <p className="text-xs text-text-muted mt-0.5">Complete immutable audit trail of all inbound receipts, FEFO dispenses, write-offs, and adjustments</p>
        </div>

        <button
          onClick={onOpenMovementDrawer}
          className="btn-primary"
        >
          <PlusCircle size={16} strokeWidth={1.75} />
          Record new movement
        </button>
      </div>

      {/* Movements Trend Chart */}
      <ChartCard
        title="Stock flow dynamics (last 30 days)"
        subtitle="Daily volume comparison across inbound receipts, customer dispenses, and expired write-offs"
        chartRef={movementsChartRef}
        labels={chartLabels}
        datasets={movementsChartData.datasets}
        badge="Grouped combo"
        height="h-72"
      >
        <Bar ref={movementsChartRef} data={movementsChartData} options={defaultOptions} />
      </ChartCard>

      {/* Transaction History Table */}
      <div className="surface-panel p-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
          <div className="flex items-center gap-2">
            <ArrowLeftRight size={16} strokeWidth={1.75} className="text-text-secondary" />
            <h3 className="text-card-title text-text">Immutable transaction ledger</h3>
          </div>

          <div className="flex items-center gap-2 text-xs">
            <div className="relative">
              <Search size={14} strokeWidth={1.75} className="absolute left-2.5 top-2.5 text-text-muted" />
              <input
                type="text"
                placeholder="Search drug, batch, user..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="h-9 bg-surface border border-border rounded-[6px] pl-8 pr-3 text-xs text-text placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand w-48 sm:w-60"
              />
            </div>

            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              className="h-9 bg-surface border border-border rounded-[6px] px-3 text-xs text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            >
              <option value="">All movement types</option>
              <option value="INBOUND">Inbound</option>
              <option value="OUTBOUND">Outbound</option>
              <option value="EXPIRED_WRITE_OFF">Expired write-off</option>
              <option value="RETURN">Return</option>
              <option value="ADJUSTMENT">Adjustment</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Tx ID</th>
                <th>Type</th>
                <th>Drug</th>
                <th>Batch ID</th>
                <th className="text-right">Quantity (units)</th>
                <th>Location</th>
                <th>Created by</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {transactionsPage?.content?.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-6 text-text-muted">
                    No transactions found.
                  </td>
                </tr>
              ) : (
                transactionsPage?.content?.map((tx) => {
                  const isInbound = tx.type === 'INBOUND' || tx.type === 'RETURN';
                  const isOutbound = tx.type === 'OUTBOUND';
                  const isWriteOff = tx.type === 'EXPIRED_WRITE_OFF';
                  return (
                    <tr key={tx.transactionId}>
                      <td className="font-mono text-text-muted text-xs">#{tx.transactionId}</td>
                      <td>
                        <span className={`px-2 py-0.5 rounded-[4px] text-[11px] font-medium ${
                          isInbound ? 'bg-brand-tint text-brand border border-brand/20'
                          : isOutbound ? 'bg-surface-2 text-text border border-border'
                          : isWriteOff ? 'bg-status-critical-tint text-status-critical border border-status-critical/30'
                          : 'bg-status-watch-tint text-status-watch border border-status-watch/30'
                        }`}>
                          {tx.type}
                        </span>
                      </td>
                      <td className="font-medium text-text">{tx.drugName}</td>
                      <td className="font-mono text-text-secondary">{tx.batchId}</td>
                      <td className="text-right font-medium text-text tabular-nums">
                        {isInbound ? `+${formatNumber(tx.quantity)}` : `-${formatNumber(tx.quantity)}`}
                      </td>
                      <td className="text-text-muted">{tx.location}</td>
                      <td className="text-text-secondary">{tx.createdBy}</td>
                      <td className="text-text-muted text-[11px] tabular-nums">{formatDate(tx.timestamp)}</td>
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

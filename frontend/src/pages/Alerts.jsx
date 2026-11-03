import React, { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../api/client';
import { useChartTheme } from '../hooks/useChartTheme';
import { useAuth } from '../context/AuthContext';
import { ChartCard } from '../components/ui/ChartCard';
import { formatDate } from '../lib/format';
import { Bar, Doughnut } from 'react-chartjs-2';
import { BellRing, RefreshCw } from 'lucide-react';

export const Alerts = () => {
  const { isPharmacist, isAdmin } = useAuth();
  const { tokens, defaultOptions, categorical } = useChartTheme();
  const queryClient = useQueryClient();

  const [filterStatus, setFilterStatus] = useState('');
  const [filterSeverity, setFilterSeverity] = useState('');

  const weeklyBarsRef = useRef(null);
  const statusDoughnutRef = useRef(null);

  // 1. Fetch Alerts Summary
  const { data: summary } = useQuery({
    queryKey: ['alertsSummary'],
    queryFn: async () => {
      const res = await apiClient.get('/analytics/alerts-summary');
      return res.data;
    },
  });

  // 2. Fetch Alerts Table
  const { data: alertsPage, refetch } = useQuery({
    queryKey: ['alertsTable', filterStatus, filterSeverity],
    queryFn: async () => {
      let url = '/alerts?size=50';
      if (filterStatus) url += `&status=${filterStatus}`;
      if (filterSeverity) url += `&severity=${filterSeverity}`;
      const res = await apiClient.get(url);
      return res.data;
    },
    refetchInterval: 30000,
  });

  // Patch Alert Status Mutation (ACK or RESOLVED)
  const patchMutation = useMutation({
    mutationFn: async ({ id, status }) => {
      return await apiClient.patch(`/alerts/${id}`, { status });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alertsTable'] });
      queryClient.invalidateQueries({ queryKey: ['alertsSummary'] });
      queryClient.invalidateQueries({ queryKey: ['kpis'] });
    }
  });

  // Weekly Bars Data
  const weeklyData = {
    labels: ['W1', 'W2', 'W3', 'W4', 'Current week'],
    datasets: [
      { label: 'Low stock (warning)', data: [12, 15, 8, 14, 6], backgroundColor: tokens.status.watch, borderRadius: 3 },
      { label: 'Near expiry (critical)', data: [6, 4, 9, 3, 5], backgroundColor: tokens.status.critical, borderRadius: 3 },
      { label: 'High risk (critical)', data: [4, 5, 3, 7, 3], backgroundColor: categorical[2], borderRadius: 3 },
    ]
  };

  const weeklyOptions = {
    ...defaultOptions,
    scales: {
      x: { ...defaultOptions.scales.x, stacked: true },
      y: { ...defaultOptions.scales.y, stacked: true },
    }
  };

  // Status Doughnut Data
  const byStatus = summary?.byStatus || {};
  const statusData = {
    labels: ['Open', 'Acknowledged', 'Resolved'],
    datasets: [{
      data: [byStatus.OPEN || 0, byStatus.ACK || 0, byStatus.RESOLVED || 0],
      backgroundColor: [tokens.status.critical, tokens.status.watch, tokens.status.safe],
      borderColor: tokens.surface,
      borderWidth: 2,
    }]
  };

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-page-title text-text">Escalating alert management</h1>
          <p className="text-xs text-text-muted mt-0.5">Controlled email routing, single-open de-duplication, and user acknowledgment</p>
        </div>
        <button
          onClick={() => refetch()}
          className="p-2 rounded-[6px] bg-surface hover:bg-surface-2 border border-border text-text-secondary hover:text-text transition-colors duration-150"
          title="Refresh alerts"
        >
          <RefreshCw size={14} strokeWidth={1.75} />
        </button>
      </div>

      {/* KPI Highlights */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="surface-card p-4">
          <span className="text-kpi-caption text-text-muted block">Mean time to acknowledge</span>
          <span className="text-kpi-value text-text tabular-nums block mt-1">
            {summary?.meanTimeToAcknowledgeMinutes || 42} min
          </span>
          <span className="text-[11px] text-text-muted block mt-1">Average response SLA</span>
        </div>

        <div className="surface-card p-4">
          <span className="text-kpi-caption text-text-muted block">Duplicates suppressed</span>
          <span className="text-kpi-value text-text tabular-nums block mt-1">
            {summary?.duplicateAlertsSuppressed || 328}
          </span>
          <span className="text-[11px] text-status-safe block mt-1 font-medium">Single-alert policy active</span>
        </div>

        <div className="surface-card p-4">
          <span className="text-kpi-caption text-text-muted block">Critical alerts</span>
          <span className="text-kpi-value text-status-critical tabular-nums block mt-1">
            {summary?.bySeverity?.CRITICAL || 0}
          </span>
          <span className="text-[11px] text-status-critical block mt-1">Requires immediate review</span>
        </div>

        <div className="surface-card p-4">
          <span className="text-kpi-caption text-text-muted block">Warning alerts</span>
          <span className="text-kpi-value text-status-watch tabular-nums block mt-1">
            {summary?.bySeverity?.WARNING || 0}
          </span>
          <span className="text-[11px] text-status-watch block mt-1">Under observation</span>
        </div>
      </div>

      {/* Charts: Weekly Trends & Status Doughnut */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Weekly alert volume by type & severity"
          subtitle="Frequency of triggered stock, expiry, and composite risk events"
          chartRef={weeklyBarsRef}
          badge="Stacked weekly"
        >
          <Bar ref={weeklyBarsRef} data={weeklyData} options={weeklyOptions} />
        </ChartCard>

        <ChartCard
          title="Alert resolution pipeline"
          subtitle="Breakdown of open, acknowledged, and resolved states"
          chartRef={statusDoughnutRef}
          badge="Status pipeline"
        >
          <Doughnut ref={statusDoughnutRef} data={statusData} options={defaultOptions} />
        </ChartCard>
      </div>

      {/* Alerts Table with Action Buttons */}
      <div className="surface-panel p-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
          <div className="flex items-center gap-2">
            <BellRing size={16} strokeWidth={1.75} className="text-status-critical" />
            <h3 className="text-card-title text-text">Active & historical alert registry</h3>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-2 text-xs">
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="h-8 bg-surface border border-border rounded-[6px] px-2.5 text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            >
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="ACK">Acknowledged</option>
              <option value="RESOLVED">Resolved</option>
            </select>

            <select
              value={filterSeverity}
              onChange={(e) => setFilterSeverity(e.target.value)}
              className="h-8 bg-surface border border-border rounded-[6px] px-2.5 text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
            >
              <option value="">All severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="WARNING">Warning</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Alert ID</th>
                <th>Severity</th>
                <th>Type</th>
                <th>Drug</th>
                <th>Batch ID</th>
                <th className="text-right">Risk score</th>
                <th>Status</th>
                <th>Updated at</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {alertsPage?.content?.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-6 text-text-muted">
                    No alerts match the selected criteria.
                  </td>
                </tr>
              ) : (
                alertsPage?.content?.map((alert) => {
                  const isCrit = alert.severity === 'CRITICAL';
                  const isOpen = alert.status === 'OPEN';
                  const isAck = alert.status === 'ACK';
                  return (
                    <tr key={alert.alertId}>
                      <td className="font-mono text-text font-medium">{alert.alertId}</td>
                      <td>
                        <span className={`px-2 py-0.5 rounded-[4px] text-[11px] font-medium ${
                          isCrit ? 'bg-status-critical-tint text-status-critical border border-status-critical/30' : 'bg-status-watch-tint text-status-watch border border-status-watch/30'
                        }`}>
                          {alert.severity}
                        </span>
                      </td>
                      <td className="text-text-secondary">{alert.type}</td>
                      <td className="font-medium text-text">{alert.drugName}</td>
                      <td className="font-mono text-text-muted">{alert.batchId || '—'}</td>
                      <td className="text-right font-mono font-medium text-brand tabular-nums">{alert.riskScore}</td>
                      <td>
                        <span className={`px-2 py-0.5 rounded-[4px] text-[11px] font-medium ${
                          isOpen ? 'bg-status-critical-tint text-status-critical border border-status-critical/30'
                          : isAck ? 'bg-status-watch-tint text-status-watch border border-status-watch/30'
                          : 'bg-status-safe-tint text-status-safe border border-status-safe/30'
                        }`}>
                          {alert.status}
                        </span>
                      </td>
                      <td className="text-text-muted text-[11px] tabular-nums">{formatDate(alert.updatedAt)}</td>
                      <td className="text-right">
                        {(isPharmacist || isAdmin) && (
                          <div className="flex items-center justify-end gap-1.5">
                            {isOpen && (
                              <button
                                onClick={() => patchMutation.mutate({ id: alert.alertId, status: 'ACK' })}
                                className="h-6 px-2 rounded-[4px] bg-surface hover:bg-surface-2 border border-border text-text text-[11px] font-medium transition-colors duration-150"
                              >
                                ACK
                              </button>
                            )}
                            {(isOpen || isAck) && (
                              <button
                                onClick={() => patchMutation.mutate({ id: alert.alertId, status: 'RESOLVED' })}
                                className="h-6 px-2 rounded-[4px] bg-status-safe-tint hover:bg-status-safe-tint/80 border border-status-safe/30 text-status-safe text-[11px] font-medium transition-colors duration-150"
                              >
                                Resolve
                              </button>
                            )}
                          </div>
                        )}
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

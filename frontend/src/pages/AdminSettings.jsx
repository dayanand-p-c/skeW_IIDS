import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../api/client';
import { Sliders, Check } from 'lucide-react';

export const AdminSettings = () => {
  const queryClient = useQueryClient();

  // Config State
  const [wS, setWS] = useState(0.4);
  const [wE, setWE] = useState(0.4);
  const [wV, setWV] = useState(0.2);
  const [warnDays, setWarnDays] = useState(90);
  const [criticalDays, setCriticalDays] = useState(30);
  const [watchBand, setWatchBand] = useState(30);
  const [criticalBand, setCriticalBand] = useState(60);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // 1. Fetch Config
  const { data: config } = useQuery({
    queryKey: ['systemConfig'],
    queryFn: async () => {
      const res = await apiClient.get('/config');
      return res.data;
    },
  });

  useEffect(() => {
    if (config) {
      if (config['weight.stock']) setWS(parseFloat(config['weight.stock']));
      if (config['weight.expiry']) setWE(parseFloat(config['weight.expiry']));
      if (config['weight.variability']) setWV(parseFloat(config['weight.variability']));
      if (config['expiry.warn.days']) setWarnDays(parseInt(config['expiry.warn.days']));
      if (config['expiry.critical.days']) setCriticalDays(parseInt(config['expiry.critical.days']));
      if (config['band.watch']) setWatchBand(parseInt(config['band.watch']));
      if (config['band.critical']) setCriticalBand(parseInt(config['band.critical']));
    }
  }, [config]);

  // 2. Fetch Top 10 Drugs for Live Ranking Preview
  const { data: topDrugsData } = useQuery({
    queryKey: ['topDrugsPreview', wS, wE, wV],
    queryFn: async () => {
      const res = await apiClient.get('/analytics/risk-distribution');
      return res.data?.topRiskDrugs || [];
    },
  });

  // Slider normalization ensuring sum = 1.0
  const handleWeightChange = (type, newVal) => {
    const val = parseFloat(newVal);
    if (type === 'STOCK') {
      const rem = Math.max(0, 1.0 - val);
      const ratio = (wE + wV) > 0 ? rem / (wE + wV) : 0.5;
      setWS(val);
      setWE(Math.round(wE * ratio * 100) / 100);
      setWV(Math.round((rem - (Math.round(wE * ratio * 100) / 100)) * 100) / 100);
    } else if (type === 'EXPIRY') {
      const rem = Math.max(0, 1.0 - val);
      const ratio = (wS + wV) > 0 ? rem / (wS + wV) : 0.5;
      setWE(val);
      setWS(Math.round(wS * ratio * 100) / 100);
      setWV(Math.round((rem - (Math.round(wS * ratio * 100) / 100)) * 100) / 100);
    } else if (type === 'VAR') {
      const rem = Math.max(0, 1.0 - val);
      const ratio = (wS + wE) > 0 ? rem / (wS + wE) : 0.5;
      setWV(val);
      setWS(Math.round(wS * ratio * 100) / 100);
      setWE(Math.round((rem - (Math.round(wS * ratio * 100) / 100)) * 100) / 100);
    }
  };

  const handleSaveConfig = async (e) => {
    e.preventDefault();
    try {
      await apiClient.put('/config', {
        'weight.stock': String(wS),
        'weight.expiry': String(wE),
        'weight.variability': String(wV),
        'expiry.warn.days': String(warnDays),
        'expiry.critical.days': String(criticalDays),
        'band.watch': String(watchBand),
        'band.critical': String(criticalBand),
      });
      setSaveSuccess(true);
      queryClient.invalidateQueries();
      setTimeout(() => setSaveSuccess(false), 2000);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h1 className="text-page-title text-text">System configuration & administration</h1>
        <p className="text-xs text-text-muted mt-0.5">Score weight tuning, risk bands, threshold parameters, and alert routing</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left: Weight Sliders & Thresholds */}
        <div className="surface-panel p-5">
          <div className="flex items-center gap-2 mb-4">
            <Sliders size={16} strokeWidth={1.75} className="text-brand" />
            <h3 className="text-card-title text-text">Score weight calibration (wS + wE + wV = 1.0)</h3>
          </div>

          <form onSubmit={handleSaveConfig} className="space-y-5 text-xs">
            {/* Stock Weight Slider */}
            <div>
              <div className="flex justify-between mb-1.5">
                <span className="text-text-secondary font-medium">Stock risk weight (wS)</span>
                <span className="font-mono text-brand font-semibold tabular-nums">{wS.toFixed(2)}</span>
              </div>
              <input
                type="range"
                min="0.0"
                max="1.0"
                step="0.05"
                value={wS}
                onChange={(e) => handleWeightChange('STOCK', e.target.value)}
                className="w-full accent-brand"
              />
            </div>

            {/* Expiry Weight Slider */}
            <div>
              <div className="flex justify-between mb-1.5">
                <span className="text-text-secondary font-medium">Expiry risk weight (wE)</span>
                <span className="font-mono text-status-critical font-semibold tabular-nums">{wE.toFixed(2)}</span>
              </div>
              <input
                type="range"
                min="0.0"
                max="1.0"
                step="0.05"
                value={wE}
                onChange={(e) => handleWeightChange('EXPIRY', e.target.value)}
                className="w-full accent-brand"
              />
            </div>

            {/* Variability Weight Slider */}
            <div>
              <div className="flex justify-between mb-1.5">
                <span className="text-text-secondary font-medium">Demand variability weight (wV)</span>
                <span className="font-mono text-text font-semibold tabular-nums">{wV.toFixed(2)}</span>
              </div>
              <input
                type="range"
                min="0.0"
                max="1.0"
                step="0.05"
                value={wV}
                onChange={(e) => handleWeightChange('VAR', e.target.value)}
                className="w-full accent-brand"
              />
            </div>

            <div className="pt-4 border-t border-border grid grid-cols-2 gap-4">
              <div>
                <label className="text-field-label text-text-muted mb-1 block">Expiry warning days</label>
                <input
                  type="number"
                  value={warnDays}
                  onChange={(e) => setWarnDays(Number(e.target.value))}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                />
              </div>

              <div>
                <label className="text-field-label text-text-muted mb-1 block">Expiry critical days</label>
                <input
                  type="number"
                  value={criticalDays}
                  onChange={(e) => setCriticalDays(Number(e.target.value))}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                />
              </div>

              <div>
                <label className="text-field-label text-text-muted mb-1 block">Watch band threshold</label>
                <input
                  type="number"
                  value={watchBand}
                  onChange={(e) => setWatchBand(Number(e.target.value))}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                />
              </div>

              <div>
                <label className="text-field-label text-text-muted mb-1 block">Critical band threshold</label>
                <input
                  type="number"
                  value={criticalBand}
                  onChange={(e) => setCriticalBand(Number(e.target.value))}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                />
              </div>
            </div>

            <button
              type="submit"
              className="btn-primary w-full h-9 mt-6"
            >
              {saveSuccess ? <Check size={16} strokeWidth={1.75} /> : null}
              {saveSuccess ? 'Saved & recalculated' : 'Save configuration & recalculate risk'}
            </button>
          </form>
        </div>

        {/* Right: Live Top 10 Ranking Preview */}
        <div className="surface-panel p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-card-title text-text">Live top 10 risk ranking preview</h3>
            <span className="text-[11px] font-mono px-2 py-0.5 rounded-[4px] bg-brand-tint text-brand border border-brand/20">
              Dynamic impact
            </span>
          </div>

          <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
            {topDrugsData?.map((drug, idx) => (
              <div key={drug.drugId} className="p-2.5 rounded-[6px] bg-surface-2 border border-border text-xs flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-text-muted w-5 tabular-nums">#{idx + 1}</span>
                  <div>
                    <span className="font-semibold text-text block">{drug.drugName}</span>
                    <span className="text-[11px] text-text-muted">{drug.category} • {drug.drugId}</span>
                  </div>
                </div>

                <div className="text-right">
                  <span className={`px-2 py-0.5 rounded-[4px] text-[11px] font-medium tabular-nums ${
                    drug.status === 'CRITICAL' ? 'bg-status-critical-tint text-status-critical border border-status-critical/30' : drug.status === 'WATCH' ? 'bg-status-watch-tint text-status-watch border border-status-watch/30' : 'bg-status-safe-tint text-status-safe border border-status-safe/30'
                  }`}>
                    {drug.riskIndex} ({drug.status})
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

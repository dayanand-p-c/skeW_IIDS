import React from 'react';
import { Download, FileSpreadsheet } from 'lucide-react';
import { downloadChartPng, exportChartCsv } from '../../hooks/useChartTheme';

export const ChartCard = ({
  title,
  subtitle,
  chartRef,
  labels,
  datasets,
  actions,
  badge,
  height = 'h-72',
  children,
  className = '',
}) => {
  const handleDownloadPng = () => {
    downloadChartPng(chartRef, title);
  };

  const handleExportCsv = () => {
    exportChartCsv(labels, datasets, title);
  };

  return (
    <div className={`surface-panel p-5 flex flex-col justify-between ${className}`}>
      {/* Header */}
      <div className="flex items-start justify-between mb-4 gap-2">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-card-title text-text">{title}</h3>
            {badge && (
              <span className="text-[11px] font-medium px-2 py-0.5 rounded-[4px] bg-brand-tint text-brand border border-brand/20">
                {badge}
              </span>
            )}
          </div>
          {subtitle && (
            <p className="text-xs text-text-muted mt-0.5">{subtitle}</p>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-1.5 shrink-0">
          {actions}
          {chartRef && (
            <button
              onClick={handleDownloadPng}
              title="Download PNG"
              className="p-1.5 rounded-[6px] bg-surface hover:bg-surface-2 text-text-secondary hover:text-text border border-border transition-colors duration-150"
            >
              <Download size={14} strokeWidth={1.75} />
            </button>
          )}
          {labels && datasets && (
            <button
              onClick={handleExportCsv}
              title="Export CSV"
              className="p-1.5 rounded-[6px] bg-surface hover:bg-surface-2 text-text-secondary hover:text-text border border-border transition-colors duration-150"
            >
              <FileSpreadsheet size={14} strokeWidth={1.75} />
            </button>
          )}
        </div>
      </div>

      {/* Chart Canvas Area */}
      <div className={`w-full ${height} relative`}>
        {children}
      </div>
    </div>
  );
};

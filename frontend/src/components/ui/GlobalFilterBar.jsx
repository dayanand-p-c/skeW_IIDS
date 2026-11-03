import React from 'react';
import { useFilters } from '../../context/FilterContext';
import { Filter, X, Search } from 'lucide-react';
import apiClient from '../../api/client';

export const GlobalFilterBar = () => {
  const {
    category,
    setCategory,
    selectedDrugId,
    setSelectedDrugId,
    selectedStatus,
    setSelectedStatus,
    resetFilters
  } = useFilters();

  const [categories, setCategories] = React.useState([]);

  React.useEffect(() => {
    apiClient.get('/drugs/categories')
      .then(res => setCategories(res.data))
      .catch(() => {});
  }, []);

  const hasActiveFilters = category || selectedDrugId || selectedStatus;

  return (
    <div className="surface-panel px-4 py-3 mb-6 flex flex-wrap items-center justify-between gap-4">
      <div className="flex flex-wrap items-center gap-3 text-xs">
        <div className="flex items-center gap-1.5 text-text-muted font-medium">
          <Filter size={16} strokeWidth={1.75} className="text-text-secondary" />
          <span>Cross filters:</span>
        </div>

        {/* Category Filter */}
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="h-9 bg-surface border border-border rounded-[6px] px-3 text-xs text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
        >
          <option value="">All categories (8)</option>
          {categories.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>

        {/* Status Filter */}
        <div className="flex items-center gap-1 bg-surface-2 border border-border rounded-[6px] p-0.5">
          <button
            onClick={() => setSelectedStatus('')}
            className={`h-7 px-2.5 rounded-[4px] text-xs transition-colors duration-150 ${
              !selectedStatus ? 'bg-surface text-text font-medium border border-border shadow-sm' : 'text-text-muted hover:text-text'
            }`}
          >
            All status
          </button>
          <button
            onClick={() => setSelectedStatus(selectedStatus === 'SAFE' ? '' : 'SAFE')}
            className={`h-7 px-2.5 rounded-[4px] text-xs transition-colors duration-150 ${
              selectedStatus === 'SAFE' ? 'bg-status-safe-tint text-status-safe border border-status-safe/30 font-medium' : 'text-text-muted hover:text-status-safe'
            }`}
          >
            Safe
          </button>
          <button
            onClick={() => setSelectedStatus(selectedStatus === 'WATCH' ? '' : 'WATCH')}
            className={`h-7 px-2.5 rounded-[4px] text-xs transition-colors duration-150 ${
              selectedStatus === 'WATCH' ? 'bg-status-watch-tint text-status-watch border border-status-watch/30 font-medium' : 'text-text-muted hover:text-status-watch'
            }`}
          >
            Watch
          </button>
          <button
            onClick={() => setSelectedStatus(selectedStatus === 'CRITICAL' ? '' : 'CRITICAL')}
            className={`h-7 px-2.5 rounded-[4px] text-xs transition-colors duration-150 ${
              selectedStatus === 'CRITICAL' ? 'bg-status-critical-tint text-status-critical border border-status-critical/30 font-medium' : 'text-text-muted hover:text-status-critical'
            }`}
          >
            Critical
          </button>
        </div>

        {/* Drug ID Search Input */}
        <div className="relative">
          <Search size={14} strokeWidth={1.75} className="absolute left-2.5 top-2.5 text-text-muted" />
          <input
            type="text"
            placeholder="Search drug code / name..."
            value={selectedDrugId}
            onChange={(e) => setSelectedDrugId(e.target.value)}
            className="h-9 bg-surface border border-border rounded-[6px] pl-8 pr-3 text-xs text-text placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand w-48 sm:w-60"
          />
        </div>
      </div>

      {/* Clear Filters Button */}
      {hasActiveFilters && (
        <button
          onClick={resetFilters}
          className="flex items-center gap-1.5 h-8 px-2.5 rounded-[6px] bg-status-critical-tint hover:bg-status-critical-tint/80 text-status-critical border border-status-critical/30 text-xs transition-colors duration-150 font-medium"
        >
          <X size={14} strokeWidth={1.75} /> Reset filters
        </button>
      )}
    </div>
  );
};

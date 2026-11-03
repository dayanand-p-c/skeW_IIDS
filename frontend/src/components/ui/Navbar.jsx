import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useFilters } from '../../context/FilterContext';
import { Sun, Moon, LogOut, Clock as ClockIcon, FastForward, RotateCcw, Activity } from 'lucide-react';
import apiClient from '../../api/client';

export const Navbar = ({ onOpenMovementDrawer }) => {
  const { user, logout, isAdmin } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const { category, setCategory, resetFilters } = useFilters();
  const [simDate, setSimDate] = React.useState('Loading...');
  const [advancing, setAdvancing] = React.useState(false);

  const fetchSimDate = () => {
    apiClient.get('/sim/current')
      .then(res => setSimDate(res.data.currentDate))
      .catch(() => setSimDate('2026-10-25'));
  };

  React.useEffect(() => {
    fetchSimDate();
  }, []);

  const handleAdvanceDays = async (days) => {
    setAdvancing(true);
    try {
      const res = await apiClient.post(`/sim/advance?days=${days}`);
      setSimDate(res.data.currentDate);
      window.location.reload(); // Refresh charts to reflect advanced date
    } catch (e) {
      console.error(e);
    } finally {
      setAdvancing(false);
    }
  };

  const handleResetClock = async () => {
    try {
      const res = await apiClient.post('/sim/reset');
      setSimDate(res.data.currentDate);
      window.location.reload();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <header className="h-16 border-b border-border bg-surface sticky top-0 z-40 px-6 flex items-center justify-between">
      {/* Brand */}
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-[6px] bg-brand text-white flex items-center justify-center font-bold text-sm">
          <Activity size={18} strokeWidth={1.75} />
        </div>
        <div>
          <span className="font-semibold text-sm tracking-tight text-text flex items-center gap-2">
            DIS <span className="text-[11px] font-mono px-1.5 py-0.5 rounded-[4px] bg-brand-tint text-brand border border-brand/20 font-medium">2026MIN549</span>
          </span>
          <span className="text-[11px] text-text-muted block">Drug inventory & supply chain tracking</span>
        </div>
      </div>

      {/* Simulated Clock Bar */}
      <div className="hidden lg:flex items-center gap-2 px-3 py-1.5 rounded-[6px] bg-surface-2 border border-border text-xs text-text-secondary">
        <ClockIcon size={16} strokeWidth={1.75} className="text-text-secondary" />
        <span className="text-text-muted">Sim clock:</span>
        <span className="font-mono font-medium text-text tabular-nums">{simDate}</span>
        {isAdmin && (
          <div className="flex items-center gap-1 ml-2 border-l border-border pl-2">
            <button
              onClick={() => handleAdvanceDays(7)}
              disabled={advancing}
              title="Advance Clock +7 Days"
              className="px-2 py-0.5 rounded-[4px] bg-surface hover:bg-surface-2 text-text-secondary hover:text-text border border-border flex items-center gap-1 transition-colors duration-150"
            >
              <FastForward size={12} strokeWidth={1.75} /> +7d
            </button>
            <button
              onClick={() => handleAdvanceDays(30)}
              disabled={advancing}
              title="Advance Clock +30 Days"
              className="px-2 py-0.5 rounded-[4px] bg-surface hover:bg-surface-2 text-text-secondary hover:text-text border border-border flex items-center gap-1 transition-colors duration-150"
            >
              <FastForward size={12} strokeWidth={1.75} /> +30d
            </button>
            <button
              onClick={handleResetClock}
              title="Reset Simulated Clock"
              className="p-1 rounded-[4px] bg-surface hover:bg-surface-2 text-text-muted hover:text-text border border-border transition-colors duration-150"
            >
              <RotateCcw size={12} strokeWidth={1.75} />
            </button>
          </div>
        )}
      </div>

      {/* Right Actions */}
      <div className="flex items-center gap-3">
        {onOpenMovementDrawer && (
          <button
            onClick={onOpenMovementDrawer}
            className="btn-primary"
          >
            Record movement
          </button>
        )}

        {/* Theme Toggle */}
        <button
          onClick={toggleTheme}
          className="p-2 rounded-[6px] bg-surface hover:bg-surface-2 border border-border text-text-secondary hover:text-text transition-colors duration-150"
          title="Toggle dark / light theme"
        >
          {isDark ? <Sun size={16} strokeWidth={1.75} /> : <Moon size={16} strokeWidth={1.75} />}
        </button>

        {/* User Info */}
        <div className="flex items-center gap-2 pl-3 border-l border-border">
          <div className="w-8 h-8 rounded-[6px] bg-surface-2 border border-border flex items-center justify-center font-semibold text-xs text-brand">
            {user?.username ? user.username.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="hidden md:block text-left">
            <span className="block text-xs font-semibold text-text leading-tight">{user?.username}</span>
            <span className="text-[11px] text-text-muted font-mono">{user?.role}</span>
          </div>
          <button
            onClick={logout}
            title="Sign out"
            className="p-1.5 rounded-[6px] hover:bg-status-critical-tint text-text-muted hover:text-status-critical transition-colors duration-150 ml-1"
          >
            <LogOut size={16} strokeWidth={1.75} />
          </button>
        </div>
      </div>
    </header>
  );
};

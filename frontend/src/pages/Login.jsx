import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { Activity, ArrowRight, AlertCircle } from 'lucide-react';

export const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await login(username, password);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFill = (u, p) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6 bg-bg font-sans">
      <div className="w-full max-w-md surface-panel p-8">
        <div className="text-center mb-8">
          <div className="w-10 h-10 rounded-[6px] bg-brand text-white flex items-center justify-center mx-auto mb-3 font-semibold text-sm">
            <Activity size={20} strokeWidth={1.75} />
          </div>
          <h1 className="text-page-title text-text tracking-tight">Drug inventory system</h1>
          <p className="text-xs text-text-muted mt-1">Project 2026MIN549 • Analytics & supply chain</p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-[6px] bg-status-critical-tint border border-status-critical/30 text-status-critical text-xs flex items-center gap-2">
            <AlertCircle size={16} strokeWidth={1.75} className="shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-field-label text-text-muted mb-1.5 block">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-xs text-text placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand transition-colors duration-150"
              required
            />
          </div>

          <div>
            <label className="text-field-label text-text-muted mb-1.5 block">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-xs text-text placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand transition-colors duration-150"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full h-9 mt-6 disabled:opacity-50"
          >
            {loading ? 'Authenticating...' : 'Sign in to dashboard'}
            <ArrowRight size={14} strokeWidth={1.75} />
          </button>
        </form>

        {/* Quick Demo Logins */}
        <div className="mt-8 pt-6 border-t border-border">
          <span className="block text-xs font-medium text-text-muted mb-2 text-center">
            Demo credentials (1-click fill)
          </span>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => handleQuickFill('admin', 'admin123')}
              className="p-2 rounded-[6px] bg-surface hover:bg-surface-2 border border-border text-center transition-colors duration-150"
            >
              <span className="block text-xs font-semibold text-text">Admin</span>
              <span className="text-[11px] text-text-muted">Full access</span>
            </button>
            <button
              type="button"
              onClick={() => handleQuickFill('pharmacist', 'pharma123')}
              className="p-2 rounded-[6px] bg-surface hover:bg-surface-2 border border-border text-center transition-colors duration-150"
            >
              <span className="block text-xs font-semibold text-text">Pharmacist</span>
              <span className="text-[11px] text-text-muted">FEFO outbound</span>
            </button>
            <button
              type="button"
              onClick={() => handleQuickFill('warehouse', 'warehouse123')}
              className="p-2 rounded-[6px] bg-surface hover:bg-surface-2 border border-border text-center transition-colors duration-150"
            >
              <span className="block text-xs font-semibold text-text">Warehouse</span>
              <span className="text-[11px] text-text-muted">Inbound stock</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

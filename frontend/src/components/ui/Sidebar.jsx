import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  TrendingUp,
  AlertTriangle,
  Boxes,
  Truck,
  BellRing,
  ArrowLeftRight,
  CheckCircle2,
  Settings,
} from 'lucide-react';

export const Sidebar = () => {
  const { isAdmin } = useAuth();

  const navItems = [
    { to: '/', label: 'Command center', icon: LayoutDashboard },
    { to: '/stock', label: 'Stock analytics', icon: TrendingUp },
    { to: '/expiry', label: 'Expiry & wastage', icon: AlertTriangle },
    { to: '/intelligence', label: 'Inventory ABC-XYZ', icon: Boxes },
    { to: '/suppliers', label: 'Suppliers', icon: Truck },
    { to: '/alerts', label: 'Alerts', icon: BellRing },
    { to: '/movements', label: 'Movements', icon: ArrowLeftRight },
    { to: '/validation', label: 'Validation results', icon: CheckCircle2 },
  ];

  if (isAdmin) {
    navItems.push({ to: '/admin', label: 'Admin settings', icon: Settings });
  }

  return (
    <aside className="w-60 shrink-0 border-r border-sidebar-border bg-sidebar-bg text-sidebar-text min-h-[calc(100vh-4rem)] p-3 flex flex-col justify-between font-sans">
      <div className="space-y-1">
        <div className="px-3 py-2 text-xs font-medium text-sidebar-text/60">
          Navigation
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-r-[6px] text-xs font-medium transition-colors duration-150 border-l-[3px] ${
                  isActive
                    ? 'border-brand text-white bg-sidebar-hover'
                    : 'border-transparent text-sidebar-text hover:text-white hover:bg-sidebar-hover/60'
                }`
              }
            >
              <Icon size={16} strokeWidth={1.75} className="shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </div>

      {/* System Status Footnote */}
      <div className="p-3 rounded-[6px] bg-sidebar-hover border border-sidebar-border text-xs text-sidebar-text">
        <div className="flex items-center gap-2 mb-1">
          <span className="w-2 h-2 rounded-full bg-status-safe"></span>
          <span className="font-semibold text-white">DIS engine online</span>
        </div>
        <p className="text-[11px] text-sidebar-text/70 leading-tight">
          Rule-based FEFO, pessimistic locks, 15m scan active.
        </p>
      </div>
    </aside>
  );
};

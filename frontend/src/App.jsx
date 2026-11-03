import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { FilterProvider } from './context/FilterContext';
import { Navbar } from './components/ui/Navbar';
import { Sidebar } from './components/ui/Sidebar';
import { MovementDrawer } from './components/ui/MovementDrawer';

// Pages
import { Login } from './pages/Login';
import { CommandCenter } from './pages/CommandCenter';
import { StockAnalytics } from './pages/StockAnalytics';
import { ExpiryWastage } from './pages/ExpiryWastage';
import { InventoryIntelligence } from './pages/InventoryIntelligence';
import { Suppliers } from './pages/Suppliers';
import { Alerts } from './pages/Alerts';
import { Movements } from './pages/Movements';
import { ValidationResults } from './pages/ValidationResults';
import { AdminSettings } from './pages/AdminSettings';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 10000,
      retry: 1,
    },
  },
});

const ProtectedLayout = () => {
  const { user, loading, isAdmin } = useAuth();
  const [drawerOpen, setDrawerOpen] = useState(false);

  if (loading) {
    return (
      <div className="min-h-screen bg-bg flex items-center justify-center text-brand font-mono text-sm">
        Initializing Drug Inventory System...
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen bg-bg text-text flex flex-col font-sans">
      <Navbar onOpenMovementDrawer={() => setDrawerOpen(true)} />
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-6 max-w-7xl mx-auto w-full overflow-y-auto">
          <Routes>
            <Route path="/" element={<CommandCenter />} />
            <Route path="/stock" element={<StockAnalytics />} />
            <Route path="/expiry" element={<ExpiryWastage />} />
            <Route path="/intelligence" element={<InventoryIntelligence onOpenMovementDrawer={() => setDrawerOpen(true)} />} />
            <Route path="/suppliers" element={<Suppliers />} />
            <Route path="/alerts" element={<Alerts />} />
            <Route path="/movements" element={<Movements onOpenMovementDrawer={() => setDrawerOpen(true)} />} />
            <Route path="/validation" element={<ValidationResults />} />
            {isAdmin && <Route path="/admin" element={<AdminSettings />} />}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>

      <MovementDrawer
        isOpen={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSuccess={() => {
          queryClient.invalidateQueries();
        }}
      />
    </div>
  );
};

export const App = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <FilterProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/*" element={<ProtectedLayout />} />
              </Routes>
            </BrowserRouter>
          </FilterProvider>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;

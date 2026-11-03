import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import apiClient from '../../api/client';
import { X, CheckCircle2, AlertOctagon } from 'lucide-react';

export const MovementDrawer = ({ isOpen, onClose, onSuccess }) => {
  const { isAdmin, isPharmacist, isWarehouse } = useAuth();

  const [activeTab, setActiveTab] = useState(isWarehouse && !isPharmacist ? 'INBOUND' : 'OUTBOUND');
  const [drugs, setDrugs] = useState([]);
  const [suppliers, setSuppliers] = useState([]);

  // Inbound Form
  const [inboundDrugId, setInboundDrugId] = useState('');
  const [inboundSupplierId, setInboundSupplierId] = useState('');
  const [mfgDate, setMfgDate] = useState('2026-01-15');
  const [expDate, setExpDate] = useState('2027-06-15');
  const [inboundQty, setInboundQty] = useState(100);

  // Outbound Form (FEFO preview)
  const [outboundDrugId, setOutboundDrugId] = useState('');
  const [outboundQty, setOutboundQty] = useState(20);
  const [availableBatches, setAvailableBatches] = useState([]);

  // Other types (Write-off, Return, Adjustment)
  const [batchId, setBatchId] = useState('');
  const [directQty, setDirectQty] = useState(10);
  const [location, setLocation] = useState('store room');

  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    if (isOpen) {
      setErrorMsg('');
      setSuccessMsg('');
      apiClient.get('/drugs?size=100').then(res => {
        setDrugs(res.data.content || []);
        if (res.data.content?.length > 0) {
          setInboundDrugId(res.data.content[0].drugId);
          setOutboundDrugId(res.data.content[0].drugId);
        }
      });
      apiClient.get('/suppliers?size=50').then(res => {
        setSuppliers(res.data.content || []);
        if (res.data.content?.length > 0) {
          setInboundSupplierId(res.data.content[0].supplierId);
        }
      });
    }
  }, [isOpen]);

  // Fetch FEFO batch preview when outbound drug changes
  useEffect(() => {
    if (outboundDrugId) {
      apiClient.get(`/batches?drugId=${outboundDrugId}&status=ACTIVE&size=50`)
        .then(res => setAvailableBatches(res.data.content || []))
        .catch(() => setAvailableBatches([]));
    }
  }, [outboundDrugId]);

  if (!isOpen) return null;

  const handleInboundSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg('');
    try {
      await apiClient.post('/transactions/inbound', {
        drugId: inboundDrugId,
        supplierId: inboundSupplierId,
        manufacturingDate: mfgDate,
        expiryDate: expDate,
        quantity: Number(inboundQty),
        location: 'store room',
      });
      setSuccessMsg(`Successfully received ${inboundQty} units into inventory.`);
      if (onSuccess) onSuccess();
      setTimeout(onClose, 1200);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to record inbound transaction');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOutboundSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg('');
    try {
      const res = await apiClient.post('/transactions/dispense', {
        drugId: outboundDrugId,
        quantity: Number(outboundQty),
        location: 'dispensing counter',
      });
      setSuccessMsg(`Dispensed ${res.data.dispensedQuantity} units across ${res.data.transactions.length} batch(es) in FEFO order.`);
      if (onSuccess) onSuccess();
      setTimeout(onClose, 1400);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to dispense drug');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDirectSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg('');
    try {
      await apiClient.post('/transactions', {
        batchId,
        type: activeTab,
        quantity: Number(directQty),
        location,
      });
      setSuccessMsg(`Successfully recorded ${activeTab} transaction.`);
      if (onSuccess) onSuccess();
      setTimeout(onClose, 1200);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to record transaction');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/40">
      <div className="w-full max-w-lg bg-surface border-l border-border p-6 flex flex-col justify-between overflow-y-auto">
        <div>
          {/* Header */}
          <div className="flex items-center justify-between pb-4 border-b border-border">
            <div>
              <h2 className="text-section-title text-text">Record stock movement</h2>
              <p className="text-xs text-text-muted">Strict rule-based transactions & FEFO allocation</p>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-[6px] hover:bg-surface-2 text-text-muted hover:text-text transition-colors duration-150"
            >
              <X size={18} strokeWidth={1.75} />
            </button>
          </div>

          {/* Role Aware Movement Tabs */}
          <div className="flex gap-1.5 mt-4 p-1 rounded-[6px] bg-surface-2 border border-border text-xs">
            {isWarehouse && (
              <button
                onClick={() => { setActiveTab('INBOUND'); setErrorMsg(''); }}
                className={`flex-1 py-1.5 rounded-[4px] font-medium transition-colors duration-150 ${
                  activeTab === 'INBOUND' ? 'bg-brand text-white' : 'text-text-muted hover:text-text'
                }`}
              >
                Inbound
              </button>
            )}
            {isPharmacist && (
              <button
                onClick={() => { setActiveTab('OUTBOUND'); setErrorMsg(''); }}
                className={`flex-1 py-1.5 rounded-[4px] font-medium transition-colors duration-150 ${
                  activeTab === 'OUTBOUND' ? 'bg-brand text-white' : 'text-text-muted hover:text-text'
                }`}
              >
                Outbound (FEFO)
              </button>
            )}
            {isPharmacist && (
              <button
                onClick={() => { setActiveTab('EXPIRED_WRITE_OFF'); setErrorMsg(''); }}
                className={`flex-1 py-1.5 rounded-[4px] font-medium transition-colors duration-150 ${
                  activeTab === 'EXPIRED_WRITE_OFF' ? 'bg-status-critical text-white' : 'text-text-muted hover:text-text'
                }`}
              >
                Write-off
              </button>
            )}
            {isAdmin && (
              <button
                onClick={() => { setActiveTab('ADJUSTMENT'); setErrorMsg(''); }}
                className={`flex-1 py-1.5 rounded-[4px] font-medium transition-colors duration-150 ${
                  activeTab === 'ADJUSTMENT' ? 'bg-brand text-white' : 'text-text-muted hover:text-text'
                }`}
              >
                Adjustment
              </button>
            )}
          </div>

          {errorMsg && (
            <div className="mt-4 p-3 rounded-[6px] bg-status-critical-tint border border-status-critical/30 text-status-critical text-xs flex items-center gap-2">
              <AlertOctagon size={16} strokeWidth={1.75} className="shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {successMsg && (
            <div className="mt-4 p-3 rounded-[6px] bg-status-safe-tint border border-status-safe/30 text-status-safe text-xs flex items-center gap-2">
              <CheckCircle2 size={16} strokeWidth={1.75} className="shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          {/* Inbound Form */}
          {activeTab === 'INBOUND' && (
            <form onSubmit={handleInboundSubmit} className="mt-5 space-y-4 text-xs">
              <div>
                <label className="text-field-label text-text-muted mb-1 block">Select drug</label>
                <select
                  value={inboundDrugId}
                  onChange={(e) => setInboundDrugId(e.target.value)}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                >
                  {drugs.map(d => (
                    <option key={d.drugId} value={d.drugId}>{d.name} ({d.drugId})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-field-label text-text-muted mb-1 block">Supplier</label>
                <select
                  value={inboundSupplierId}
                  onChange={(e) => setInboundSupplierId(e.target.value)}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                >
                  {suppliers.map(s => (
                    <option key={s.supplierId} value={s.supplierId}>{s.name} (L={s.leadTimeDays}d)</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-field-label text-text-muted mb-1 block">Manufacturing date</label>
                  <input
                    type="date"
                    value={mfgDate}
                    onChange={(e) => setMfgDate(e.target.value)}
                    className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                    required
                  />
                </div>
                <div>
                  <label className="text-field-label text-text-muted mb-1 block">Expiry date</label>
                  <input
                    type="date"
                    value={expDate}
                    onChange={(e) => setExpDate(e.target.value)}
                    className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="text-field-label text-text-muted mb-1 block">Inbound quantity</label>
                <input
                  type="number"
                  min="1"
                  value={inboundQty}
                  onChange={(e) => setInboundQty(e.target.value)}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="btn-primary w-full mt-6 disabled:opacity-50"
              >
                {submitting ? 'Recording inbound...' : 'Receive stock & create batch'}
              </button>
            </form>
          )}

          {/* Outbound Dispense Form with FEFO Preview */}
          {activeTab === 'OUTBOUND' && (
            <form onSubmit={handleOutboundSubmit} className="mt-5 space-y-4 text-xs">
              <div>
                <label className="text-field-label text-text-muted mb-1 block">Drug to dispense</label>
                <select
                  value={outboundDrugId}
                  onChange={(e) => setOutboundDrugId(e.target.value)}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                >
                  {drugs.map(d => (
                    <option key={d.drugId} value={d.drugId}>{d.name} ({d.drugId})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-field-label text-text-muted mb-1 block">Quantity to dispense</label>
                <input
                  type="number"
                  min="1"
                  value={outboundQty}
                  onChange={(e) => setOutboundQty(e.target.value)}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                  required
                />
              </div>

              {/* FEFO Queue Preview */}
              <div className="p-3 rounded-[6px] bg-surface-2 border border-border">
                <span className="block font-semibold text-text mb-2">FEFO queue (earliest expiry first):</span>
                {availableBatches.length === 0 ? (
                  <span className="text-status-critical">No active unexpired batches found.</span>
                ) : (
                  <div className="space-y-1.5 max-h-40 overflow-y-auto">
                    {availableBatches.map((b, i) => (
                      <div key={b.batchId} className="flex justify-between items-center text-xs p-2 rounded-[4px] bg-surface border border-border">
                        <span className="font-mono text-text">
                          {i + 1}. {b.batchId}
                        </span>
                        <div className="text-right">
                          <span className="font-mono text-brand font-semibold mr-2 tabular-nums">{b.quantity} units</span>
                          <span className="text-text-muted text-[11px]">exp {b.expiryDate}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <button
                type="submit"
                disabled={submitting || availableBatches.length === 0}
                className="btn-primary w-full mt-6 disabled:opacity-50"
              >
                {submitting ? 'Dispensing...' : 'Dispense outbound (FEFO)'}
              </button>
            </form>
          )}

          {/* Write-off / Return / Adjustment Form */}
          {(activeTab === 'EXPIRED_WRITE_OFF' || activeTab === 'RETURN' || activeTab === 'ADJUSTMENT') && (
            <form onSubmit={handleDirectSubmit} className="mt-5 space-y-4 text-xs">
              <div>
                <label className="text-field-label text-text-muted mb-1 block">Target batch ID</label>
                <input
                  type="text"
                  placeholder="e.g. BT-AMOX-500-2601-001"
                  value={batchId}
                  onChange={(e) => setBatchId(e.target.value)}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                  required
                />
              </div>

              <div>
                <label className="text-field-label text-text-muted mb-1 block">
                  Quantity {activeTab === 'ADJUSTMENT' ? '(positive or negative)' : ''}
                </label>
                <input
                  type="number"
                  value={directQty}
                  onChange={(e) => setDirectQty(e.target.value)}
                  className="w-full h-9 bg-surface border border-border rounded-[6px] px-3 text-text font-mono focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={submitting || !batchId}
                className={`${activeTab === 'EXPIRED_WRITE_OFF' ? 'btn-destructive' : 'btn-primary'} w-full mt-6 disabled:opacity-50`}
              >
                {submitting ? 'Recording...' : `Confirm ${activeTab.toLowerCase().replace('_', ' ')}`}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

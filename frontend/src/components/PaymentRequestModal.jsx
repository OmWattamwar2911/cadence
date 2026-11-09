import React, { useState } from 'react';
import apiClient from '../api/axios';
import { rupeesToPaise } from '../utils/money';
import { Users, X, Send, AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';

export default function PaymentRequestModal({ isOpen, onClose, onCreated }) {
  const [payerEmail, setPayerEmail] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [reference, setReference] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!payerEmail.trim()) {
      setError('Please provide the email of the person to request money from.');
      return;
    }

    const amountInPaise = rupeesToPaise(amountRupees);
    if (amountInPaise <= 0) {
      setError('Requested amount must be greater than ₹0.00.');
      return;
    }

    setLoading(true);
    try {
      await apiClient.post('/payment-requests', {
        payerEmail: payerEmail.trim(),
        amount: amountInPaise,
        reference: reference.trim() || undefined,
      });

      setSuccess(`Payment request for ₹${parseFloat(amountRupees).toFixed(2)} sent to ${payerEmail}!`);
      setPayerEmail('');
      setAmountRupees('');
      setReference('');
      if (onCreated) {
        onCreated();
      }
      setTimeout(() => {
        setSuccess('');
        onClose();
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit payment request.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        backgroundColor: 'rgba(23, 30, 25, 0.75)',
        backdropFilter: 'blur(4px)',
        zIndex: 10000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '520px',
          padding: '28px 32px',
          borderRadius: 'var(--radius-card)',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.35)',
          background: '#ffffff',
          position: 'relative',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-light)', paddingBottom: '16px', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Users size={22} color="var(--charcoal)" />
            <div>
              <h2 style={{ fontSize: '20px', margin: 0, color: 'var(--charcoal)' }}>
                REQUEST MONEY / SPLIT BILL
              </h2>
              <span style={{ fontSize: '12px', color: '#64748b' }}>
                Ask a peer to pay you back with 1-click approval
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: '#64748b',
              padding: '6px',
            }}
            aria-label="Close"
          >
            <X size={20} />
          </button>
        </div>

        {error && (
          <div className="alert alert-danger" style={{ marginBottom: '16px' }}>
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="alert alert-success" style={{ marginBottom: '16px' }}>
            <CheckCircle2 size={18} />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="payerEmail">REQUEST FROM (USER EMAIL)</label>
            <input
              id="payerEmail"
              type="email"
              className="form-control"
              placeholder="friend@example.com"
              value={payerEmail}
              onChange={(e) => setPayerEmail(e.target.value)}
              disabled={loading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="reqAmount">AMOUNT TO REQUEST (₹)</label>
            <input
              id="reqAmount"
              type="number"
              step="0.01"
              min="0.01"
              className="form-control"
              placeholder="e.g. 500.00"
              value={amountRupees}
              onChange={(e) => setAmountRupees(e.target.value)}
              disabled={loading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="reqMemo">SPLIT MEMO / NOTE</label>
            <input
              id="reqMemo"
              type="text"
              className="form-control"
              placeholder="e.g. Dinner split, Uber ride, Utilities"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
              disabled={loading}
            />
          </div>

          <div style={{ marginTop: '24px' }}>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%' }}
              disabled={loading}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="animate-spin" /> SUBMITTING REQUEST...
                </>
              ) : (
                <>
                  <Send size={16} /> SEND PAYMENT REQUEST
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

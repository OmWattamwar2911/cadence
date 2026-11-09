import React, { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import apiClient from '../api/axios';
import { rupeesToPaise, formatPaise } from '../utils/money';
import { ArrowDownToLine, AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';

export default function Deposit() {
  const [amountRupees, setAmountRupees] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    const amountInPaise = rupeesToPaise(amountRupees);
    if (amountInPaise <= 0) {
      setError('Deposit amount must be strictly greater than ₹0.00.');
      return;
    }

    setLoading(true);
    try {
      const idemKey = `dep-${uuidv4()}`;
      const res = await apiClient.post(
        '/wallet/deposit',
        { amount: amountInPaise },
        { headers: { 'Idempotency-Key': idemKey } }
      );

      setSuccess(`Deposit of ${formatPaise(amountInPaise)} succeeded! New Balance: ${formatPaise(res.data.balance)}`);
      setAmountRupees('');
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else {
        setError('Deposit failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: '500px', paddingTop: '40px', paddingBottom: '60px' }}>
      <div className="card">
        <div style={{ marginBottom: '24px' }}>
          <h1 style={{ fontSize: '32px', color: 'var(--charcoal)', marginBottom: '6px' }}>
            DEPOSIT FUNDS
          </h1>
          <p style={{ color: '#64748b', fontSize: '14px' }}>
            Simulate an instant banking top-up to your wallet
          </p>
        </div>

        {error && (
          <div className="alert alert-danger" role="alert">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="alert alert-success">
            <CheckCircle2 size={18} />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="deposit-amount">AMOUNT IN RUPEES (₹)</label>
            <input
              id="deposit-amount"
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
            {amountRupees && !isNaN(amountRupees) && parseFloat(amountRupees) > 0 && (
              <small style={{ color: '#64748b', display: 'block', marginTop: '6px', fontSize: '12px' }}>
                Minor units: <strong>{rupeesToPaise(amountRupees)} paise</strong>
              </small>
            )}
          </div>

          {/* Quick Amount Tiles */}
          <div style={{ display: 'flex', gap: '8px', marginBottom: '24px', flexWrap: 'wrap' }}>
            {[100, 500, 1000, 5000].map((val) => (
              <button
                key={val}
                type="button"
                className="btn btn-secondary"
                style={{ flex: '1 1 80px', padding: '8px 0', fontSize: '13px' }}
                onClick={() => setAmountRupees(String(val))}
                disabled={loading}
              >
                +₹{val}
              </button>
            ))}
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%' }}
            disabled={loading}
          >
            {loading ? (
              <>
                <Loader2 size={16} className="animate-spin" /> PROCESSING DEPOSIT...
              </>
            ) : (
              <>
                <ArrowDownToLine size={16} /> CONFIRM DEPOSIT
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}

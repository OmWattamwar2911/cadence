import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { v4 as uuidv4 } from 'uuid';
import apiClient from '../api/axios';
import { rupeesToPaise, formatPaise } from '../utils/money';
import { ArrowDownToLine, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function Deposit() {
  const [amountRupees, setAmountRupees] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const navigate = useNavigate();

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

      setSuccess(`Successfully deposited ${formatPaise(amountInPaise)}! New balance: ${formatPaise(res.data.balance)}`);
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
    <div className="container" style={{ maxWidth: '480px' }}>
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}>
          <div style={{ padding: '8px', background: 'var(--success-light)', color: 'var(--success)', borderRadius: '8px' }}>
            <ArrowDownToLine size={22} />
          </div>
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 700 }}>Deposit Funds</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Simulate instant bank top-up to your wallet</p>
          </div>
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

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="deposit-amount">Amount in Rupees (₹)</label>
            <input
              id="deposit-amount"
              type="number"
              step="0.01"
              min="0.01"
              className="form-control"
              placeholder="e.g. 1000.00"
              value={amountRupees}
              onChange={(e) => setAmountRupees(e.target.value)}
              disabled={loading}
              required
            />
            {amountRupees && !isNaN(amountRupees) && parseFloat(amountRupees) > 0 && (
              <small style={{ color: 'var(--text-muted)', display: 'block', marginTop: '4px' }}>
                Minor units: {rupeesToPaise(amountRupees)} paise
              </small>
            )}
          </div>

          <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
            {[100, 500, 1000, 5000].map((quickVal) => (
              <button
                key={quickVal}
                type="button"
                className="btn btn-secondary"
                style={{ flex: 1, padding: '6px 0', fontSize: '12px' }}
                onClick={() => setAmountRupees(String(quickVal))}
                disabled={loading}
              >
                +₹{quickVal}
              </button>
            ))}
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%' }}
            disabled={loading}
          >
            <ArrowDownToLine size={16} />
            {loading ? 'Processing Deposit...' : 'Confirm Deposit'}
          </button>
        </form>
      </div>
    </div>
  );
}

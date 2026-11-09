import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { v4 as uuidv4 } from 'uuid';
import apiClient from '../api/axios';
import { rupeesToPaise, formatPaise } from '../utils/money';
import { Send, AlertCircle, CheckCircle2, RotateCw } from 'lucide-react';

export default function SendMoney() {
  const [toEmail, setToEmail] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [reference, setReference] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isNetworkFailure, setIsNetworkFailure] = useState(false);
  
  // Track idempotency key for network retry capability
  const [idempotencyKey, setIdempotencyKey] = useState(uuidv4());

  const navigate = useNavigate();

  // Reset idempotency key if user modifies core transaction parameters
  const handleRecipientChange = (e) => {
    setToEmail(e.target.value);
    setIsNetworkFailure(false);
    setError('');
  };

  const handleAmountChange = (e) => {
    setAmountRupees(e.target.value);
    setIsNetworkFailure(false);
    setError('');
  };

  const handleReferenceChange = (e) => {
    setReference(e.target.value);
  };

  const executeSend = async (keyToUse) => {
    setError('');
    setSuccess('');

    // Form validation
    if (!toEmail.trim()) {
      setError('Recipient email is required.');
      return;
    }

    const amountInPaise = rupeesToPaise(amountRupees);
    if (amountInPaise <= 0) {
      setError('Transfer amount must be strictly greater than ₹0.00.');
      return;
    }

    setLoading(true);
    setIsNetworkFailure(false);

    try {
      const payload = {
        toEmail: toEmail.trim(),
        amount: amountInPaise,
        reference: reference.trim() || undefined,
      };

      const res = await apiClient.post('/transfers', payload, {
        headers: {
          'Idempotency-Key': keyToUse,
        },
      });

      setSuccess(`Transfer of ${formatPaise(res.data.amount)} to ${toEmail} completed successfully!`);
      // Reset form and generate fresh idempotency key for next transaction
      setToEmail('');
      setAmountRupees('');
      setReference('');
      setIdempotencyKey(uuidv4());
    } catch (err) {
      if (err.response) {
        // Application error response (4xx, 5xx)
        const status = err.response.status;
        const data = err.response.data;

        if (status === 429) {
          const retryAfter = err.response.headers['retry-after'] || '60';
          setError(`Rate limit exceeded. Please wait ${retryAfter} seconds before trying again.`);
        } else if (data && data.message) {
          setError(data.message);
        } else {
          setError(`Transfer failed with HTTP error ${status}.`);
        }
      } else {
        // Network error (timeout, connection reset, offline)
        setIsNetworkFailure(true);
        setError('Network failure: Unable to reach payments server. You can safely retry without moving money twice.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    executeSend(idempotencyKey);
  };

  const handleRetry = () => {
    // Re-use exact same idempotencyKey as required by idempotency algorithm
    executeSend(idempotencyKey);
  };

  return (
    <div className="container" style={{ maxWidth: '520px' }}>
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}>
          <div style={{ padding: '8px', background: 'var(--primary-light)', color: 'var(--primary)', borderRadius: '8px' }}>
            <Send size={22} />
          </div>
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 700 }}>Send Money</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Instant, idempotent peer-to-peer transfer</p>
          </div>
        </div>

        {error && (
          <div className="alert alert-danger" role="alert">
            <AlertCircle size={18} />
            <div style={{ flex: 1 }}>{error}</div>
          </div>
        )}

        {success && (
          <div className="alert alert-success">
            <CheckCircle2 size={18} />
            <div style={{ flex: 1 }}>{success}</div>
          </div>
        )}

        {isNetworkFailure && (
          <div style={{ marginBottom: '16px', padding: '12px', background: 'var(--warning-light)', borderRadius: '8px', border: '1px solid #fde68a' }}>
            <p style={{ fontSize: '13px', color: '#92400e', marginBottom: '8px' }}>
              <strong>Safe Retry Active:</strong> Idempotency key <code>{idempotencyKey.slice(0, 8)}...</code> is preserved.
            </p>
            <button
              type="button"
              onClick={handleRetry}
              disabled={loading}
              className="btn"
              style={{ background: '#d97706', color: '#ffffff', width: '100%' }}
            >
              <RotateCw size={16} /> {loading ? 'Retrying...' : 'Retry Same Transfer'}
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate data-testid="send-money-form">
          <div className="form-group">
            <label htmlFor="toEmail">Recipient Email</label>
            <input
              id="toEmail"
              name="toEmail"
              type="email"
              className="form-control"
              placeholder="recipient@example.com"
              value={toEmail}
              onChange={handleRecipientChange}
              disabled={loading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="amount">Amount in Rupees (₹)</label>
            <input
              id="amount"
              name="amount"
              type="number"
              step="0.01"
              min="0.01"
              className="form-control"
              placeholder="e.g. 250.00"
              value={amountRupees}
              onChange={handleAmountChange}
              disabled={loading}
              required
            />
            {amountRupees && !isNaN(amountRupees) && parseFloat(amountRupees) > 0 && (
              <small style={{ color: 'var(--text-muted)', display: 'block', marginTop: '4px' }}>
                Minor units: {rupeesToPaise(amountRupees)} paise
              </small>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="reference">Reference / Memo (Optional)</label>
            <input
              id="reference"
              name="reference"
              type="text"
              maxLength={255}
              className="form-control"
              placeholder="e.g. Dinner split, Rent"
              value={reference}
              onChange={handleReferenceChange}
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
              <Send size={16} />
              {loading ? 'Processing Transfer...' : 'Send Money'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

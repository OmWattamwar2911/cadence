import React, { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import apiClient from '../api/axios';
import { rupeesToPaise, formatPaise } from '../utils/money';
import { Send, AlertCircle, CheckCircle2, RotateCw, Loader2 } from 'lucide-react';

export default function SendMoney() {
  const [toEmail, setToEmail] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [reference, setReference] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isNetworkFailure, setIsNetworkFailure] = useState(false);

  // Preserve UUID Idempotency-Key across network error retries
  const [idempotencyKey, setIdempotencyKey] = useState(uuidv4());

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
      setToEmail('');
      setAmountRupees('');
      setReference('');
      setIdempotencyKey(uuidv4());
    } catch (err) {
      if (err.response) {
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
    executeSend(idempotencyKey);
  };

  return (
    <div className="container" style={{ maxWidth: '540px', paddingTop: '40px', paddingBottom: '60px' }}>
      <div className="card">
        <div style={{ marginBottom: '24px' }}>
          <h1 style={{ fontSize: '32px', color: 'var(--charcoal)', marginBottom: '6px' }}>
            SEND MONEY
          </h1>
          <p style={{ color: '#64748b', fontSize: '14px' }}>
            Instant, idempotent transfer backed by database row locking
          </p>
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
          <div
            style={{
              marginBottom: '20px',
              padding: '16px',
              background: '#fef3c7',
              borderRadius: 'var(--radius-input)',
              border: '1px solid #fde68a',
            }}
          >
            <p style={{ fontSize: '13px', color: '#92400e', marginBottom: '10px', lineHeight: 1.5 }}>
              <strong>Idempotent Safe Retry:</strong> Key <code>{idempotencyKey.slice(0, 8)}...</code> is active.
              Retrying will NOT move money twice if the initial request completed.
            </p>
            <button
              type="button"
              onClick={handleRetry}
              disabled={loading}
              className="btn"
              style={{
                background: 'var(--charcoal)',
                color: 'var(--yellow)',
                width: '100%',
                padding: '10px 20px',
                fontSize: '14px',
              }}
            >
              <RotateCw size={16} /> {loading ? 'RETRYING TRANSFER...' : 'RETRY SAME TRANSFER'}
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate data-testid="send-money-form">
          <div className="form-group">
            <label htmlFor="toEmail">RECIPIENT EMAIL</label>
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
            <label htmlFor="amount">AMOUNT IN RUPEES (₹)</label>
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
              <small style={{ color: '#64748b', display: 'block', marginTop: '6px', fontSize: '12px' }}>
                Minor units: <strong>{rupeesToPaise(amountRupees)} paise</strong>
              </small>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="reference">REFERENCE / MEMO (OPTIONAL)</label>
            <input
              id="reference"
              name="reference"
              type="text"
              maxLength={255}
              className="form-control"
              placeholder="e.g. Dinner, Rent, Invoice #12"
              value={reference}
              onChange={handleReferenceChange}
              disabled={loading}
            />
          </div>

          <div style={{ marginTop: '28px' }}>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%' }}
              disabled={loading}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="animate-spin" /> PROCESSING TRANSFER...
                </>
              ) : (
                <>
                  <Send size={16} /> SEND MONEY
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

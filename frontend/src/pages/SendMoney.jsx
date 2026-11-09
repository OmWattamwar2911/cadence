import React, { useState, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';
import apiClient from '../api/axios';
import { rupeesToPaise, formatPaise } from '../utils/money';
import {
  Send,
  AlertCircle,
  CheckCircle2,
  RotateCw,
  Loader2,
  CalendarClock,
  Clock,
  Trash2,
} from 'lucide-react';

export default function SendMoney() {
  const [activeTab, setActiveTab] = useState('instant'); // 'instant' | 'scheduled'

  // Instant Transfer State
  const [toEmail, setToEmail] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [reference, setReference] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isNetworkFailure, setIsNetworkFailure] = useState(false);
  const [idempotencyKey, setIdempotencyKey] = useState(uuidv4());

  // Recurring / Scheduled Transfer State
  const getTomorrowDate = () => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  };

  const [schedEmail, setSchedEmail] = useState('');
  const [schedAmountRupees, setSchedAmountRupees] = useState('');
  const [schedFrequency, setSchedFrequency] = useState('DAILY');
  const [schedStartDate, setSchedStartDate] = useState(getTomorrowDate());
  const [schedLoading, setSchedLoading] = useState(false);
  const [schedError, setSchedError] = useState('');
  const [schedSuccess, setSchedSuccess] = useState('');
  const [scheduledTransfers, setScheduledTransfers] = useState([]);
  const [cancellingId, setCancellingId] = useState(null);

  const fetchScheduledTransfers = async () => {
    try {
      const res = await apiClient.get('/scheduled-transfers');
      setScheduledTransfers(res.data || []);
    } catch (err) {
      console.error('Failed to load standing orders:', err);
    }
  };

  useEffect(() => {
    if (activeTab === 'scheduled') {
      fetchScheduledTransfers();
    }
  }, [activeTab]);

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

  // Scheduled Transfer Handlers
  const handleScheduledSubmit = async (e) => {
    e.preventDefault();
    setSchedError('');
    setSchedSuccess('');

    if (!schedEmail.trim()) {
      setSchedError('Recipient email is required.');
      return;
    }

    const amountInPaise = rupeesToPaise(schedAmountRupees);
    if (amountInPaise <= 0) {
      setSchedError('Recurring amount must be greater than ₹0.00.');
      return;
    }

    setSchedLoading(true);
    try {
      await apiClient.post('/scheduled-transfers', {
        toEmail: schedEmail.trim(),
        amount: amountInPaise,
        frequency: schedFrequency,
        startDate: schedStartDate,
      });

      setSchedSuccess(`Recurring ${schedFrequency.toLowerCase()} transfer of ₹${parseFloat(schedAmountRupees).toFixed(2)} scheduled successfully!`);
      setSchedEmail('');
      setSchedAmountRupees('');
      setSchedStartDate(getTomorrowDate());
      await fetchScheduledTransfers();
    } catch (err) {
      setSchedError(err.response?.data?.message || 'Failed to schedule recurring auto-transfer.');
    } finally {
      setSchedLoading(false);
    }
  };

  const handleCancelSchedule = async (id) => {
    setCancellingId(id);
    try {
      await apiClient.delete(`/scheduled-transfers/${id}`);
      await fetchScheduledTransfers();
    } catch (err) {
      console.error('Failed to cancel schedule:', err);
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <div className="container" style={{ maxWidth: '600px', paddingTop: '40px', paddingBottom: '60px' }}>
      {/* Tab Switcher */}
      <div
        style={{
          display: 'flex',
          background: '#f1f5f9',
          borderRadius: 'var(--radius-pill)',
          padding: '4px',
          marginBottom: '24px',
          border: '1px solid var(--border-light)',
        }}
      >
        <button
          type="button"
          onClick={() => setActiveTab('instant')}
          style={{
            flex: 1,
            padding: '10px 16px',
            borderRadius: 'var(--radius-pill)',
            border: 'none',
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'pointer',
            transition: 'all 0.2s',
            background: activeTab === 'instant' ? 'var(--charcoal)' : 'transparent',
            color: activeTab === 'instant' ? '#ffffff' : '#64748b',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
          }}
        >
          <Send size={15} /> INSTANT TRANSFER
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('scheduled')}
          style={{
            flex: 1,
            padding: '10px 16px',
            borderRadius: 'var(--radius-pill)',
            border: 'none',
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'pointer',
            transition: 'all 0.2s',
            background: activeTab === 'scheduled' ? 'var(--charcoal)' : 'transparent',
            color: activeTab === 'scheduled' ? '#ffffff' : '#64748b',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
          }}
        >
          <CalendarClock size={15} /> STANDING ORDERS
        </button>
      </div>

      {activeTab === 'instant' ? (
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
      ) : (
        <div>
          {/* Schedule Form Card */}
          <div className="card" style={{ marginBottom: '32px' }}>
            <div style={{ marginBottom: '24px' }}>
              <h1 style={{ fontSize: '30px', color: 'var(--charcoal)', marginBottom: '6px' }}>
                RECURRING AUTO-TRANSFER
              </h1>
              <p style={{ color: '#64748b', fontSize: '14px' }}>
                Set up automated recurring transfers (daily, weekly, or monthly)
              </p>
            </div>

            {schedError && (
              <div className="alert alert-danger" role="alert">
                <AlertCircle size={18} />
                <div style={{ flex: 1 }}>{schedError}</div>
              </div>
            )}

            {schedSuccess && (
              <div className="alert alert-success">
                <CheckCircle2 size={18} />
                <div style={{ flex: 1 }}>{schedSuccess}</div>
              </div>
            )}

            <form onSubmit={handleScheduledSubmit}>
              <div className="form-group">
                <label htmlFor="schedEmail">RECIPIENT EMAIL</label>
                <input
                  id="schedEmail"
                  type="email"
                  className="form-control"
                  placeholder="landlord@example.com"
                  value={schedEmail}
                  onChange={(e) => setSchedEmail(e.target.value)}
                  disabled={schedLoading}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="schedAmount">AMOUNT IN RUPEES (₹)</label>
                <input
                  id="schedAmount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-control"
                  placeholder="e.g. 5000.00"
                  value={schedAmountRupees}
                  onChange={(e) => setSchedAmountRupees(e.target.value)}
                  disabled={schedLoading}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="form-group">
                  <label htmlFor="schedFrequency">FREQUENCY</label>
                  <select
                    id="schedFrequency"
                    className="form-control"
                    value={schedFrequency}
                    onChange={(e) => setSchedFrequency(e.target.value)}
                    disabled={schedLoading}
                  >
                    <option value="DAILY">Daily</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="schedStartDate">START DATE</label>
                  <input
                    id="schedStartDate"
                    type="date"
                    className="form-control"
                    value={schedStartDate}
                    onChange={(e) => setSchedStartDate(e.target.value)}
                    disabled={schedLoading}
                    required
                  />
                </div>
              </div>

              <div style={{ marginTop: '24px' }}>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ width: '100%' }}
                  disabled={schedLoading}
                >
                  {schedLoading ? (
                    <>
                      <Loader2 size={16} className="animate-spin" /> SCHEDULING AUTO-TRANSFER...
                    </>
                  ) : (
                    <>
                      <Clock size={16} /> SCHEDULE AUTO-TRANSFER
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>

          {/* Active Standing Orders Card */}
          <div className="card">
            <h2 style={{ fontSize: '20px', color: 'var(--charcoal)', marginBottom: '16px' }}>
              ACTIVE STANDING ORDERS ({scheduledTransfers.filter((s) => s.status === 'ACTIVE').length})
            </h2>

            {scheduledTransfers.length === 0 ? (
              <p style={{ color: '#64748b', fontSize: '13px' }}>
                No standing orders configured yet.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {scheduledTransfers.map((sched) => (
                  <div
                    key={sched.id}
                    style={{
                      background: '#ffffff',
                      border: '1px solid var(--border-light)',
                      borderRadius: 'var(--radius-input)',
                      padding: '14px 16px',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: '12px',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontWeight: 700, fontSize: '14px', color: 'var(--charcoal)' }}>
                          {formatPaise(sched.amount)}
                        </span>
                        <span className="badge badge-credit" style={{ fontSize: '11px' }}>
                          {sched.frequency}
                        </span>
                        <span style={{ fontSize: '11px', color: sched.status === 'ACTIVE' ? 'var(--success)' : '#64748b', fontWeight: 600 }}>
                          ● {sched.status}
                        </span>
                      </div>
                      <div style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                        To: <strong>{sched.recipientEmail}</strong>
                      </div>
                      <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '4px' }}>
                        Next run: {sched.nextExecutionAt ? new Date(sched.nextExecutionAt).toLocaleString() : '—'}
                      </div>
                    </div>

                    {sched.status === 'ACTIVE' && (
                      <button
                        type="button"
                        onClick={() => handleCancelSchedule(sched.id)}
                        disabled={cancellingId === sched.id}
                        className="btn btn-secondary"
                        style={{ fontSize: '12px', padding: '6px 12px', color: 'var(--danger)', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                      >
                        <Trash2 size={13} /> CANCEL
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

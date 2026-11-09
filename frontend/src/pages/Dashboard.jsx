import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/axios';
import { formatPaise } from '../utils/money';
import LedgerAuditModal from '../components/LedgerAuditModal';
import PaymentRequestModal from '../components/PaymentRequestModal';
import {
  ArrowUpRight,
  PlusCircle,
  History,
  AlertCircle,
  RefreshCw,
  ShieldCheck,
  Users,
  CheckCircle2,
  XCircle,
  Loader2,
} from 'lucide-react';

export default function Dashboard() {
  const [wallet, setWallet] = useState(null);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [incomingRequests, setIncomingRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');

  // Modals state
  const [isAuditOpen, setIsAuditOpen] = useState(false);
  const [isRequestOpen, setIsRequestOpen] = useState(false);

  // In-flight action tracking
  const [processingRequestId, setProcessingRequestId] = useState(null);

  const fetchDashboardData = useCallback(async (showFullLoader = true) => {
    if (showFullLoader) setLoading(true);
    setError('');
    try {
      const [walletRes, txRes, reqRes] = await Promise.all([
        apiClient.get('/wallet'),
        apiClient.get('/transactions?page=0&size=5'),
        apiClient.get('/payment-requests/incoming'),
      ]);
      setWallet(walletRes.data);
      setRecentTransactions(txRes.data.content || []);
      // Filter pending incoming requests
      const pending = (reqRes.data || []).filter((r) => r.status === 'PENDING');
      setIncomingRequests(pending);
    } catch (err) {
      console.error('Failed to load dashboard:', err);
      setError('Failed to load wallet details. Please refresh the page.');
    } finally {
      if (showFullLoader) setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData(true);

    // Auto-refresh when real-time SSE events arrive
    const handleWalletRefresh = () => {
      fetchDashboardData(false);
    };
    window.addEventListener('cadence:refresh_wallet', handleWalletRefresh);
    return () => {
      window.removeEventListener('cadence:refresh_wallet', handleWalletRefresh);
    };
  }, [fetchDashboardData]);

  const handlePayRequest = async (requestId) => {
    setProcessingRequestId(requestId);
    setActionError('');
    setActionSuccess('');
    try {
      const res = await apiClient.post(`/payment-requests/${requestId}/pay`);
      setActionSuccess(`Payment of ${formatPaise(res.data.amount)} approved and transferred successfully!`);
      await fetchDashboardData(false);
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to execute payment approval.');
    } finally {
      setProcessingRequestId(null);
    }
  };

  const handleDeclineRequest = async (requestId) => {
    setProcessingRequestId(requestId);
    setActionError('');
    setActionSuccess('');
    try {
      await apiClient.post(`/payment-requests/${requestId}/decline`);
      setActionSuccess('Payment request declined.');
      await fetchDashboardData(false);
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to decline request.');
    } finally {
      setProcessingRequestId(null);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ textAlign: 'center', paddingTop: '80px' }}>
        <p style={{ color: 'var(--sage)', fontSize: '16px' }}>Loading wallet dashboard...</p>
      </div>
    );
  }

  return (
    <div className="container" style={{ paddingTop: '32px', paddingBottom: '64px' }}>
      {error && (
        <div className="alert alert-danger" role="alert">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {actionError && (
        <div className="alert alert-danger" role="alert">
          <AlertCircle size={18} />
          <span>{actionError}</span>
        </div>
      )}

      {actionSuccess && (
        <div className="alert alert-success" role="alert">
          <CheckCircle2 size={18} />
          <span>{actionSuccess}</span>
        </div>
      )}

      {/* Large Charcoal Hero Card */}
      <div
        className="card card-dark"
        style={{
          background: 'var(--charcoal)',
          color: '#ffffff',
          marginBottom: '32px',
          padding: '40px 36px',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <span
                style={{
                  fontSize: '12px',
                  fontWeight: 700,
                  letterSpacing: '0.12em',
                  textTransform: 'uppercase',
                  color: 'var(--sage)',
                }}
              >
                AVAILABLE BALANCE
              </span>
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: 600,
                  background: 'rgba(16, 185, 129, 0.2)',
                  color: '#34d399',
                  padding: '2px 8px',
                  borderRadius: 'var(--radius-pill)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <ShieldCheck size={12} /> HASH-CHAIN VERIFIED
              </span>
            </div>

            <div
              style={{
                fontFamily: 'var(--font-headline)',
                fontSize: 'clamp(3rem, 7vw, 5.5rem)',
                color: '#ffffff',
                lineHeight: 0.9,
                margin: '8px 0 16px 0',
              }}
            >
              {wallet ? (
                <>
                  <span style={{ color: 'var(--yellow)' }}>₹</span>
                  {((wallet.balance || 0) / 100).toLocaleString('en-IN', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </>
              ) : (
                '₹0.00'
              )}
            </div>
            <p style={{ fontSize: '13px', color: 'var(--sage)' }}>
              Wallet ID: #{wallet?.walletId} • Currency: {wallet?.currency || 'INR'} • Minor Units: {wallet?.balance || 0} paise
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              onClick={() => setIsAuditOpen(true)}
              className="btn"
              style={{
                padding: '8px 14px',
                fontSize: '12px',
                background: 'rgba(255, 225, 124, 0.15)',
                color: 'var(--yellow)',
                border: '1px solid rgba(255, 225, 124, 0.3)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <ShieldCheck size={14} /> AUDIT LEDGER CHAIN
            </button>
            <button
              onClick={() => fetchDashboardData(false)}
              className="btn btn-secondary"
              style={{ padding: '8px 14px', fontSize: '12px', background: 'rgba(255,255,255,0.1)', color: '#ffffff', border: '1px solid var(--border-dark)' }}
              aria-label="Refresh balance"
            >
              <RefreshCw size={14} /> Refresh
            </button>
          </div>
        </div>

        {/* Quick Action Buttons */}
        <div style={{ display: 'flex', gap: '16px', marginTop: '32px', flexWrap: 'wrap' }}>
          <Link to="/send" className="btn btn-primary">
            <ArrowUpRight size={18} /> SEND MONEY
          </Link>
          <Link
            to="/deposit"
            className="btn btn-dark-pill"
            style={{
              background: '#272727',
              border: '1px solid var(--border-dark)',
              fontSize: '14px',
              padding: '14px 28px',
            }}
          >
            <PlusCircle size={18} /> DEPOSIT FUNDS
          </Link>
          <button
            type="button"
            onClick={() => setIsRequestOpen(true)}
            className="btn btn-dark-pill"
            style={{
              background: '#272727',
              border: '1px solid var(--border-dark)',
              fontSize: '14px',
              padding: '14px 24px',
              color: 'var(--yellow)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Users size={18} /> REQUEST / SPLIT BILL
          </button>
        </div>
      </div>

      {/* Incoming Payment Requests Alert / Section */}
      {incomingRequests.length > 0 && (
        <div
          className="card"
          style={{
            marginBottom: '32px',
            border: '2px solid var(--charcoal)',
            background: '#fffbeb',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
            <Users size={20} color="var(--charcoal)" />
            <h3 style={{ fontSize: '18px', margin: 0, color: 'var(--charcoal)' }}>
              PENDING PAYMENT REQUESTS ({incomingRequests.length})
            </h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {incomingRequests.map((req) => (
              <div
                key={req.id}
                style={{
                  background: '#ffffff',
                  border: '1px solid var(--border-light)',
                  borderRadius: 'var(--radius-input)',
                  padding: '16px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '12px',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '15px', color: 'var(--charcoal)' }}>
                    {req.requesterEmail} requested <span style={{ color: '#065f46' }}>{formatPaise(req.amount)}</span>
                  </div>
                  {req.reference && (
                    <div style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                      Memo: "{req.reference}"
                    </div>
                  )}
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '4px' }}>
                    Received: {new Date(req.createdAt).toLocaleString()}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    onClick={() => handlePayRequest(req.id)}
                    disabled={processingRequestId === req.id}
                    className="btn btn-primary"
                    style={{ fontSize: '13px', padding: '8px 16px' }}
                  >
                    {processingRequestId === req.id ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <CheckCircle2 size={14} />
                    )}
                    APPROVE & PAY
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDeclineRequest(req.id)}
                    disabled={processingRequestId === req.id}
                    className="btn btn-secondary"
                    style={{ fontSize: '13px', padding: '8px 14px' }}
                  >
                    <XCircle size={14} /> DECLINE
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Activity Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h2 style={{ fontSize: '26px', color: 'var(--charcoal)', marginBottom: '4px' }}>
              RECENT TRANSACTIONS
            </h2>
            <p style={{ color: '#64748b', fontSize: '13px' }}>
              Cryptographically chained double-entry ledger records
            </p>
          </div>
          <Link
            to="/history"
            style={{
              fontSize: '13px',
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              color: 'var(--charcoal)',
            }}
          >
            View Full History <History size={16} />
          </Link>
        </div>

        {recentTransactions.length === 0 ? (
          <div className="empty-state">
            <p style={{ fontSize: '15px', color: '#64748b', marginBottom: '16px' }}>
              No transactions recorded yet in your ledger.
            </p>
            <Link to="/deposit" className="btn btn-primary" style={{ fontSize: '14px', padding: '10px 24px' }}>
              MAKE YOUR FIRST DEPOSIT
            </Link>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>TYPE</th>
                  <th>REFERENCE</th>
                  <th>AMOUNT</th>
                  <th>BALANCE AFTER</th>
                  <th>TIMESTAMP</th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((tx) => {
                  const isPositive = tx.type === 'DEPOSIT' || tx.type === 'CREDIT';
                  return (
                    <tr key={tx.id}>
                      <td>
                        <span className={`badge badge-${tx.type.toLowerCase()}`}>
                          {tx.type}
                        </span>
                      </td>
                      <td style={{ color: '#64748b', fontWeight: 500 }}>
                        {tx.reference || '—'}
                      </td>
                      <td
                        style={{
                          fontWeight: 700,
                          color: isPositive ? '#065f46' : '#991b1b',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {isPositive ? '+ ' : '- '}
                        {formatPaise(tx.amount)}
                      </td>
                      <td style={{ color: '#64748b', fontWeight: 500 }}>
                        {formatPaise(tx.balanceAfter)}
                      </td>
                      <td style={{ color: '#64748b', fontSize: '13px' }}>
                        {new Date(tx.createdAt).toLocaleString()}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modals */}
      <LedgerAuditModal isOpen={isAuditOpen} onClose={() => setIsAuditOpen(false)} />
      <PaymentRequestModal
        isOpen={isRequestOpen}
        onClose={() => setIsRequestOpen(false)}
        onCreated={() => fetchDashboardData(false)}
      />
    </div>
  );
}

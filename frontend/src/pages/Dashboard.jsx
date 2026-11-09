import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/axios';
import { formatPaise } from '../utils/money';
import { ArrowUpRight, PlusCircle, History, AlertCircle, RefreshCw } from 'lucide-react';

export default function Dashboard() {
  const [wallet, setWallet] = useState(null);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const [walletRes, txRes] = await Promise.all([
        apiClient.get('/wallet'),
        apiClient.get('/transactions?page=0&size=5'),
      ]);
      setWallet(walletRes.data);
      setRecentTransactions(txRes.data.content || []);
    } catch (err) {
      console.error('Failed to load dashboard:', err);
      setError('Failed to load wallet details. Please refresh the page.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

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
            <span
              style={{
                fontSize: '12px',
                fontWeight: 700,
                letterSpacing: '0.12em',
                textTransform: 'uppercase',
                color: 'var(--sage)',
                display: 'block',
                marginBottom: '8px',
              }}
            >
              AVAILABLE BALANCE
            </span>
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

          <button
            onClick={fetchDashboardData}
            className="btn btn-secondary"
            style={{ padding: '8px 14px', fontSize: '12px', background: 'rgba(255,255,255,0.1)', color: '#ffffff', border: '1px solid var(--border-dark)' }}
            aria-label="Refresh balance"
          >
            <RefreshCw size={14} /> Refresh
          </button>
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
        </div>
      </div>

      {/* Recent Activity Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h2 style={{ fontSize: '26px', color: 'var(--charcoal)', marginBottom: '4px' }}>
              RECENT TRANSACTIONS
            </h2>
            <p style={{ color: '#64748b', fontSize: '13px' }}>
              Double-entry ledger records (latest 5)
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
    </div>
  );
}

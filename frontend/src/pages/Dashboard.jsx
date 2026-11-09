import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/axios';
import { formatPaise } from '../utils/money';
import { Wallet, ArrowUpRight, ArrowDownLeft, PlusCircle, History, AlertCircle } from 'lucide-react';

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
      <div className="container" style={{ textAlign: 'center', paddingTop: '60px' }}>
        <p style={{ color: 'var(--text-muted)' }}>Loading wallet dashboard...</p>
      </div>
    );
  }

  return (
    <div className="container">
      {error && (
        <div className="alert alert-danger" role="alert">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Balance Card */}
      <div className="card" style={{ marginBottom: '24px', background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)', color: '#ffffff' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <p style={{ fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.05em', opacity: 0.85 }}>Available Balance</p>
            <h1 style={{ fontSize: '36px', fontWeight: 700, margin: '8px 0 16px 0' }}>
              {wallet ? formatPaise(wallet.balance) : '₹0.00'}
            </h1>
            <p style={{ fontSize: '12px', opacity: 0.75 }}>
              Wallet ID: #{wallet?.walletId} • Currency: {wallet?.currency || 'INR'}
            </p>
          </div>
          <div style={{ padding: '10px', background: 'rgba(255, 255, 255, 0.15)', borderRadius: '12px' }}>
            <Wallet size={28} />
          </div>
        </div>

        {/* Quick Actions */}
        <div style={{ display: 'flex', gap: '12px', marginTop: '20px', flexWrap: 'wrap' }}>
          <Link to="/send" className="btn" style={{ background: '#ffffff', color: 'var(--primary)', fontWeight: 600 }}>
            <ArrowUpRight size={18} /> Send Money
          </Link>
          <Link to="/deposit" className="btn" style={{ background: 'rgba(255, 255, 255, 0.2)', color: '#ffffff', fontWeight: 600 }}>
            <PlusCircle size={18} /> Deposit Funds
          </Link>
        </div>
      </div>

      {/* Recent Activity Card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: 600 }}>Recent Transactions</h3>
          <Link to="/history" style={{ fontSize: '14px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '4px' }}>
            View All <History size={15} />
          </Link>
        </div>

        {recentTransactions.length === 0 ? (
          <div className="empty-state">
            <p>No transactions yet. Make a deposit or send money to get started!</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Reference</th>
                  <th>Amount</th>
                  <th>Balance After</th>
                  <th>Date & Time</th>
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
                      <td style={{ color: 'var(--text-muted)' }}>{tx.reference || '—'}</td>
                      <td style={{ fontWeight: 600, color: isPositive ? 'var(--success)' : 'var(--danger)' }}>
                        {isPositive ? '+' : '-'}{formatPaise(tx.amount)}
                      </td>
                      <td style={{ color: 'var(--text-muted)' }}>{formatPaise(tx.balanceAfter)}</td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '13px' }}>
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

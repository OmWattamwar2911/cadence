import React, { useEffect, useState } from 'react';
import apiClient from '../api/axios';
import { formatPaise } from '../utils/money';
import { History as HistoryIcon, Filter, ChevronLeft, ChevronRight, AlertCircle } from 'lucide-react';

export default function HistoryPage() {
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedType, setSelectedType] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const pageSize = 10;

  const fetchTransactions = async (pageIndex = 0) => {
    setLoading(true);
    setError('');

    try {
      const params = new URLSearchParams();
      params.append('page', String(pageIndex));
      params.append('size', String(pageSize));

      if (selectedType) {
        params.append('type', selectedType);
      }
      if (fromDate) {
        params.append('from', new Date(fromDate).toISOString());
      }
      if (toDate) {
        params.append('to', new Date(toDate).toISOString());
      }

      const res = await apiClient.get(`/transactions?${params.toString()}`);
      setTransactions(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setTotalElements(res.data.totalElements || 0);
      setPage(res.data.page || 0);
    } catch (err) {
      console.error('Failed to load transaction history:', err);
      setError('Unable to fetch transaction history. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions(0);
  }, [selectedType]);

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    fetchTransactions(0);
  };

  const handleResetFilter = () => {
    setSelectedType('');
    setFromDate('');
    setToDate('');
  };

  return (
    <div className="container">
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ padding: '8px', background: 'var(--primary-light)', color: 'var(--primary)', borderRadius: '8px' }}>
              <HistoryIcon size={22} />
            </div>
            <div>
              <h2 style={{ fontSize: '20px', fontWeight: 700 }}>Transaction History</h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '13px' }}>
                {totalElements} total entries • Paginated double-entry ledger
              </p>
            </div>
          </div>

          {/* Type quick filter buttons */}
          <div style={{ display: 'flex', gap: '6px' }}>
            {['', 'DEPOSIT', 'DEBIT', 'CREDIT'].map((t) => (
              <button
                key={t}
                type="button"
                className={`btn ${selectedType === t ? 'btn-primary' : 'btn-secondary'}`}
                style={{ padding: '6px 12px', fontSize: '12px' }}
                onClick={() => setSelectedType(t)}
              >
                {t || 'All'}
              </button>
            ))}
          </div>
        </div>

        {/* Date Filter Bar */}
        <form onSubmit={handleFilterSubmit} style={{ display: 'flex', gap: '12px', marginBottom: '20px', flexWrap: 'wrap', alignItems: 'center', background: '#f8fafc', padding: '12px', borderRadius: '8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={16} color="var(--text-muted)" />
            <span style={{ fontSize: '13px', fontWeight: 500 }}>Date Range:</span>
          </div>
          <input
            type="date"
            className="form-control"
            style={{ width: 'auto', padding: '6px 10px', fontSize: '13px' }}
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
          <span style={{ color: 'var(--text-muted)', fontSize: '13px' }}>to</span>
          <input
            type="date"
            className="form-control"
            style={{ width: 'auto', padding: '6px 10px', fontSize: '13px' }}
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
          <button type="submit" className="btn btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
            Apply
          </button>
          {(fromDate || toDate) && (
            <button type="button" onClick={handleResetFilter} className="btn" style={{ padding: '6px 10px', fontSize: '13px', background: 'transparent', color: 'var(--text-muted)' }}>
              Clear
            </button>
          )}
        </form>

        {error && (
          <div className="alert alert-danger" role="alert">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>
            <p>Loading transactions...</p>
          </div>
        ) : transactions.length === 0 ? (
          <div className="empty-state">
            <p>No transactions match your criteria.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Reference</th>
                  <th>Amount</th>
                  <th>Balance After</th>
                  <th>Date & Time</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => {
                  const isPositive = tx.type === 'DEPOSIT' || tx.type === 'CREDIT';
                  return (
                    <tr key={tx.id}>
                      <td style={{ color: 'var(--text-muted)' }}>#{tx.id}</td>
                      <td>
                        <span className={`badge badge-${tx.type.toLowerCase()}`}>
                          {tx.type}
                        </span>
                      </td>
                      <td>{tx.reference || '—'}</td>
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

            {/* Pagination Controls */}
            <div className="pagination">
              <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                Page {page + 1} of {Math.max(1, totalPages)}
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={page <= 0 || loading}
                  onClick={() => fetchTransactions(page - 1)}
                  style={{ padding: '6px 12px', fontSize: '13px' }}
                >
                  <ChevronLeft size={16} /> Previous
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={page >= totalPages - 1 || loading}
                  onClick={() => fetchTransactions(page + 1)}
                  style={{ padding: '6px 12px', fontSize: '13px' }}
                >
                  Next <ChevronRight size={16} />
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

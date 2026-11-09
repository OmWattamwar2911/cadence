import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/axios';
import { formatPaise } from '../utils/money';
import AuditReceiptModal from '../components/AuditReceiptModal';
import {
  History as HistoryIcon,
  Filter,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  PlusCircle,
  Download,
  FileSpreadsheet,
  ShieldCheck,
  Loader2,
} from 'lucide-react';

export default function HistoryPage() {
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedType, setSelectedType] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState('');

  // Receipt modal state
  const [selectedReceiptRef, setSelectedReceiptRef] = useState(null);

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

  const handleExportCsv = async () => {
    setExporting(true);
    setError('');
    try {
      const res = await apiClient.get('/transactions/export', {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `statement-${new Date().toISOString().slice(0, 10)}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to export statement:', err);
      setError('Failed to download certified financial statement. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="container" style={{ paddingTop: '32px', paddingBottom: '64px' }}>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h1 style={{ fontSize: '32px', color: 'var(--charcoal)', marginBottom: '4px' }}>
              TRANSACTION HISTORY
            </h1>
            <p style={{ color: '#64748b', fontSize: '14px' }}>
              {totalElements} total entries • Double-entry immutable audit ledger
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
            <button
              type="button"
              onClick={handleExportCsv}
              disabled={exporting}
              className="btn"
              style={{
                fontSize: '13px',
                padding: '8px 16px',
                background: 'var(--charcoal)',
                color: 'var(--yellow)',
                border: '1px solid var(--charcoal)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              {exporting ? (
                <Loader2 size={15} className="animate-spin" />
              ) : (
                <FileSpreadsheet size={15} />
              )}
              {exporting ? 'GENERATING CSV...' : 'EXPORT STATEMENT (CSV)'}
            </button>

            {/* Type Filter Buttons */}
            <div style={{ display: 'flex', gap: '6px' }}>
              {['', 'DEPOSIT', 'DEBIT', 'CREDIT'].map((t) => (
                <button
                  key={t}
                  type="button"
                  className={`btn ${selectedType === t ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ padding: '8px 14px', fontSize: '13px' }}
                  onClick={() => setSelectedType(t)}
                >
                  {t || 'ALL'}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Date Filter Bar */}
        <form
          onSubmit={handleFilterSubmit}
          style={{
            display: 'flex',
            gap: '12px',
            marginBottom: '24px',
            flexWrap: 'wrap',
            alignItems: 'center',
            background: '#f8fafc',
            padding: '16px',
            borderRadius: 'var(--radius-input)',
            border: '1px solid var(--border-light)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={16} color="var(--charcoal)" />
            <span style={{ fontSize: '13px', fontWeight: 700, textTransform: 'uppercase' }}>DATE FILTER:</span>
          </div>
          <input
            type="date"
            className="form-control"
            style={{ width: 'auto', padding: '8px 12px', fontSize: '13px' }}
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            aria-label="From Date"
          />
          <span style={{ color: '#64748b', fontSize: '13px' }}>TO</span>
          <input
            type="date"
            className="form-control"
            style={{ width: 'auto', padding: '8px 12px', fontSize: '13px' }}
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            aria-label="To Date"
          />
          <button type="submit" className="btn btn-secondary" style={{ padding: '8px 16px', fontSize: '13px' }}>
            APPLY
          </button>
          {(fromDate || toDate) && (
            <button
              type="button"
              onClick={handleResetFilter}
              className="btn"
              style={{ padding: '8px 12px', fontSize: '13px', background: 'transparent', color: '#64748b' }}
            >
              CLEAR
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
          <div style={{ textAlign: 'center', padding: '60px 0', color: '#64748b' }}>
            <p style={{ fontSize: '15px' }}>Loading transactions from ledger...</p>
          </div>
        ) : transactions.length === 0 ? (
          <div className="empty-state">
            <p style={{ fontSize: '16px', color: '#64748b', marginBottom: '20px' }}>
              No transactions match your criteria.
            </p>
            <Link to="/deposit" className="btn btn-primary" style={{ padding: '12px 28px', fontSize: '14px' }}>
              <PlusCircle size={16} /> DEPOSIT FUNDS NOW
            </Link>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>TYPE</th>
                  <th>REFERENCE</th>
                  <th>AMOUNT</th>
                  <th>BALANCE AFTER</th>
                  <th>DATE & TIME</th>
                  <th>PROOF</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => {
                  const isPositive = tx.type === 'DEPOSIT' || tx.type === 'CREDIT';
                  return (
                    <tr key={tx.id}>
                      <td style={{ color: '#64748b', fontWeight: 600 }}>#{tx.id}</td>
                      <td>
                        <span className={`badge badge-${tx.type.toLowerCase()}`}>
                          {tx.type}
                        </span>
                      </td>
                      <td style={{ color: '#334155' }}>{tx.reference || '—'}</td>
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
                      <td>
                        {tx.reference && tx.reference.startsWith('TRF-') ? (
                          <button
                            type="button"
                            onClick={() => setSelectedReceiptRef(tx.reference)}
                            className="btn btn-secondary"
                            style={{
                              padding: '4px 10px',
                              fontSize: '11px',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                            }}
                            title="View dual-entry cryptographic SHA-256 seal"
                          >
                            <ShieldCheck size={13} color="var(--success)" /> RECEIPT
                          </button>
                        ) : (
                          <span style={{ color: '#94a3b8', fontSize: '12px' }}>—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {/* Pill-Style Pagination Controls */}
            <div className="pagination-container">
              <span style={{ fontSize: '13px', color: '#64748b', fontWeight: 500 }}>
                Page {page + 1} of {Math.max(1, totalPages)} ({totalElements} total entries)
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  className="pill-pagination-btn"
                  disabled={page <= 0 || loading}
                  onClick={() => fetchTransactions(page - 1)}
                  aria-label="Previous Page"
                >
                  <ChevronLeft size={14} style={{ display: 'inline', verticalAlign: 'middle' }} /> PREVIOUS
                </button>
                <button
                  type="button"
                  className="pill-pagination-btn"
                  disabled={page >= totalPages - 1 || loading}
                  onClick={() => fetchTransactions(page + 1)}
                  aria-label="Next Page"
                >
                  NEXT <ChevronRight size={14} style={{ display: 'inline', verticalAlign: 'middle' }} />
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Cryptographic Receipt Modal */}
      <AuditReceiptModal
        reference={selectedReceiptRef}
        isOpen={!!selectedReceiptRef}
        onClose={() => setSelectedReceiptRef(null)}
      />
    </div>
  );
}

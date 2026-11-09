import React, { useEffect, useState } from 'react';
import apiClient from '../api/axios';
import { formatPaise } from '../utils/money';
import { ShieldCheck, ShieldAlert, X, Copy, Check, RefreshCw, Layers } from 'lucide-react';

export default function LedgerAuditModal({ isOpen, onClose }) {
  const [loading, setLoading] = useState(true);
  const [auditData, setAuditData] = useState(null);
  const [error, setError] = useState('');
  const [copiedHash, setCopiedHash] = useState(false);

  const fetchAudit = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await apiClient.get('/ledger/audit');
      setAuditData(res.data);
    } catch (err) {
      console.error('Audit verification failed:', err);
      setError('Failed to complete cryptographic ledger audit.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchAudit();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleCopyHeadHash = () => {
    if (auditData?.headHash) {
      navigator.clipboard.writeText(auditData.headHash);
      setCopiedHash(true);
      setTimeout(() => setCopiedHash(false), 2000);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        backgroundColor: 'rgba(23, 30, 25, 0.75)',
        backdropFilter: 'blur(4px)',
        zIndex: 10000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '780px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          padding: '28px 32px',
          borderRadius: 'var(--radius-card)',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.35)',
          background: '#ffffff',
          position: 'relative',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-light)', paddingBottom: '16px', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div
              style={{
                width: '40px',
                height: '40px',
                borderRadius: '50%',
                background: auditData?.tamperFree ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                color: auditData?.tamperFree ? 'var(--success)' : 'var(--danger)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {auditData?.tamperFree ? <ShieldCheck size={24} /> : <ShieldAlert size={24} />}
            </div>
            <div>
              <h2 style={{ fontSize: '22px', margin: 0, color: 'var(--charcoal)' }}>
                CRYPTOGRAPHIC AUDIT
              </h2>
              <span style={{ fontSize: '12px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Merkle Hash-Chain Verification
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: '#64748b',
              padding: '6px',
              borderRadius: '50%',
            }}
            aria-label="Close modal"
          >
            <X size={20} />
          </button>
        </div>

        {/* Content Body */}
        <div style={{ overflowY: 'auto', flex: 1, paddingRight: '4px' }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: '60px 0', color: '#64748b' }}>
              <RefreshCw size={28} className="animate-spin" style={{ margin: '0 auto 12px auto' }} />
              <p style={{ fontSize: '14px', fontWeight: 600 }}>Computing SHA-256 integrity proofs across ledger entries...</p>
            </div>
          ) : error ? (
            <div className="alert alert-danger">
              <ShieldAlert size={18} />
              <span>{error}</span>
            </div>
          ) : auditData && (
            <div>
              {/* Verdict Banner */}
              <div
                style={{
                  background: auditData.tamperFree ? 'rgba(16, 185, 129, 0.08)' : 'rgba(239, 68, 68, 0.08)',
                  border: `1.5px solid ${auditData.tamperFree ? 'var(--success)' : 'var(--danger)'}`,
                  borderRadius: 'var(--radius-input)',
                  padding: '16px 20px',
                  marginBottom: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '12px',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span
                      style={{
                        fontFamily: 'var(--font-headline)',
                        fontSize: '18px',
                        color: auditData.tamperFree ? '#065f46' : '#991b1b',
                        letterSpacing: '0.05em',
                      }}
                    >
                      {auditData.tamperFree ? '100% TAMPER-FREE & CRYPTOGRAPHICALLY VERIFIED' : 'INTEGRITY BREACH DETECTED'}
                    </span>
                  </div>
                  <p style={{ fontSize: '13px', color: '#475569', margin: '4px 0 0' }}>
                    All {auditData.totalLedgerEntries} historical double-entry rows match deterministic SHA-256 digests.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={fetchAudit}
                  className="btn btn-secondary"
                  style={{ fontSize: '12px', padding: '6px 12px' }}
                >
                  <RefreshCw size={13} /> Re-verify
                </button>
              </div>

              {/* Head Hash Block */}
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid var(--border-light)',
                  borderRadius: 'var(--radius-input)',
                  padding: '14px 18px',
                  marginBottom: '20px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <span style={{ fontSize: '11px', fontWeight: 700, letterSpacing: '0.08em', color: '#64748b' }}>
                    CURRENT MERKLE HEAD DIGEST (SHA-256)
                  </span>
                  <button
                    onClick={handleCopyHeadHash}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: copiedHash ? 'var(--success)' : '#475569',
                      cursor: 'pointer',
                      fontSize: '12px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px',
                      fontWeight: 600,
                    }}
                  >
                    {copiedHash ? <Check size={13} /> : <Copy size={13} />}
                    {copiedHash ? 'COPIED' : 'COPY HASH'}
                  </button>
                </div>
                <code
                  style={{
                    fontFamily: 'monospace',
                    fontSize: '12px',
                    wordBreak: 'break-all',
                    color: 'var(--charcoal)',
                    background: '#ffffff',
                    padding: '6px 10px',
                    display: 'block',
                    borderRadius: '4px',
                    border: '1px solid #e2e8f0',
                  }}
                >
                  {auditData.headHash}
                </code>
              </div>

              {/* Hash Chain Timeline */}
              <div style={{ marginBottom: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                  <Layers size={16} color="var(--charcoal)" />
                  <span style={{ fontSize: '13px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    CRYPTOGRAPHIC BLOCK SEQUENCE (LATEST ENTRIES)
                  </span>
                </div>

                {auditData.entries.length === 0 ? (
                  <p style={{ color: '#64748b', fontSize: '13px' }}>Ledger is in genesis state with zero entries.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {auditData.entries.slice(-5).reverse().map((entry, idx) => (
                      <div
                        key={entry.id || idx}
                        style={{
                          background: '#ffffff',
                          border: '1px solid var(--border-light)',
                          borderRadius: '8px',
                          padding: '12px 14px',
                          fontSize: '12px',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                          <span style={{ fontWeight: 700, color: 'var(--charcoal)' }}>
                            Entry #{entry.id} • <span className={`badge badge-${entry.type.toLowerCase()}`}>{entry.type}</span>
                          </span>
                          <span style={{ fontWeight: 700, color: entry.type === 'DEPOSIT' || entry.type === 'CREDIT' ? '#065f46' : '#991b1b' }}>
                            {formatPaise(entry.amount)} (Bal: {formatPaise(entry.balanceAfter)})
                          </span>
                        </div>
                        <div style={{ fontFamily: 'monospace', fontSize: '11px', color: '#64748b', lineHeight: 1.5 }}>
                          <div><strong style={{ color: '#475569' }}>Prev:</strong> {entry.prevHash ? entry.prevHash.slice(0, 18) + '...' : 'GENESIS'}</div>
                          <div><strong style={{ color: '#475569' }}>Hash:</strong> {entry.entryHash ? entry.entryHash.slice(0, 26) + '...' : '—'}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Cryptographic Guarantee Info */}
              <div
                style={{
                  background: '#f1f5f9',
                  borderRadius: 'var(--radius-input)',
                  padding: '12px 16px',
                  fontSize: '12px',
                  color: '#475569',
                  lineHeight: 1.5,
                }}
              >
                <strong>Mathematical Proof:</strong> Every ledger entry calculates <code>SHA-256(prevHash:walletId:type:amount:balanceAfter)</code>. Because each block seals the previous block’s digest, retroactively altering or deleting any historical record breaks all downstream cryptographic signatures.
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

import React, { useEffect, useState } from 'react';
import apiClient from '../api/axios';
import { formatPaise } from '../utils/money';
import { ShieldCheck, ShieldAlert, X, Copy, Check, FileCheck } from 'lucide-react';

export default function AuditReceiptModal({ reference, isOpen, onClose }) {
  const [loading, setLoading] = useState(true);
  const [receipt, setReceipt] = useState(null);
  const [error, setError] = useState('');
  const [copiedKey, setCopiedKey] = useState(null);

  useEffect(() => {
    if (isOpen && reference) {
      const fetchReceipt = async () => {
        setLoading(true);
        setError('');
        try {
          const res = await apiClient.get(`/ledger/receipts/${encodeURIComponent(reference)}`);
          setReceipt(res.data);
        } catch (err) {
          console.error('Failed to fetch cryptographic receipt:', err);
          setError(
            err.response?.data?.message || 'Receipt could not be verified or is not an inter-wallet transfer.'
          );
        } finally {
          setLoading(false);
        }
      };
      fetchReceipt();
    }
  }, [isOpen, reference]);

  if (!isOpen) return null;

  const copyToClipboard = (text, key) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
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
          maxWidth: '640px',
          padding: '28px 32px',
          borderRadius: 'var(--radius-card)',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.35)',
          background: '#ffffff',
          position: 'relative',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-light)', paddingBottom: '16px', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <FileCheck size={24} color="var(--charcoal)" />
            <h2 style={{ fontSize: '20px', margin: 0, color: 'var(--charcoal)' }}>
              CRYPTOGRAPHIC TRANSFER RECEIPT
            </h2>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: '#64748b',
              padding: '6px',
            }}
            aria-label="Close receipt"
          >
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#64748b' }}>
            <p style={{ fontSize: '14px' }}>Verifying dual-entry cryptographic hashes...</p>
          </div>
        ) : error ? (
          <div className="alert alert-danger" style={{ marginBottom: '16px' }}>
            <ShieldAlert size={18} />
            <span>{error}</span>
          </div>
        ) : receipt && (
          <div>
            <div
              style={{
                background: 'rgba(16, 185, 129, 0.08)',
                border: '1.5px solid var(--success)',
                borderRadius: 'var(--radius-input)',
                padding: '12px 16px',
                marginBottom: '20px',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
              }}
            >
              <ShieldCheck size={20} color="var(--success)" />
              <span style={{ fontSize: '13px', fontWeight: 700, color: '#065f46' }}>
                AUDIT VERIFIED: DUAL-ENTRY IMMUTABLE LEDGER SEAL
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
              <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-light)' }}>
                <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 700, textTransform: 'uppercase' }}>REFERENCE</span>
                <p style={{ margin: '4px 0 0', fontWeight: 700, fontSize: '14px', wordBreak: 'break-all' }}>{receipt.reference}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-light)' }}>
                <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 700, textTransform: 'uppercase' }}>AMOUNT TRANSFERRED</span>
                <p style={{ margin: '4px 0 0', fontWeight: 700, fontSize: '16px', color: '#065f46' }}>{formatPaise(receipt.amount)}</p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
              <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-light)' }}>
                <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 700, textTransform: 'uppercase' }}>SENDER WALLET</span>
                <p style={{ margin: '4px 0 0', fontWeight: 600, fontSize: '13px' }}>Wallet #{receipt.senderWalletId}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-light)' }}>
                <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 700, textTransform: 'uppercase' }}>RECIPIENT WALLET</span>
                <p style={{ margin: '4px 0 0', fontWeight: 600, fontSize: '13px' }}>Wallet #{receipt.recipientWalletId}</p>
              </div>
            </div>

            {/* Cryptographic Hashes */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '20px' }}>
              <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <span style={{ fontSize: '11px', fontWeight: 700, color: '#475569' }}>SENDER LEDGER ENTRY HASH (DEBIT)</span>
                  <button
                    onClick={() => copyToClipboard(receipt.senderEntryHash, 'sender')}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '11px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    {copiedKey === 'sender' ? <Check size={12} /> : <Copy size={12} />}
                    {copiedKey === 'sender' ? 'COPIED' : 'COPY'}
                  </button>
                </div>
                <code style={{ fontSize: '11px', fontFamily: 'monospace', wordBreak: 'break-all', color: 'var(--charcoal)' }}>
                  {receipt.senderEntryHash || '—'}
                </code>
              </div>

              <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <span style={{ fontSize: '11px', fontWeight: 700, color: '#475569' }}>RECIPIENT LEDGER ENTRY HASH (CREDIT)</span>
                  <button
                    onClick={() => copyToClipboard(receipt.recipientEntryHash, 'recipient')}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '11px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    {copiedKey === 'recipient' ? <Check size={12} /> : <Copy size={12} />}
                    {copiedKey === 'recipient' ? 'COPIED' : 'COPY'}
                  </button>
                </div>
                <code style={{ fontSize: '11px', fontFamily: 'monospace', wordBreak: 'break-all', color: 'var(--charcoal)' }}>
                  {receipt.recipientEntryHash || '—'}
                </code>
              </div>
            </div>

            <div style={{ fontSize: '12px', color: '#64748b', textAlign: 'center' }}>
              Sealed at: {receipt.timestamp ? new Date(receipt.timestamp).toLocaleString() : '—'}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

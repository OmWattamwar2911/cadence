import React from 'react';
import { Wallet, Send, History, MousePointer, AlignLeft, AlignCenter, AlignRight, Check } from 'lucide-react';
import { APP_NAME } from '../config/constants';

export default function AbstractMockup() {
  return (
    <div
      style={{
        position: 'relative',
        maxWidth: '1000px',
        margin: '0 auto',
        borderRadius: '16px 16px 0 0',
        border: '1px solid rgba(23, 30, 25, 0.10)',
        borderBottom: 'none',
        background: '#ffffff',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.15)',
        overflow: 'hidden',
      }}
      aria-label="Abstract Dashboard Preview"
    >
      {/* Browser Chrome Header */}
      <div
        style={{
          background: '#f8fafc',
          borderBottom: '1px solid rgba(23, 30, 25, 0.08)',
          padding: '12px 18px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span style={{ width: '11px', height: '11px', borderRadius: '50%', background: '#ff5f56', display: 'inline-block' }} />
          <span style={{ width: '11px', height: '11px', borderRadius: '50%', background: '#ffbd2e', display: 'inline-block' }} />
          <span style={{ width: '11px', height: '11px', borderRadius: '50%', background: '#27c93f', display: 'inline-block' }} />
        </div>
        <div style={{ fontSize: '12px', fontWeight: 500, color: '#64748b' }}>
          {APP_NAME} - Dashboard
        </div>
        <div style={{ width: '48px' }} />
      </div>

      {/* 3-Part Body Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          background: '#f1f5f9',
          minHeight: '360px',
          position: 'relative',
        }}
      >
        {/* 1. Left Sidebar */}
        <div
          style={{
            background: '#ffffff',
            borderRight: '1px solid rgba(23, 30, 25, 0.08)',
            padding: '24px 18px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
          }}
        >
          <div style={{ fontSize: '11px', fontWeight: 700, letterSpacing: '0.08em', color: '#94a3b8', textTransform: 'uppercase', marginBottom: '8px' }}>
            Menu
          </div>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '10px 14px',
              borderRadius: '8px',
              background: '#ffe17c',
              color: '#171e19',
              fontWeight: 600,
              fontSize: '13px',
            }}
          >
            <Wallet size={16} /> Wallet
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 14px', color: '#64748b', fontSize: '13px', fontWeight: 500 }}>
            <Send size={16} /> Send
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 14px', color: '#64748b', fontSize: '13px', fontWeight: 500 }}>
            <History size={16} /> History
          </div>
        </div>

        {/* 2. Main Canvas */}
        <div
          style={{
            gridColumn: 'span 2',
            padding: '28px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            position: 'relative',
          }}
        >
          {/* Centered Sample Balance Card */}
          <div
            style={{
              background: '#ffffff',
              borderRadius: '12px',
              padding: '20px 24px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
              border: '1px solid rgba(23, 30, 25, 0.08)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '11px', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>Sample Balance</span>
              <span style={{ fontSize: '10px', background: '#ffe17c', color: '#171e19', padding: '2px 8px', borderRadius: '4px', fontWeight: 700 }}>SAMPLE DATA</span>
            </div>
            <div style={{ fontFamily: 'var(--font-headline)', fontSize: '32px', color: '#171e19', margin: '8px 0 4px 0' }}>
              ₹12,450.00
            </div>
            <span style={{ fontSize: '11px', color: '#94a3b8' }}>Wallet ID #10492 • INR</span>
          </div>

          {/* Two Sample Transactions */}
          <div
            style={{
              background: '#ffffff',
              borderRadius: '12px',
              padding: '16px 20px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
              border: '1px solid rgba(23, 30, 25, 0.08)',
            }}
          >
            <div style={{ fontSize: '11px', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', marginBottom: '12px' }}>
              Recent Transfers (Sample)
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f1f5f9', fontSize: '12px' }}>
              <div>
                <strong style={{ display: 'block', color: '#171e19' }}>P2P Transfer to rahul@upi</strong>
                <span style={{ fontSize: '11px', color: '#94a3b8' }}>DEP-9418 • Idempotent</span>
              </div>
              <span style={{ fontWeight: 700, color: '#ef4444' }}>-₹1,500.00</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', fontSize: '12px' }}>
              <div>
                <strong style={{ display: 'block', color: '#171e19' }}>Instant Wallet Deposit</strong>
                <span style={{ fontSize: '11px', color: '#94a3b8' }}>DEP-1204 • Verified</span>
              </div>
              <span style={{ fontWeight: 700, color: '#10b981' }}>+₹5,000.00</span>
            </div>
          </div>

          {/* Floating Cursor with Name Tag */}
          <div
            style={{
              position: 'absolute',
              right: '40px',
              bottom: '30px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              zIndex: 10,
              filter: 'drop-shadow(0 4px 6px rgba(0,0,0,0.15))',
            }}
          >
            <MousePointer size={18} fill="#171e19" color="#171e19" style={{ transform: 'rotate(-45deg)' }} />
            <span
              style={{
                background: '#171e19',
                color: '#ffe17c',
                fontSize: '11px',
                fontWeight: 700,
                padding: '3px 8px',
                borderRadius: '4px',
                letterSpacing: '0.04em',
              }}
            >
              alex@payflow
            </span>
          </div>
        </div>

        {/* 3. Right Properties Panel */}
        <div
          style={{
            background: '#ffffff',
            borderLeft: '1px solid rgba(23, 30, 25, 0.08)',
            padding: '24px 18px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
          }}
        >
          <div>
            <div style={{ fontFamily: 'var(--font-headline)', fontSize: '13px', color: '#171e19', marginBottom: '8px', letterSpacing: '0.05em' }}>
              PROPERTIES
            </div>
            <div style={{ display: 'flex', gap: '8px', background: '#f8fafc', padding: '6px', borderRadius: '6px' }}>
              <button type="button" aria-label="Align Left" style={{ flex: 1, padding: '4px', border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', justifyContent: 'center' }}>
                <AlignLeft size={14} color="#64748b" />
              </button>
              <button type="button" aria-label="Align Center" style={{ flex: 1, padding: '4px', border: 'none', background: '#ffffff', borderRadius: '4px', cursor: 'pointer', display: 'flex', justifyContent: 'center', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
                <AlignCenter size={14} color="#171e19" />
              </button>
              <button type="button" aria-label="Align Right" style={{ flex: 1, padding: '4px', border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', justifyContent: 'center' }}>
                <AlignRight size={14} color="#64748b" />
              </button>
            </div>
          </div>

          <div>
            <div style={{ fontFamily: 'var(--font-headline)', fontSize: '13px', color: '#171e19', marginBottom: '8px', letterSpacing: '0.05em' }}>
              ACCENT PALETTE
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '6px',
                  background: '#ffe17c',
                  border: '1px solid rgba(0,0,0,0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Check size={16} color="#171e19" />
              </div>
              <div>
                <span style={{ fontSize: '12px', fontWeight: 600, color: '#171e19', display: 'block' }}>#FFE17C</span>
                <span style={{ fontSize: '10px', color: '#94a3b8' }}>Primary Highlight</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

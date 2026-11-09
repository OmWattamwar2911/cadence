import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Highlight from '../components/Highlight';
import AbstractMockup from '../components/AbstractMockup';
import { APP_NAME, REPO_URL } from '../config/constants';
import {
  XCircle,
  CheckCircle2,
  ShieldCheck,
  ArrowRight,
  User,
  Briefcase,
  Users,
  Github,
  Zap,
  Lock,
  FileText,
  Database
} from 'lucide-react';

export default function Landing() {
  const [heroEmail, setHeroEmail] = useState('');
  const [ctaEmail, setCtaEmail] = useState('');
  const navigate = useNavigate();

  const handleHeroSubmit = (e) => {
    e.preventDefault();
    if (heroEmail.trim()) {
      navigate(`/register?email=${encodeURIComponent(heroEmail.trim())}`);
    } else {
      navigate('/register');
    }
  };

  const handleCtaSubmit = (e) => {
    e.preventDefault();
    if (ctaEmail.trim()) {
      navigate(`/register?email=${encodeURIComponent(ctaEmail.trim())}`);
    } else {
      navigate('/register');
    }
  };

  return (
    <div style={{ position: 'relative', overflowX: 'hidden' }}>
      {/* 1. NAVBAR */}
      <header className="fixed-navbar">
        <Link to="/" className="navbar-logo">
          {APP_NAME}<span className="dot">.</span>
        </Link>
        <nav className="navbar-center-links">
          <a href="#features">Features</a>
          <a href="#how-it-works">How it Works</a>
          <a href="#security">Security</a>
        </nav>
        <div className="navbar-right-actions">
          <Link to="/login" className="login-link">
            LOGIN
          </Link>
          <Link to="/register" className="btn btn-dark-pill">
            GET STARTED
          </Link>
        </div>
      </header>

      {/* 2. HERO */}
      <section
        className="bg-grid"
        style={{
          paddingTop: '140px',
          paddingBottom: '0',
          textAlign: 'center',
          position: 'relative',
        }}
      >
        <div className="container" style={{ position: 'relative', zIndex: 2 }}>
          {/* Badge */}
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              padding: '6px 16px',
              borderRadius: 'var(--radius-pill)',
              background: '#f1f5f9',
              border: '1px solid rgba(23, 30, 25, 0.08)',
              fontSize: '12px',
              fontWeight: 700,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: 'var(--charcoal)',
              marginBottom: '24px',
            }}
          >
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--yellow)' }} />
            BUILT FOR SAFE TRANSFERS
          </div>

          {/* Headline */}
          <h1
            style={{
              fontSize: 'clamp(3.75rem, 9vw, 8.5rem)',
              lineHeight: 0.9,
              color: 'var(--charcoal)',
              maxWidth: '1100px',
              margin: '0 auto 24px auto',
            }}
          >
            MOVE MONEY WITHOUT<br />
            <Highlight variant="bar">CHAOS</Highlight>
          </h1>

          {/* Subheadline */}
          <p
            style={{
              fontSize: '18px',
              color: 'rgba(23, 30, 25, 0.70)',
              maxWidth: '36rem',
              margin: '0 auto 36px auto',
              lineHeight: 1.6,
            }}
          >
            Send, receive and track money with a ledger-backed wallet. Every transfer is atomic, idempotent and fully traceable.
          </p>

          {/* Inline Email Form */}
          <form
            onSubmit={handleHeroSubmit}
            style={{
              display: 'flex',
              justifyContent: 'center',
              gap: '12px',
              maxWidth: '520px',
              margin: '0 auto 64px auto',
              flexWrap: 'wrap',
            }}
          >
            <input
              type="email"
              placeholder="you@email.com"
              value={heroEmail}
              onChange={(e) => setHeroEmail(e.target.value)}
              className="form-control"
              style={{
                flex: '1 1 260px',
                padding: '14px 24px',
                borderRadius: 'var(--radius-input)',
                border: '1px solid rgba(23, 30, 25, 0.20)',
                background: '#ffffff',
                fontSize: '15px',
              }}
              aria-label="Email Address for Registration"
            />
            <button type="submit" className="btn btn-primary" style={{ flexShrink: 0 }}>
              GET STARTED
            </button>
          </form>

          {/* 3. ABSTRACT UI MOCKUP PEEKING */}
          <AbstractMockup />
        </div>

        {/* Soft pale-yellow glow at the bottom of hero */}
        <div
          style={{
            position: 'absolute',
            bottom: 0,
            left: '50%',
            transform: 'translateX(-50%)',
            width: '80%',
            height: '180px',
            background: 'radial-gradient(ellipse at bottom, rgba(255, 225, 124, 0.25) 0%, transparent 70%)',
            pointerEvents: 'none',
          }}
        />
      </section>

      {/* 4. PROBLEM / SOLUTION */}
      <section className="bg-charcoal" style={{ padding: '100px 0', borderTop: '1px solid var(--border-dark)' }}>
        <div className="container">
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: '48px',
              alignItems: 'start',
            }}
          >
            {/* Left Half: The Old Way */}
            <div style={{ paddingLeft: '24px', borderLeft: '2px solid rgba(183, 198, 194, 0.20)' }}>
              <h2
                style={{
                  fontSize: 'clamp(2.5rem, 5vw, 4.5rem)',
                  color: 'rgba(183, 198, 194, 0.50)',
                  marginBottom: '36px',
                }}
              >
                THE OLD WAY
              </h2>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <XCircle size={24} color="#ef4444" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <h3 style={{ fontSize: '18px', color: '#e2e8f0', marginBottom: '4px' }}>Duplicate Charges</h3>
                    <p style={{ color: 'var(--sage)', fontSize: '14px' }}>Retries that charge you twice.</p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <XCircle size={24} color="#ef4444" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <h3 style={{ fontSize: '18px', color: '#e2e8f0', marginBottom: '4px' }}>Lost Updates</h3>
                    <p style={{ color: 'var(--sage)', fontSize: '14px' }}>Balances that drift after failures.</p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <XCircle size={24} color="#ef4444" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <h3 style={{ fontSize: '18px', color: '#e2e8f0', marginBottom: '4px' }}>No Audit Trail</h3>
                    <p style={{ color: 'var(--sage)', fontSize: '14px' }}>Nobody can explain where the money went.</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Half: The Payflow Way */}
            <div
              className="card card-dark"
              style={{
                background: '#1f2723',
                borderLeft: '4px solid var(--yellow)',
                padding: '40px 32px',
              }}
            >
              <h2
                style={{
                  fontSize: 'clamp(2.5rem, 5vw, 4.5rem)',
                  color: '#ffffff',
                  marginBottom: '36px',
                }}
              >
                THE {APP_NAME} WAY
              </h2>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <CheckCircle2 size={24} color="var(--yellow)" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <h3 style={{ fontSize: '18px', color: '#ffffff', marginBottom: '4px' }}>Idempotent Transfers</h3>
                    <p style={{ color: 'var(--sage)', fontSize: '14px' }}>Retry safely, money moves once.</p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <CheckCircle2 size={24} color="var(--yellow)" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <h3 style={{ fontSize: '18px', color: '#ffffff', marginBottom: '4px' }}>Atomic Ledger</h3>
                    <p style={{ color: 'var(--sage)', fontSize: '14px' }}>Every transfer writes balanced debit and credit entries.</p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <CheckCircle2 size={24} color="var(--yellow)" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <h3 style={{ fontSize: '18px', color: '#ffffff', marginBottom: '4px' }}>Full History</h3>
                    <p style={{ color: 'var(--sage)', fontSize: '14px' }}>Searchable, paginated transaction history.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 5. FEATURES (Bento Grid) */}
      <section id="features" style={{ padding: '100px 0', background: 'var(--white)' }}>
        <div className="container">
          <div style={{ marginBottom: '48px' }}>
            <h2 style={{ fontSize: 'clamp(3rem, 6vw, 6rem)', color: 'var(--charcoal)', marginBottom: '12px' }}>
              BUILT FOR <Highlight variant="yellowText">SAFETY</Highlight>
            </h2>
            <p style={{ fontSize: '18px', color: '#64748b' }}>
              Everything you need to move money, minus the worry.
            </p>
          </div>

          {/* 3-Column Bento Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: '24px',
            }}
          >
            {/* Card 1 (spans 2 columns on wide screens) */}
            <div
              className="card"
              style={{
                gridColumn: 'span 2',
                background: '#f8f9fa',
                minHeight: '380px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                position: 'relative',
                overflow: 'hidden',
              }}
            >
              <div>
                <Zap size={28} color="var(--charcoal)" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '30px', color: 'var(--charcoal)', marginBottom: '8px' }}>
                  Instant Transfers
                </h3>
                <p style={{ color: '#64748b', fontSize: '15px', maxWidth: '28rem' }}>
                  Real-time peer-to-peer money movement with deterministic deadlock prevention.
                </p>
              </div>

              {/* Inner white panel anchored bottom-right */}
              <div
                style={{
                  alignSelf: 'flex-end',
                  background: '#ffffff',
                  padding: '20px',
                  borderRadius: '12px',
                  boxShadow: 'var(--shadow-soft)',
                  border: '1px solid rgba(23, 30, 25, 0.08)',
                  display: 'grid',
                  gridTemplateColumns: 'repeat(3, 48px)',
                  gap: '8px',
                  marginTop: '24px',
                }}
              >
                <div style={{ width: '48px', height: '48px', borderRadius: '8px', background: '#f1f5f9' }} />
                <div style={{ width: '48px', height: '48px', borderRadius: '8px', background: '#f1f5f9' }} />
                <div style={{ width: '48px', height: '48px', borderRadius: '8px', background: 'var(--yellow)' }} />
                <div style={{ width: '48px', height: '48px', borderRadius: '8px', background: '#f1f5f9' }} />
                <div style={{ width: '48px', height: '48px', borderRadius: '8px', background: '#f1f5f9' }} />
                <div style={{ width: '48px', height: '48px', borderRadius: '8px', background: '#f1f5f9' }} />
              </div>
            </div>

            {/* Card 2: Double-Entry Ledger */}
            <div
              className="card"
              style={{
                background: 'var(--charcoal)',
                color: 'var(--white)',
                minHeight: '380px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
              }}
            >
              <div>
                <Database size={28} color="var(--yellow)" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '28px', color: 'var(--white)', marginBottom: '8px' }}>
                  Double-Entry Ledger
                </h3>
                <p style={{ color: 'var(--sage)', fontSize: '14px' }}>
                  Strict financial double-entry: debit sender, credit receiver, zero balance drift.
                </p>
              </div>

              {/* Dark Code Block */}
              <div
                style={{
                  background: 'var(--dark-gray)',
                  padding: '16px',
                  borderRadius: '8px',
                  fontFamily: 'monospace',
                  fontSize: '12px',
                  lineHeight: 1.6,
                  border: '1px solid rgba(183, 198, 194, 0.15)',
                }}
              >
                <span style={{ color: '#ffe17c' }}>// Atomic ledger row</span><br />
                <span style={{ color: '#ffe17c' }}>INSERT INTO</span> <span style={{ color: '#ffffff' }}>ledger_entries</span><br />
                <span style={{ color: 'var(--sage)' }}>(transfer_id, type, amt, bal)</span><br />
                <span style={{ color: '#ffe17c' }}>VALUES</span> <span style={{ color: '#ffffff' }}>(311, 'DEBIT', 5000, 25000);</span>
              </div>
            </div>

            {/* Card 3: Idempotency Keys (Solid Yellow) */}
            <div
              className="card"
              style={{
                background: 'var(--yellow)',
                color: 'var(--charcoal)',
                minHeight: '340px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                border: 'none',
              }}
            >
              <div>
                <Lock size={28} color="var(--charcoal)" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '32px', color: 'var(--charcoal)', marginBottom: '8px' }}>
                  Idempotency Keys
                </h3>
                <p style={{ color: 'var(--charcoal)', fontSize: '15px', fontWeight: 500 }}>
                  Unique UUID headers ensure retries replay responses safely without duplicate debits.
                </p>
              </div>
              <div style={{ fontFamily: 'var(--font-headline)', fontSize: '48px', color: 'rgba(23, 30, 25, 0.20)' }}>
                UUIDv4
              </div>
            </div>

            {/* Card 4: Transaction History (spans 2 columns) */}
            <div
              className="card"
              style={{
                gridColumn: 'span 2',
                background: '#f8f9fa',
                minHeight: '340px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
              }}
            >
              <div>
                <FileText size={28} color="var(--charcoal)" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '30px', color: 'var(--charcoal)', marginBottom: '8px' }}>
                  Transaction History
                </h3>
                <p style={{ color: '#64748b', fontSize: '15px' }}>
                  Searchable, paginated audit records with exact balance timestamps.
                </p>
              </div>

              {/* Mini List UI */}
              <div style={{ background: '#ffffff', borderRadius: '8px', padding: '14px', border: '1px solid rgba(23, 30, 25, 0.08)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', padding: '6px 0', borderBottom: '1px solid #f1f5f9' }}>
                  <span>Transfer to Alice</span>
                  <strong style={{ color: '#ef4444' }}>-₹250.00</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', padding: '6px 0' }}>
                  <span>Top-up Deposit</span>
                  <strong style={{ color: '#10b981' }}>+₹1,000.00</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 6. HOW IT WORKS */}
      <section id="how-it-works" className="bg-dark-gray" style={{ padding: '100px 0' }}>
        <div className="container">
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
              gap: '64px',
              alignItems: 'start',
            }}
          >
            {/* Sticky Left Heading */}
            <div style={{ position: 'sticky', top: '120px' }}>
              <h2 style={{ fontSize: '72px', lineHeight: 0.9 }}>
                <span style={{ color: '#ffffff', display: 'block' }}>HOW IT</span>
                <span style={{ color: 'var(--sage)', display: 'block' }}>WORKS</span>
              </h2>
            </div>

            {/* Right Steps Stack */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '64px' }}>
              {/* Step 01 */}
              <div
                style={{
                  display: 'flex',
                  gap: '24px',
                  alignItems: 'baseline',
                  transition: 'opacity var(--transition-smooth)',
                }}
              >
                <span
                  style={{
                    fontFamily: 'var(--font-headline)',
                    fontSize: '8rem',
                    lineHeight: 0.8,
                    color: 'var(--yellow)',
                    opacity: 0.25,
                    transition: 'opacity 300ms ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.opacity = '1.0')}
                  onMouseLeave={(e) => (e.currentTarget.style.opacity = '0.25')}
                >
                  01
                </span>
                <div>
                  <h3 style={{ fontSize: '28px', color: '#ffffff', marginBottom: '8px' }}>
                    Create Your Wallet
                  </h3>
                  <p style={{ color: 'var(--sage)', fontSize: '16px', lineHeight: 1.6 }}>
                    Register and get a wallet instantly.
                  </p>
                </div>
              </div>

              {/* Step 02 */}
              <div
                style={{
                  display: 'flex',
                  gap: '24px',
                  alignItems: 'baseline',
                  transition: 'opacity var(--transition-smooth)',
                }}
              >
                <span
                  style={{
                    fontFamily: 'var(--font-headline)',
                    fontSize: '8rem',
                    lineHeight: 0.8,
                    color: 'var(--yellow)',
                    opacity: 0.25,
                    transition: 'opacity 300ms ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.opacity = '1.0')}
                  onMouseLeave={(e) => (e.currentTarget.style.opacity = '0.25')}
                >
                  02
                </span>
                <div>
                  <h3 style={{ fontSize: '28px', color: '#ffffff', marginBottom: '8px' }}>
                    Add Funds
                  </h3>
                  <p style={{ color: 'var(--sage)', fontSize: '16px', lineHeight: 1.6 }}>
                    Deposit a simulated top-up in seconds.
                  </p>
                </div>
              </div>

              {/* Step 03 */}
              <div
                style={{
                  display: 'flex',
                  gap: '24px',
                  alignItems: 'baseline',
                  transition: 'opacity var(--transition-smooth)',
                }}
              >
                <span
                  style={{
                    fontFamily: 'var(--font-headline)',
                    fontSize: '8rem',
                    lineHeight: 0.8,
                    color: 'var(--yellow)',
                    opacity: 0.25,
                    transition: 'opacity 300ms ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.opacity = '1.0')}
                  onMouseLeave={(e) => (e.currentTarget.style.opacity = '0.25')}
                >
                  03
                </span>
                <div>
                  <h3 style={{ fontSize: '28px', color: '#ffffff', marginBottom: '8px' }}>
                    Send and Track
                  </h3>
                  <p style={{ color: 'var(--sage)', fontSize: '16px', lineHeight: 1.6 }}>
                    Transfer to any user by email and watch it appear in your history.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 7. AUDIENCE */}
      <section style={{ padding: '100px 0', background: 'var(--white)' }}>
        <div className="container" style={{ textAlign: 'center' }}>
          <h2 style={{ fontSize: 'clamp(2.75rem, 5vw, 5rem)', color: 'var(--charcoal)', marginBottom: '56px' }}>
            BUILT FOR <Highlight variant="darkBox">EVERYONE</Highlight>
          </h2>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: '24px',
              textAlign: 'left',
            }}
          >
            {/* Card 1: Individuals */}
            <div
              className="card"
              style={{
                background: '#f8f9fa',
                borderBottom: '3px solid var(--charcoal)',
              }}
            >
              <div
                style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '10px',
                  background: 'var(--charcoal)',
                  color: 'var(--yellow)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '20px',
                }}
              >
                <User size={22} />
              </div>
              <h3 style={{ fontSize: '24px', color: 'var(--charcoal)', marginBottom: '8px' }}>
                Individuals
              </h3>
              <p style={{ color: '#64748b', fontSize: '14px', lineHeight: 1.6 }}>
                Split bills, pay friends, and track your personal digital cash flow with zero friction.
              </p>
            </div>

            {/* Card 2: Freelancers */}
            <div
              className="card"
              style={{
                background: '#f8f9fa',
                borderBottom: '3px solid var(--yellow)',
              }}
            >
              <div
                style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '10px',
                  background: 'var(--yellow)',
                  color: 'var(--charcoal)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '20px',
                }}
              >
                <Briefcase size={22} />
              </div>
              <h3 style={{ fontSize: '24px', color: 'var(--charcoal)', marginBottom: '8px' }}>
                Freelancers
              </h3>
              <p style={{ color: '#64748b', fontSize: '14px', lineHeight: 1.6 }}>
                Receive client payouts with transparent ledger receipts and instant fund availability.
              </p>
            </div>

            {/* Card 3: Small Teams */}
            <div
              className="card"
              style={{
                background: '#f8f9fa',
                borderBottom: '3px solid var(--charcoal)',
              }}
            >
              <div
                style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '10px',
                  background: 'var(--charcoal)',
                  color: 'var(--yellow)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '20px',
                }}
              >
                <Users size={22} />
              </div>
              <h3 style={{ fontSize: '24px', color: 'var(--charcoal)', marginBottom: '8px' }}>
                Small Teams
              </h3>
              <p style={{ color: '#64748b', fontSize: '14px', lineHeight: 1.6 }}>
                Disburse stipends, reimbursements, and peer rewards backed by audit-grade consistency.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 8. TRUST SECTION */}
      <section id="security" style={{ padding: '100px 0', background: '#f8fafc' }}>
        <div className="container">
          <h2
            style={{
              fontSize: 'clamp(2.75rem, 6vw, 5.5rem)',
              color: 'var(--charcoal)',
              marginBottom: '56px',
            }}
          >
            DON'T JUST TAKE OUR<br />
            <Highlight variant="outline">WORD</Highlight> FOR IT
          </h2>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
              gap: '24px',
              alignItems: 'center',
            }}
          >
            {/* Card 1: Side Light */}
            <div className="card" style={{ background: '#ffffff', minHeight: '260px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <ShieldCheck size={28} color="var(--yellow)" style={{ fill: 'var(--charcoal)', marginBottom: '16px' }} />
                <p style={{ fontSize: '18px', fontWeight: 500, color: 'var(--charcoal)', lineHeight: 1.5 }}>
                  ACID transactions with ordered pessimistic row locking eliminate deadlocks and negative balances.
                </p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginTop: '24px' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Database size={22} color="var(--charcoal)" />
                </div>
                <div>
                  <h4 style={{ fontSize: '16px', color: 'var(--charcoal)' }}>DATABASE INVARIANTS</h4>
                  <span style={{ fontSize: '12px', color: '#64748b' }}>MySQL InnoDB</span>
                </div>
              </div>
            </div>

            {/* Card 2: Center Charcoal (translateY 16px) */}
            <div
              className="card"
              style={{
                background: 'var(--charcoal)',
                color: '#ffffff',
                minHeight: '280px',
                transform: 'translateY(16px)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                boxShadow: 'var(--shadow-dark)',
              }}
            >
              <div>
                <ShieldCheck size={28} color="var(--yellow)" style={{ fill: 'var(--yellow)', color: 'var(--charcoal)', marginBottom: '16px' }} />
                <p style={{ fontSize: '18px', fontWeight: 500, color: '#ffffff', lineHeight: 1.5 }}>
                  Idempotent by design: SHA-256 request hashing ensures network retries never double-charge.
                </p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginTop: '24px' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#272727', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Lock size={22} color="var(--yellow)" />
                </div>
                <div>
                  <h4 style={{ fontSize: '16px', color: '#ffffff' }}>IDEMPOTENCY ENGINE</h4>
                  <span style={{ fontSize: '12px', color: 'var(--sage)' }}>Zero Duplicate Charges</span>
                </div>
              </div>
            </div>

            {/* Card 3: Side Light */}
            <div className="card" style={{ background: '#ffffff', minHeight: '260px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <ShieldCheck size={28} color="var(--yellow)" style={{ fill: 'var(--charcoal)', marginBottom: '16px' }} />
                <p style={{ fontSize: '18px', fontWeight: 500, color: 'var(--charcoal)', lineHeight: 1.5 }}>
                  Rate limited and monitored with token-bucket algorithms and automated velocity fraud gates.
                </p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginTop: '24px' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Zap size={22} color="var(--charcoal)" />
                </div>
                <div>
                  <h4 style={{ fontSize: '16px', color: 'var(--charcoal)' }}>RATE LIMITING</h4>
                  <span style={{ fontSize: '12px', color: '#64748b' }}>Bucket4j Protected</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 9. FINAL CTA (Solid Yellow) */}
      <section
        style={{
          background: 'var(--yellow)',
          padding: '120px 0',
          position: 'relative',
          overflow: 'hidden',
          textAlign: 'center',
        }}
      >
        {/* Huge faint PAYFLOW watermark */}
        <div
          style={{
            position: 'absolute',
            bottom: '-20px',
            right: '-10px',
            fontFamily: 'var(--font-headline)',
            fontSize: '18vw',
            color: 'rgba(23, 30, 25, 0.08)',
            lineHeight: 0.8,
            pointerEvents: 'none',
            userSelect: 'none',
          }}
        >
          {APP_NAME}
        </div>

        <div className="container" style={{ position: 'relative', zIndex: 2 }}>
          <h2
            style={{
              fontSize: 'clamp(3.5rem, 8vw, 8rem)',
              lineHeight: 0.9,
              color: 'var(--charcoal)',
              marginBottom: '20px',
            }}
          >
            READY TO MOVE MONEY?
          </h2>

          <p
            style={{
              fontSize: '20px',
              color: 'var(--charcoal)',
              maxWidth: '42rem',
              margin: '0 auto 40px auto',
              lineHeight: 1.5,
              fontWeight: 500,
            }}
          >
            Join thousands of users transferring funds safely on a production-grade double-entry financial engine.
          </p>

          <form
            onSubmit={handleCtaSubmit}
            style={{
              display: 'flex',
              justifyContent: 'center',
              gap: '12px',
              maxWidth: '520px',
              margin: '0 auto 20px auto',
              flexWrap: 'wrap',
            }}
          >
            <input
              type="email"
              placeholder="you@email.com"
              value={ctaEmail}
              onChange={(e) => setCtaEmail(e.target.value)}
              className="form-control"
              style={{
                flex: '1 1 260px',
                padding: '16px 24px',
                borderRadius: 'var(--radius-input)',
                border: 'none',
                background: '#ffffff',
                fontSize: '16px',
              }}
              aria-label="Email address for starting"
            />
            <button type="submit" className="btn btn-cta-final">
              GET STARTED
            </button>
          </form>

          <span style={{ fontSize: '12px', fontWeight: 700, letterSpacing: '0.15em', color: 'rgba(23, 30, 25, 0.70)' }}>
            NO CREDIT CARD REQUIRED
          </span>
        </div>
      </section>

      {/* 10. FOOTER */}
      <footer className="bg-charcoal" style={{ padding: '80px 0 40px 0', borderTop: '1px solid var(--border-dark)' }}>
        <div className="container">
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '40px',
              marginBottom: '60px',
            }}
          >
            {/* Column 1: Brand */}
            <div>
              <div style={{ fontFamily: 'var(--font-headline)', fontSize: '28px', color: '#ffffff', marginBottom: '12px' }}>
                {APP_NAME}<span style={{ color: 'var(--yellow)' }}>.</span>
              </div>
              <p style={{ color: 'var(--sage)', fontSize: '14px', lineHeight: 1.6 }}>
                Built for safe transfers. Double-entry ledger architecture for P2P payments.
              </p>
            </div>

            {/* Column 2: Product */}
            <div>
              <h4 style={{ fontSize: '13px', fontWeight: 700, letterSpacing: '0.12em', color: 'var(--sage)', textTransform: 'uppercase', marginBottom: '16px' }}>
                PRODUCT
              </h4>
              <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <li><a href="#features" style={{ color: '#ffffff', fontSize: '14px' }}>Features</a></li>
                <li><a href="#how-it-works" style={{ color: '#ffffff', fontSize: '14px' }}>How it Works</a></li>
                <li><a href="#security" style={{ color: '#ffffff', fontSize: '14px' }}>Security</a></li>
              </ul>
            </div>

            {/* Column 3: Company */}
            <div>
              <h4 style={{ fontSize: '13px', fontWeight: 700, letterSpacing: '0.12em', color: 'var(--sage)', textTransform: 'uppercase', marginBottom: '16px' }}>
                ACCESS
              </h4>
              <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <li><Link to="/login" style={{ color: '#ffffff', fontSize: '14px' }}>Sign In</Link></li>
                <li><Link to="/register" style={{ color: '#ffffff', fontSize: '14px' }}>Create Account</Link></li>
                <li><Link to="/dashboard" style={{ color: '#ffffff', fontSize: '14px' }}>Dashboard</Link></li>
              </ul>
            </div>

            {/* Column 4: Social */}
            <div>
              <h4 style={{ fontSize: '13px', fontWeight: 700, letterSpacing: '0.12em', color: 'var(--sage)', textTransform: 'uppercase', marginBottom: '16px' }}>
                OPEN SOURCE
              </h4>
              <div style={{ display: 'flex', gap: '12px' }}>
                <a
                  href={REPO_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="GitHub Repository"
                  style={{
                    width: '42px',
                    height: '42px',
                    borderRadius: '50%',
                    background: 'var(--dark-gray)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#ffffff',
                    transition: 'background var(--transition-smooth)',
                  }}
                >
                  <Github size={20} />
                </a>
              </div>
            </div>
          </div>

          <div style={{ borderTop: '1px solid rgba(183, 198, 194, 0.15)', paddingTop: '24px', textAlign: 'center', fontSize: '13px', color: 'var(--sage)' }}>
            © {new Date().getFullYear()} {APP_NAME}. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}

import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { APP_NAME } from '../config/constants';
import { AlertCircle, ArrowRight } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isExpired = new URLSearchParams(location.search).get('expired') === 'true';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.trim() || !password) {
      setError('Please provide both email and password.');
      return;
    }

    setLoading(true);
    try {
      await login(email.trim(), password);
      navigate('/dashboard');
    } catch (err) {
      if (err.response) {
        if (err.response.status === 429) {
          const retryAfter = err.response.headers['retry-after'] || '60';
          setError(`Rate limit exceeded. Please wait ${retryAfter} seconds before trying again.`);
        } else if (err.response.data && err.response.data.message) {
          setError(err.response.data.message);
        } else {
          setError('Invalid email or password.');
        }
      } else {
        setError('Network error: Unable to connect to server.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-grid" style={{ minHeight: 'calc(100vh - 80px)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 16px' }}>
      <div className="card" style={{ maxWidth: '440px', width: '100%', padding: '40px 32px' }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <Link to="/" style={{ fontFamily: 'var(--font-headline)', fontSize: '26px', color: 'var(--charcoal)', textDecoration: 'none', display: 'inline-block', marginBottom: '8px' }}>
            {APP_NAME}<span style={{ color: 'var(--yellow)' }}>.</span>
          </Link>
          <h1 style={{ fontSize: '32px', color: 'var(--charcoal)', marginBottom: '6px' }}>
            SIGN IN
          </h1>
          <p style={{ color: '#64748b', fontSize: '14px' }}>
            Enter your credentials to access your ledger wallet
          </p>
        </div>

        {isExpired && (
          <div className="alert alert-info">
            <AlertCircle size={18} />
            <span>Your session has expired. Please sign in again.</span>
          </div>
        )}

        {error && (
          <div className="alert alert-danger" role="alert">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="email">EMAIL ADDRESS</label>
            <input
              id="email"
              type="email"
              className="form-control"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">PASSWORD</label>
            <input
              id="password"
              type="password"
              className="form-control"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '12px' }}
            disabled={loading}
          >
            {loading ? 'AUTHENTICATING...' : 'SIGN IN'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '24px', fontSize: '14px', color: '#64748b' }}>
          New to {APP_NAME}?{' '}
          <Link to="/register" style={{ fontWeight: 700, color: 'var(--charcoal)' }}>
            Create an Account
          </Link>
        </p>
      </div>
    </div>
  );
}

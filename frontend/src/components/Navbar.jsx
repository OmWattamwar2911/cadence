import React from 'react';
import { NavLink, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { APP_NAME } from '../config/constants';
import { LogOut } from 'lucide-react';

export default function Navbar() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  if (!isAuthenticated) {
    return (
      <header className="fixed-navbar">
        <Link to="/" className="navbar-logo">
          {APP_NAME}<span className="dot">.</span>
        </Link>
        <div className="navbar-right-actions">
          <Link to="/login" className="login-link">
            LOGIN
          </Link>
          <Link to="/register" className="btn btn-dark-pill">
            GET STARTED
          </Link>
        </div>
      </header>
    );
  }

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="fixed-navbar">
      <Link to="/dashboard" className="navbar-logo">
        {APP_NAME}<span className="dot">.</span>
      </Link>

      <nav className="navbar-center-links">
        <NavLink
          to="/dashboard"
          style={({ isActive }) => ({
            fontWeight: isActive ? 700 : 500,
            borderBottom: isActive ? '2px solid var(--charcoal)' : 'none',
            paddingBottom: '4px',
          })}
        >
          DASHBOARD
        </NavLink>
        <NavLink
          to="/send"
          style={({ isActive }) => ({
            fontWeight: isActive ? 700 : 500,
            borderBottom: isActive ? '2px solid var(--charcoal)' : 'none',
            paddingBottom: '4px',
          })}
        >
          SEND MONEY
        </NavLink>
        <NavLink
          to="/deposit"
          style={({ isActive }) => ({
            fontWeight: isActive ? 700 : 500,
            borderBottom: isActive ? '2px solid var(--charcoal)' : 'none',
            paddingBottom: '4px',
          })}
        >
          DEPOSIT
        </NavLink>
        <NavLink
          to="/history"
          style={({ isActive }) => ({
            fontWeight: isActive ? 700 : 500,
            borderBottom: isActive ? '2px solid var(--charcoal)' : 'none',
            paddingBottom: '4px',
          })}
        >
          HISTORY
        </NavLink>
      </nav>

      <div className="navbar-right-actions">
        <button
          onClick={handleLogout}
          className="btn btn-dark-pill"
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          aria-label="Logout"
        >
          <LogOut size={14} /> LOGOUT
        </button>
      </div>
    </header>
  );
}

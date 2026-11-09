import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Wallet, Send, ArrowDownToLine, History, LogOut } from 'lucide-react';

export default function Navbar() {
  const { isAuthenticated, logout, user } = useAuth();
  const navigate = useNavigate();

  if (!isAuthenticated) return null;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <NavLink to="/" className="navbar-brand">
        <Wallet size={22} />
        <span>Payments Wallet</span>
      </NavLink>
      <nav className="nav-links">
        <NavLink to="/" end>Dashboard</NavLink>
        <NavLink to="/send">Send Money</NavLink>
        <NavLink to="/deposit">Deposit</NavLink>
        <NavLink to="/history">History</NavLink>
        <button
          onClick={handleLogout}
          className="btn btn-secondary"
          style={{ padding: '6px 12px', fontSize: '13px' }}
        >
          <LogOut size={16} /> Logout
        </button>
      </nav>
    </header>
  );
}

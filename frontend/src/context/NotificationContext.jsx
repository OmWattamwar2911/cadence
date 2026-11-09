import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react';
import { useAuth } from './AuthContext';
import { Bell, CheckCircle2, ArrowDownLeft, ArrowUpRight, DollarSign, X } from 'lucide-react';

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
  const { token, isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const eventSourceRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);

  const removeNotification = useCallback((id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  }, []);

  const addNotification = useCallback((notification) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 9);
    const newNotif = { id, ...notification };
    setNotifications((prev) => [newNotif, ...prev.slice(0, 4)]);

    // Auto-dismiss after 7 seconds
    setTimeout(() => {
      removeNotification(id);
    }, 7000);
  }, [removeNotification]);

  useEffect(() => {
    if (!isAuthenticated || !token) {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      return;
    }

    const apiBase = import.meta.env.VITE_API_BASE_URL || '';
    const streamUrl = `${apiBase}/api/v1/notifications/stream?token=${encodeURIComponent(token)}`;

    const connectSSE = () => {
      try {
        const es = new EventSource(streamUrl);
        eventSourceRef.current = es;

        es.addEventListener('open', () => {
          // Connected successfully
        });

        es.addEventListener('TRANSFER_RECEIVED', (e) => {
          try {
            const data = JSON.parse(e.data);
            const amtRupees = ((data.amount || 0) / 100).toFixed(2);
            addNotification({
              type: 'TRANSFER_RECEIVED',
              title: 'Funds Received!',
              message: `₹${amtRupees} received from ${data.fromEmail}${data.reference ? ` (${data.reference})` : ''}`,
            });
            window.dispatchEvent(new CustomEvent('cadence:refresh_wallet'));
          } catch (err) {
            console.error('SSE parse error:', err);
          }
        });

        es.addEventListener('TRANSFER_SENT', (e) => {
          try {
            const data = JSON.parse(e.data);
            const amtRupees = ((data.amount || 0) / 100).toFixed(2);
            addNotification({
              type: 'TRANSFER_SENT',
              title: 'Transfer Completed',
              message: `₹${amtRupees} sent to ${data.toEmail}`,
            });
            window.dispatchEvent(new CustomEvent('cadence:refresh_wallet'));
          } catch (err) {
            console.error('SSE parse error:', err);
          }
        });

        es.addEventListener('DEPOSIT_COMPLETED', (e) => {
          try {
            const data = JSON.parse(e.data);
            const amtRupees = ((data.amount || 0) / 100).toFixed(2);
            addNotification({
              type: 'DEPOSIT_COMPLETED',
              title: 'Deposit Successful',
              message: `₹${amtRupees} credited to your account`,
            });
            window.dispatchEvent(new CustomEvent('cadence:refresh_wallet'));
          } catch (err) {
            console.error('SSE parse error:', err);
          }
        });

        es.addEventListener('PAYMENT_REQUEST', (e) => {
          try {
            const data = JSON.parse(e.data);
            const amtRupees = ((data.amount || 0) / 100).toFixed(2);
            addNotification({
              type: 'PAYMENT_REQUEST',
              title: 'Payment Requested',
              message: `${data.requesterEmail} requested ₹${amtRupees}${data.reference ? ` for "${data.reference}"` : ''}`,
            });
            window.dispatchEvent(new CustomEvent('cadence:refresh_wallet'));
          } catch (err) {
            console.error('SSE parse error:', err);
          }
        });

        es.onerror = () => {
          es.close();
          eventSourceRef.current = null;
          // Reconnect with backoff
          reconnectTimeoutRef.current = setTimeout(connectSSE, 5000);
        };
      } catch (err) {
        console.error('Failed to create EventSource:', err);
      }
    };

    connectSSE();

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [token, isAuthenticated, addNotification]);

  return (
    <NotificationContext.Provider value={{ notifications, addNotification, removeNotification }}>
      {children}
      {/* Floating Real-Time Notifications Container */}
      <div
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
          maxWidth: '380px',
          width: 'calc(100vw - 48px)',
          pointerEvents: 'none',
        }}
      >
        {notifications.map((n) => (
          <div
            key={n.id}
            style={{
              pointerEvents: 'auto',
              background: 'var(--charcoal)',
              color: '#ffffff',
              border: '1px solid rgba(255, 225, 124, 0.4)',
              borderRadius: 'var(--radius-card)',
              boxShadow: '0 12px 30px rgba(0,0,0,0.3)',
              padding: '16px 18px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '12px',
              animation: 'slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
            }}
          >
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: 'rgba(255, 225, 124, 0.15)',
                color: 'var(--yellow)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                marginTop: '2px',
              }}
            >
              {n.type === 'TRANSFER_RECEIVED' ? (
                <ArrowDownLeft size={18} />
              ) : n.type === 'TRANSFER_SENT' ? (
                <ArrowUpRight size={18} />
              ) : n.type === 'DEPOSIT_COMPLETED' ? (
                <DollarSign size={18} />
              ) : (
                <Bell size={18} />
              )}
            </div>

            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                <span style={{ fontSize: '12px', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--yellow)' }}>
                  {n.title}
                </span>
                <button
                  type="button"
                  onClick={() => removeNotification(n.id)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#94a3b8',
                    cursor: 'pointer',
                    padding: '2px',
                    lineHeight: 1,
                  }}
                  aria-label="Dismiss notification"
                >
                  <X size={14} />
                </button>
              </div>
              <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#e2e8f0', lineHeight: 1.4, wordBreak: 'break-word' }}>
                {n.message}
              </p>
            </div>
          </div>
        ))}
      </div>
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};

import React, { createContext, useContext, useState, useEffect } from 'react';
import apiClient, { setAuthToken, getAuthToken } from '../api/axios';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(getAuthToken());
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const existingToken = getAuthToken();
      if (existingToken) {
        try {
          const res = await apiClient.get('/wallet');
          setUser({ id: res.data.userId, walletId: res.data.walletId });
        } catch (err) {
          console.error('Failed to restore session:', err);
          setAuthToken(null);
          setToken(null);
          setUser(null);
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email, password) => {
    const res = await apiClient.post('/auth/login', { email, password });
    const { accessToken } = res.data;
    setAuthToken(accessToken);
    setToken(accessToken);

    const walletRes = await apiClient.get('/wallet');
    setUser({ id: walletRes.data.userId, walletId: walletRes.data.walletId, email });
    return res.data;
  };

  const register = async (email, password) => {
    return await apiClient.post('/auth/register', { email, password });
  };

  const logout = () => {
    setAuthToken(null);
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        loading,
        isAuthenticated: !!token,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

import React, { createContext, useContext, useState, useEffect } from 'react';
import apiClient, { setAuthToken, getAuthToken } from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getAuthToken();
    if (token) {
      apiClient.get('/auth/me')
        .then((res) => {
          setUser(res.data);
        })
        .catch(() => {
          setAuthToken(null);
          setUser(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (username, password) => {
    const res = await apiClient.post('/auth/login', { username, password });
    const { token, role, email, userId } = res.data;
    setAuthToken(token);
    setUser({ username, role, email, userId });
    return res.data;
  };

  const logout = () => {
    setAuthToken(null);
    setUser(null);
  };

  const isAdmin = user?.role === 'ADMIN';
  const isPharmacist = user?.role === 'PHARMACIST' || isAdmin;
  const isWarehouse = user?.role === 'WAREHOUSE' || isAdmin;

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isAdmin, isPharmacist, isWarehouse }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

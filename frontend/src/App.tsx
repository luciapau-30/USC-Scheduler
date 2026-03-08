import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ScheduleProvider } from './context/ScheduleContext';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Search from './pages/Search';
import Watchlist from './pages/Watchlist';
import Schedule from './pages/Schedule';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token, ready } = useAuth();
  if (!ready) {
    return (
      <div style={{ padding: 48, textAlign: 'center', minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <p style={{ margin: 0, fontSize: 18 }}>Loading…</p>
      </div>
    );
  }
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/search" replace />} />
        <Route path="search" element={<Search />} />
        <Route path="watchlist" element={<Watchlist />} />
        <Route path="schedule" element={<Schedule />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ScheduleProvider>
        <AppRoutes />
      </ScheduleProvider>
    </AuthProvider>
  );
}

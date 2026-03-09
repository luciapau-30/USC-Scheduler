import { Outlet, NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationToast from './NotificationToast';
import './Layout.css';

export default function Layout() {
  const { logout } = useAuth();
  return (
    <div className="layout">
      <header className="layout-header">
        <NavLink to="/search" className="layout-brand">Trojan Scheduler</NavLink>
        <nav className="layout-nav">
          <NavLink to="/search" end>Search</NavLink>
          <NavLink to="/watchlist">Watchlist</NavLink>
          <NavLink to="/schedule">Schedule</NavLink>
          <button type="button" onClick={() => logout()} className="layout-logout">Log out</button>
        </nav>
      </header>
      <main className="layout-main">
        <Outlet />
      </main>
      <NotificationToast />
    </div>
  );
}

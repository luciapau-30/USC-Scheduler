import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ErrorBoundary } from './components/ErrorBoundary';
import App from './App';
import './index.css';

function showBootError(message: string) {
  const root = document.getElementById('root');
  if (root) {
    root.innerHTML = `<div style="padding:24px;max-width:560px;margin:40px auto;background:#1a2332;border:1px solid #2d3a4f;border-radius:8px;color:#e6edf3;font-family:system-ui,sans-serif"><h2 style="margin:0 0 12px;color:#f85149">Failed to start</h2><pre style="margin:0;font-size:14px;overflow:auto;white-space:pre-wrap">${message.replace(/</g, '&lt;')}</pre><p style="margin-top:16px;font-size:14px">Open the browser console (F12 → Console) for details.</p></div>`;
  }
}

const rootEl = document.getElementById('root');
if (!rootEl) {
  showBootError('Root element #root not found');
  throw new Error('Root element #root not found');
}

try {
  createRoot(rootEl).render(
    <StrictMode>
      <ErrorBoundary>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ErrorBoundary>
    </StrictMode>
  );
} catch (err) {
  const msg = err instanceof Error ? err.message : String(err);
  showBootError(msg);
  console.error(err);
}

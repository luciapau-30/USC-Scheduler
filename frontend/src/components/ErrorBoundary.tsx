import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError && this.state.error) {
      return (
        <div style={{
          padding: 24,
          maxWidth: 600,
          margin: '40px auto',
          background: 'var(--surface, #1a2332)',
          border: '1px solid var(--border, #2d3a4f)',
          borderRadius: 8,
          color: 'var(--text, #e6edf3)',
          fontFamily: 'system-ui, sans-serif',
        }}>
          <h2 style={{ margin: '0 0 12px', color: 'var(--error, #f85149)' }}>Something went wrong</h2>
          <pre style={{ margin: 0, fontSize: 14, overflow: 'auto' }}>
            {this.state.error.message}
          </pre>
          <p style={{ marginTop: 16, fontSize: 14 }}>
            Check the browser console (F12 → Console) for details.
          </p>
        </div>
      );
    }
    return this.props.children;
  }
}

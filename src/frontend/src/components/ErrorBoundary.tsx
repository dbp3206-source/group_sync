import { Component, type ErrorInfo, type ReactNode } from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export default class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  }

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught React Error caught by ErrorBoundary:', error, errorInfo)
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null })
    window.location.reload()
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '60vh',
            padding: '2rem',
            textAlign: 'center',
            color: 'var(--kos-ink, #0f172a)',
          }}
        >
          <div
            style={{
              padding: '1rem',
              borderRadius: '50%',
              background: 'rgba(239, 68, 68, 0.1)',
              color: 'var(--kos-danger, #ef4444)',
              marginBottom: '1rem',
            }}
          >
            <AlertTriangle size={36} />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, margin: '0 0 0.5rem' }}>
            Đã xảy ra sự cố hiển thị
          </h2>
          <p
            style={{
              maxWidth: '500px',
              color: 'var(--kos-muted, #64748b)',
              fontSize: '0.95rem',
              margin: '0 0 1.5rem',
              lineHeight: 1.5,
            }}
          >
            {this.state.error?.message ||
              'Không thể hiển thị phần này của ứng dụng. Vui lòng thử tải lại trang hoặc kiểm tra kết nối mạng.'}
          </p>
          <button
            type="button"
            className="kos-button kos-button--primary"
            onClick={this.handleReset}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.6rem 1.25rem',
              fontWeight: 600,
            }}
          >
            <RefreshCw size={16} /> Tải lại trang
          </button>
        </div>
      )
    }

    return this.props.children
  }
}

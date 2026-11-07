import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

interface ProtectedRouteProps {
  children: React.ReactNode
  roles?: string[]
}

const ProtectedRoute = ({ children, roles }: ProtectedRouteProps) => {
  const { user, isAuthenticated, isLoading, sessionError } = useAuth()

  const location = useLocation()
  if (isLoading) return <p className="p-8">Đang khôi phục phiên…</p>
  if (sessionError && !isAuthenticated) return <p className="p-8">Cần xác minh phiên đăng nhập trước khi mở trang này.</p>

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (roles && user && !roles.some(role => user.roles.includes(role))) {
    return <Navigate to="/unauthorized" replace />
  }

  return <>{children}</>
}

export default ProtectedRoute

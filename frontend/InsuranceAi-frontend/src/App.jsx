import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ThemeProvider } from './context/ThemeProvider'
import { AuthProvider } from './context/AuthProvider'
import { RequireAuth } from './routes/RequireAuth'
import { RedirectIfAuthenticated } from './routes/RedirectIfAuthenticated'
import { AppLayout } from './layouts/AppLayout'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { DashboardPage } from './pages/DashboardPage'
import { PoliciesPage } from './pages/PoliciesPage'
import { ClaimsPage } from './pages/ClaimsPage'
import { RenewalsPage } from './pages/RenewalsPage'
import { ChatPage } from './pages/ChatPage'

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<RedirectIfAuthenticated />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
            </Route>

            <Route element={<RequireAuth />}>
              <Route element={<AppLayout />}>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/policies" element={<PoliciesPage />} />
                <Route path="/claims" element={<ClaimsPage />} />
                <Route path="/renewals" element={<RenewalsPage />} />
                <Route path="/assistant" element={<ChatPage />} />
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}

export default App

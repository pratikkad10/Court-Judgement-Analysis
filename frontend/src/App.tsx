import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { Layout } from "./components/Layout";
import { AuthPage } from "./pages/AuthPage";
import { DocumentLibrary } from "./features/documents/DocumentLibrary";
import { ResearchChat } from "./features/chat/ResearchChat";
import { AnalysisPage } from "./features/analysis/AnalysisPage";
import { HistoryPage } from "./features/history/HistoryPage";
import { AdminDashboard, Dashboard } from "./features/dashboard/Dashboard";
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />
      <Route path="/register" element={<AuthPage register />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<DocumentLibrary />} />
          <Route path="/chat" element={<ResearchChat />} />
          <Route path="/summary" element={<AnalysisPage type="summary" />} />
          <Route
            path="/arguments"
            element={<AnalysisPage type="arguments" />}
          />
          <Route path="/compare" element={<AnalysisPage type="compare" />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route element={<ProtectedRoute role="ADMIN" />}>
            <Route path="/admin" element={<AdminDashboard />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

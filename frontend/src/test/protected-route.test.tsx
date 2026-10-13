import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AuthProvider } from "../auth/AuthContext";
import { ProtectedRoute } from "../components/ProtectedRoute";
describe("protected route", () => {
  it("redirects unauthenticated users", () => {
    render(
      <MemoryRouter initialEntries={["/private"]}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<p>Login screen</p>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/private" element={<p>Private</p>} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );
    expect(screen.getByText("Login screen")).toBeInTheDocument();
  });
});

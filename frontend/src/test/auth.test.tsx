import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AuthProvider } from "../auth/AuthContext";
import { AuthPage } from "../pages/AuthPage";
import { authApi } from "../api/judgmentApi";
vi.mock("../api/judgmentApi", () => ({
  authApi: { login: vi.fn(), register: vi.fn() },
}));
describe("login", () => {
  it("submits credentials and stores a returned session", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "a",
      refreshToken: "r",
      tokenType: "Bearer",
      expiresInSeconds: 900,
      user: { id: "1", email: "a@example.com", role: "USER", enabled: true },
    });
    render(
      <MemoryRouter>
        <AuthProvider>
          <AuthPage />
        </AuthProvider>
      </MemoryRouter>,
    );
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Email"), "a@example.com");
    await user.type(screen.getByLabelText("Password"), "long-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    expect(
      await screen.findByRole("button", { name: "Sign in" }),
    ).toBeInTheDocument();
    expect(authApi.login).toHaveBeenCalledWith(
      "a@example.com",
      "long-password",
    );
  });
});

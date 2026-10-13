import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../api/judgmentApi";
import { useAuth } from "../auth/AuthContext";
import { saveSession } from "../auth/session";
export function AuthPage({ register = false }: { register?: boolean }) {
  const navigate = useNavigate();
  const { refresh } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      saveSession(
        register
          ? await authApi.register(email, password)
          : await authApi.login(email, password),
      );
      refresh();
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to sign in.");
    }
  };
  return (
    <main className="mx-auto mt-16 max-w-md rounded bg-white p-8 shadow">
      <h1 className="text-2xl font-bold">
        {register ? "Create account" : "Sign in"}
      </h1>
      <p className="mt-1 text-slate-600">
        Research court judgments with source-grounded answers.
      </p>
      <form className="mt-5 space-y-4" onSubmit={submit}>
        <label>
          Email
          <input
            aria-label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          Password
          <input
            aria-label="Password"
            type="password"
            minLength={12}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        {error && (
          <p role="alert" className="text-red-700">
            {error}
          </p>
        )}
        <button className="w-full bg-blue-700 text-white">
          {register ? "Register" : "Sign in"}
        </button>
      </form>
      <p className="mt-4 text-sm">
        {register ? "Already registered?" : "Need an account?"}{" "}
        <Link className="text-blue-700" to={register ? "/login" : "/register"}>
          {register ? "Sign in" : "Register"}
        </Link>
      </p>
    </main>
  );
}

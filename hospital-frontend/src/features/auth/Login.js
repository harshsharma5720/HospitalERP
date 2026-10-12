import React, { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { getRoleFromToken } from "../../shared/utils/jwtUtils";
import { getErrorMessage } from "../../shared/utils/apiError";
import { Button, Field, Input, Notice } from "../../shared/ui";
import useAuthStore, { homePathForRole } from "./useAuthStore";
import AuthLayout from "./AuthLayout";
import * as authApi from "./api";

const LINK = "font-medium text-primary-ink hover:underline";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((s) => s.login);
  const expired = Boolean(new URLSearchParams(location.search).get("expired"));

  const [formData, setFormData] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const response = await authApi.login(formData.username, formData.password);
      const token = response.data.token;
      if (token) {
        login(token);
        // Back to the protected page that sent the user here, otherwise to their role's home
        navigate(location.state?.from || homePathForRole(getRoleFromToken(token)), { replace: true });
      } else {
        setError("Login successful, but token not received.");
      }
    } catch (err) {
      setError(getErrorMessage(err, "Invalid credentials. Please try again."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout
      title="Welcome back"
      description="Log in to book and follow your appointments."
      footer={
        <>
          New here?{" "}
          <Link to="/register" className={LINK}>
            Create an account
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {expired && !error && <Notice tone="warning">Your session has expired. Please log in again.</Notice>}
        <Notice tone="danger">{error}</Notice>
        <Field label="Username or email" htmlFor="login-username">
          <Input id="login-username" name="username" value={formData.username} onChange={handleChange} autoComplete="username" required />
        </Field>
        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label htmlFor="login-password" className="text-sm font-medium text-ink">
              Password
            </label>
            <Link to="/forgot-password" className={`text-sm ${LINK}`}>
              Forgot password?
            </Link>
          </div>
          <Input
            id="login-password"
            name="password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            autoComplete="current-password"
            required
          />
        </div>
        <Button type="submit" size="lg" className="w-full" disabled={busy}>
          {busy ? "Logging in..." : "Log in"}
        </Button>
      </form>
    </AuthLayout>
  );
}

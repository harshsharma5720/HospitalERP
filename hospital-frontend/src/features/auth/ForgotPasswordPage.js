import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { Button, Field, Input, Notice, useToast } from "../../shared/ui";
import AuthLayout from "./AuthLayout";
import * as authApi from "./api";

// Two steps: 1) request a code for a username/email, 2) enter the code and a new password
export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [step, setStep] = useState(1);
  const [identifier, setIdentifier] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [info, setInfo] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const requestCode = async (e) => {
    e?.preventDefault();
    setError("");
    try {
      setBusy(true);
      const res = await authApi.forgotPassword(identifier);
      setInfo(res.data.message);
      setStep(2);
    } catch (err) {
      setError(getErrorMessage(err, "Could not send the code. Please try again."));
    } finally {
      setBusy(false);
    }
  };

  const resetPassword = async (e) => {
    e.preventDefault();
    setError("");
    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }
    try {
      setBusy(true);
      await authApi.resetPassword(identifier, code, password);
      toast.success("Password changed. Please log in with your new password.");
      navigate("/login");
    } catch (err) {
      setError(getErrorMessage(err, "Could not reset the password."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout
      title="Forgot your password?"
      description={
        step === 1
          ? "Enter your username or email. We'll send a 6-digit code to the phone and email on your account."
          : "Enter the code we sent and choose a new password."
      }
      footer={
        <Link to="/login" className="font-medium text-primary-ink hover:underline">
          Back to login
        </Link>
      }
    >
      {step === 1 ? (
        <form onSubmit={requestCode} className="space-y-5">
          <Notice tone="danger">{error}</Notice>
          <Field label="Username or email" htmlFor="forgot-identifier">
            <Input id="forgot-identifier" value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="username" required />
          </Field>
          <Button type="submit" size="lg" className="w-full" disabled={busy}>
            {busy ? "Sending..." : "Send code"}
          </Button>
        </form>
      ) : (
        <form onSubmit={resetPassword} className="space-y-5">
          <Notice tone="success">{info}</Notice>
          <Notice tone="danger">{error}</Notice>
          <Field label="Reset code" htmlFor="forgot-code">
            <Input
              id="forgot-code"
              inputMode="numeric"
              pattern="[0-9]{6}"
              title="6-digit code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              autoComplete="one-time-code"
              required
            />
          </Field>
          <Field label="New password" htmlFor="forgot-password" hint="At least 6 characters.">
            <Input
              id="forgot-password"
              type="password"
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              required
            />
          </Field>
          <Field label="Confirm new password" htmlFor="forgot-confirm">
            <Input id="forgot-confirm" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" required />
          </Field>
          <Button type="submit" size="lg" className="w-full" disabled={busy}>
            {busy ? "Saving..." : "Reset password"}
          </Button>
          <Button variant="ghost" className="w-full" onClick={() => requestCode()} disabled={busy}>
            Didn't get a code? Send again
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}

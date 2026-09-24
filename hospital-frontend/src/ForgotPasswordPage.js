import React, { useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";
import { API_BASE_URL } from "./config";
import { getErrorMessage } from "./utils/apiError";

const inputClass = `
  w-full px-4 py-3 rounded-lg
  bg-white/70 text-gray-900 placeholder-gray-500
  focus:outline-none focus:ring-2 focus:ring-blue-600
  dark:bg-[#111a3b] dark:text-[#50d4f2] dark:placeholder-gray-400 dark:focus:ring-[#50d4f2]
`;

const buttonClass = `
  w-full py-3 rounded-lg font-semibold shadow-lg transition-all
  bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white
  dark:from-[#50d4f2] dark:to-[#3bc2df] dark:text-black
  disabled:opacity-60
`;

// Two steps: 1) request a code for a username/email, 2) enter the code and a new password
export default function ForgotPasswordPage() {
  const navigate = useNavigate();
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
      const res = await axios.post(`${API_BASE_URL}/api/auth/forgot-password`, { identifier });
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
      await axios.post(`${API_BASE_URL}/api/auth/reset-password`, {
        identifier,
        code,
        newPassword: password,
      });
      alert("Password changed. Please log in with your new password.");
      navigate("/login");
    } catch (err) {
      setError(getErrorMessage(err, "Could not reset the password."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      className="min-h-screen bg-cover bg-center flex items-center justify-center relative dark:bg-[#0a1330]"
      style={{ backgroundImage: "url('/background.jpeg')" }}
    >
      <div className="absolute inset-0 bg-black bg-opacity-60 dark:bg-[#0a1330]/80"></div>

      <div className="relative z-10 w-[90%] max-w-md rounded-3xl shadow-2xl border border-white/20 bg-white/10 backdrop-blur-xl p-8 text-white dark:bg-[#111a3b]/60">
        <h1 className="text-3xl font-bold mb-2 text-center">Forgot Password</h1>

        {step === 1 ? (
          <form onSubmit={requestCode} className="space-y-5 mt-6">
            <p className="text-sm text-gray-200 dark:text-[#8ddff8]">
              Enter your username or email. We'll send a 6-digit code to the phone and email on your account.
            </p>
            <input
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="Username or Email"
              aria-label="Username or email"
              className={inputClass}
              required
            />
            {error && <p className="text-red-300 font-semibold">{error}</p>}
            <button type="submit" disabled={busy} className={buttonClass}>
              {busy ? "Sending..." : "Send Code"}
            </button>
          </form>
        ) : (
          <form onSubmit={resetPassword} className="space-y-4 mt-6">
            {info && <p className="text-sm text-green-200">{info}</p>}
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]{6}"
              title="6-digit code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="6-digit code"
              aria-label="Reset code"
              className={inputClass}
              required
            />
            <input
              type="password"
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="New password (min 6 characters)"
              aria-label="New password"
              className={inputClass}
              required
            />
            <input
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              placeholder="Confirm new password"
              aria-label="Confirm new password"
              className={inputClass}
              required
            />
            {error && <p className="text-red-300 font-semibold">{error}</p>}
            <button type="submit" disabled={busy} className={buttonClass}>
              {busy ? "Saving..." : "Reset Password"}
            </button>
            <button type="button" onClick={() => requestCode()} disabled={busy}
              className="w-full text-sm text-gray-200 hover:underline">
              Didn't get a code? Send again
            </button>
          </form>
        )}

        <p className="text-center mt-6 text-sm">
          <Link to="/login" className="font-bold text-[#8ddff8] hover:underline">Back to login</Link>
        </p>
      </div>
    </div>
  );
}

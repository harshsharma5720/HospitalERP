import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { Button, Field, Input, Notice, useToast } from "../../shared/ui";
import useAuthStore from "./useAuthStore";
import AuthLayout from "./AuthLayout";
import * as authApi from "./api";

// A patient's own account. When the server asks for it (OTP_REQUIRED) the phone number is verified with a code
// first; staff accounts are made by an admin.
export default function RegisterPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const login = useAuthStore((s) => s.login);

  const [formData, setFormData] = useState({ email: "", username: "", phone: "", otp: "", password: "", confirmPassword: "" });
  const [error, setError] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [otpVerified, setOtpVerified] = useState(false);
  const [sending, setSending] = useState(false);
  const [saving, setSaving] = useState(false);
  // The server decides whether phone verification is mandatory (OTP_REQUIRED)
  const [otpRequired, setOtpRequired] = useState(true);

  useEffect(() => {
    authApi
      .isOtpRequired()
      .then((res) => setOtpRequired(res.data.otpRequired !== false))
      .catch(() => setOtpRequired(true));
  }, []);

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  // A new number must be verified again
  const changePhone = (e) => {
    setFormData({ ...formData, phone: e.target.value, otp: "" });
    setOtpSent(false);
    setOtpVerified(false);
  };

  const handleSendOtp = async () => {
    if (!formData.phone.trim()) {
      setError("Please enter your phone number first.");
      return;
    }
    setError("");
    setSending(true);
    try {
      const res = await authApi.sendOtp(formData.phone.trim());
      toast.success(res.data.message || "Code sent.");
      setOtpSent(true);
    } catch (err) {
      setError(getErrorMessage(err, "Failed to send the code. Try again."));
    } finally {
      setSending(false);
    }
  };

  const handleVerifyOtp = async () => {
    setError("");
    try {
      const res = await authApi.verifyOtp(formData.phone.trim(), formData.otp.trim());
      if (res.data.success) {
        setOtpVerified(true);
        toast.success("Phone number verified.");
      } else {
        setError("That code isn't right. Check it, or send a new one.");
      }
    } catch (err) {
      setError(getErrorMessage(err, "The code could not be verified."));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match!");
      return;
    }
    if (otpRequired && !otpVerified) {
      setError("Please verify your phone number before registering!");
      return;
    }
    setSaving(true);
    try {
      const response = await authApi.register({
        email: formData.email,
        username: formData.username.trim(),
        phoneNumber: formData.phone.trim(),
        password: formData.password,
      });
      const token = response.data.token;
      if (token) {
        // The register endpoint returns a token, so the new patient is logged in right away
        login(token);
        toast.success(`Welcome, ${formData.username.trim()}! Your account is ready.`);
        navigate("/");
      } else {
        toast.success("Account created. Please log in.");
        navigate("/login");
      }
    } catch (err) {
      setError(getErrorMessage(err, "Registration failed. Please try again."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <AuthLayout
      title="Create your account"
      description="Book appointments for yourself and your family, and keep your visits in one place."
      footer={
        <>
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-primary-ink hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <Notice tone="danger">{error}</Notice>
        <Field label="Email" htmlFor="register-email">
          <Input id="register-email" name="email" type="email" value={formData.email} onChange={handleChange} autoComplete="email" required />
        </Field>
        <Field label="Username" htmlFor="register-username" hint="You log in with it.">
          <Input id="register-username" name="username" value={formData.username} onChange={handleChange} autoComplete="username" required />
        </Field>
        <Field
          label="Phone number"
          htmlFor="register-phone"
          hint={otpRequired && !otpVerified ? "We send a code to check it." : null}
        >
          <div className="flex gap-2">
            <Input id="register-phone" name="phone" type="tel" value={formData.phone} onChange={changePhone} autoComplete="tel" required />
            {otpRequired && !otpVerified && (
              <Button variant="secondary" onClick={handleSendOtp} disabled={sending}>
                {sending ? "Sending..." : otpSent ? "Send again" : "Send code"}
              </Button>
            )}
          </div>
        </Field>
        {otpRequired && otpSent && !otpVerified && (
          <Field label="Code" htmlFor="register-otp">
            <div className="flex gap-2">
              <Input id="register-otp" name="otp" inputMode="numeric" value={formData.otp} onChange={handleChange} autoComplete="one-time-code" />
              <Button variant="secondary" onClick={handleVerifyOtp} disabled={!formData.otp.trim()}>
                Verify
              </Button>
            </div>
          </Field>
        )}
        {otpRequired && otpVerified && (
          <p className="flex items-center gap-2 text-sm font-medium text-success-ink">
            <CheckCircle2 size={16} aria-hidden="true" />
            Phone number verified
          </p>
        )}
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Password" htmlFor="register-password">
            <Input id="register-password" name="password" type="password" value={formData.password} onChange={handleChange} autoComplete="new-password" required />
          </Field>
          <Field label="Confirm password" htmlFor="register-confirm">
            <Input
              id="register-confirm"
              name="confirmPassword"
              type="password"
              value={formData.confirmPassword}
              onChange={handleChange}
              autoComplete="new-password"
              required
            />
          </Field>
        </div>
        <Button type="submit" size="lg" className="w-full" disabled={saving || (otpRequired && !otpVerified)}>
          {saving ? "Creating account..." : "Create account"}
        </Button>
      </form>
    </AuthLayout>
  );
}

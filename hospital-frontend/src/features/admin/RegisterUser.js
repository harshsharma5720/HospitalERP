import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import * as adminApi from "./api";
import { getErrorMessage } from "../../shared/utils/apiError";
import { Button, Card, Field, Input, PageHeader, Select, useToast } from "../../shared/ui";
import { ROLE_LABELS } from "./ManageUsers";

const EMPTY = { username: "", email: "", phone: "", password: "", role: "" };

// A new account of any role. Staff accounts are created through the admin API; public /api/auth/register only
// creates patients.
export default function RegisterUser() {
  const toast = useToast();
  const navigate = useNavigate();
  const [formData, setFormData] = useState(EMPTY);
  const [saving, setSaving] = useState(false);

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.role) {
      toast.error("Please select a role");
      return;
    }
    setSaving(true);
    try {
      await adminApi.createUser({
        email: formData.email,
        username: formData.username.trim(),
        phoneNumber: formData.phone,
        password: formData.password,
        role: formData.role,
      });
      toast.success(`${formData.username.trim()} registered.`);
      navigate("/admin/manage-users");
    } catch (error) {
      toast.error(getErrorMessage(error, "Registration failed!"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-xl">
      <PageHeader
        back={
          <Link to="/admin/manage-users" className="text-primary-ink hover:underline">
            ← Users
          </Link>
        }
        title="Add user"
        description="Staff accounts are made here; patients can also register themselves."
      />
      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="Role" htmlFor="new-user-role">
            <Select id="new-user-role" name="role" value={formData.role} onChange={handleChange}>
              <option value="">Select a role</option>
              {Object.entries(ROLE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Username" htmlFor="new-user-username">
            <Input id="new-user-username" name="username" value={formData.username} onChange={handleChange} required autoComplete="off" />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Email" htmlFor="new-user-email">
              <Input id="new-user-email" name="email" type="email" value={formData.email} onChange={handleChange} required />
            </Field>
            <Field label="Phone number" htmlFor="new-user-phone">
              <Input id="new-user-phone" name="phone" type="tel" value={formData.phone} onChange={handleChange} />
            </Field>
          </div>
          <Field label="Password" htmlFor="new-user-password">
            <Input id="new-user-password" name="password" type="password" value={formData.password} onChange={handleChange} required autoComplete="new-password" />
          </Field>
          <div className="flex justify-end gap-2 border-t border-line pt-4">
            <Button as={Link} to="/admin/manage-users" variant="secondary">
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Registering..." : "Register"}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Search, UserPlus } from "lucide-react";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";
import {
  Badge, Button, Card, Input, PageHeader, Select, Table, TableMessage, Td, Th, useConfirm, useToast,
} from "../../shared/ui";

export const ROLE_LABELS = { ADMIN: "Admin", DOCTOR: "Doctor", PATIENT: "Patient", RECEPTIONIST: "Receptionist" };

// Accounts are deactivated, not deleted (docs/ACCOUNT_DEACTIVATION_PLAN.md): a deactivated user can't log in
// and their history is kept. "Delete permanently" only works for accounts without any appointment.
export default function ManageUsers() {
  const toast = useToast();
  const confirm = useConfirm();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("");

  const fetchUsers = async () => {
    try {
      const response = await adminApi.getAllUsers();
      setUsers(response.data);
    } catch (error) {
      toast.error(getErrorMessage(error, "Could not load the users."));
    } finally {
      setLoading(false);
    }
  };

  const deactivate = async (user) => {
    const ok = await confirm({
      title: `Deactivate ${user.username}?`,
      message:
        "They can't log in any more and their upcoming appointments are cancelled. All history is kept, and you can reactivate the account later.",
      confirmLabel: "Deactivate",
      tone: "danger",
    });
    if (!ok) return;
    try {
      await adminApi.deactivateUser(user.id);
      toast.success(`${user.username} deactivated.`);
      fetchUsers();
    } catch (error) {
      toast.error(getErrorMessage(error, "Deactivation failed!"));
    }
  };

  const reactivate = async (user) => {
    try {
      await adminApi.reactivateUser(user.id);
      toast.success(`${user.username} reactivated: they can log in again.`);
      fetchUsers();
    } catch (error) {
      toast.error(getErrorMessage(error, "Reactivation failed!"));
    }
  };

  const deletePermanently = async (user) => {
    const ok = await confirm({
      title: `Delete ${user.username} permanently?`,
      message:
        "This can't be undone. It only works for accounts without any appointment (medical records must be kept); otherwise deactivate the account.",
      confirmLabel: "Delete permanently",
      tone: "danger",
    });
    if (!ok) return;
    try {
      await adminApi.deleteUserPermanently(user.id);
      toast.success(`${user.username} deleted permanently.`);
      fetchUsers();
    } catch (error) {
      toast.error(getErrorMessage(error, "Delete failed!"));
    }
  };

  useEffect(() => {
    fetchUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) return <p className="text-ink-muted">Loading users...</p>;

  const needle = search.trim().toLowerCase();
  const shown = users.filter((user) => (!role || user.role === role) && (!needle || user.username.toLowerCase().includes(needle)));

  return (
    <>
      <PageHeader
        title="Users"
        description="Accounts are deactivated rather than deleted, so their history is kept."
        actions={
          <Button as={Link} to="/admin/register-user">
            <UserPlus size={16} aria-hidden="true" />
            Add user
          </Button>
        }
      />

      <Card padding="sm" className="mb-4">
        <div className="flex flex-wrap gap-3">
          <div className="relative min-w-[14rem] flex-1">
            <Search size={16} aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted" />
            <Input type="search" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search by username" aria-label="Search by username" className="pl-9" />
          </div>
          <div className="w-48">
            <Select value={role} onChange={(e) => setRole(e.target.value)} aria-label="Role">
              <option value="">All roles</option>
              {Object.entries(ROLE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </div>
        </div>
      </Card>

      <Table>
        <thead>
          <tr>
            <Th>ID</Th>
            <Th>Username</Th>
            <Th>Role</Th>
            <Th>Status</Th>
            <Th align="right">Actions</Th>
          </tr>
        </thead>
        <tbody>
          {shown.map((user) => (
            <tr key={user.id} className={user.active ? "" : "text-ink-muted"}>
              <Td className="tabular-nums text-ink-muted">{user.id}</Td>
              <Td className="font-medium">{user.username}</Td>
              <Td>{ROLE_LABELS[user.role] ?? user.role}</Td>
              <Td>
                {user.active ? (
                  <Badge tone="success">Active</Badge>
                ) : (
                  <span title={user.deactivatedAt ? `since ${String(user.deactivatedAt).slice(0, 10)}` : undefined}>
                    <Badge>Deactivated</Badge>
                  </span>
                )}
              </Td>
              <Td align="right">
                <div className="flex flex-wrap justify-end gap-2">
                  {user.active ? (
                    <Button size="sm" variant="secondary" onClick={() => deactivate(user)}>
                      Deactivate
                    </Button>
                  ) : (
                    <Button size="sm" variant="secondary" onClick={() => reactivate(user)}>
                      Reactivate
                    </Button>
                  )}
                  <Button size="sm" variant="danger-ghost" onClick={() => deletePermanently(user)}>
                    Delete permanently
                  </Button>
                </div>
              </Td>
            </tr>
          ))}
          {shown.length === 0 && <TableMessage colSpan={5}>No users match.</TableMessage>}
        </tbody>
      </Table>
    </>
  );
}

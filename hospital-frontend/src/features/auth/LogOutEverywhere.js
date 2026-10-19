import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { MonitorSmartphone } from "lucide-react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { Button, Card, CardHeader, useConfirm, useToast } from "../../shared/ui";
import useAuthStore from "./useAuthStore";

// "Log out of all devices" (docs/SAFER_LOGIN_TOKENS_PLAN.md): every login of the account ends, this one too, and the
// access tokens already given out stop working at once. For a lost phone or a shared computer.

// The action: asks first; afterwards the login page says what happened
export function useLogOutEverywhere() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const toast = useToast();
  const [busy, setBusy] = useState(false);

  const logOutEverywhere = async () => {
    const ok = await confirm({
      title: "Log out of all devices?",
      message: "You'll be logged out on every phone and computer where you're logged in, this one too.",
      confirmLabel: "Log out everywhere",
      tone: "danger",
    });
    if (!ok) return;
    setBusy(true);
    try {
      await useAuthStore.getState().logoutEverywhere();
      navigate("/login?loggedOut=everywhere", { replace: true });
    } catch (err) {
      setBusy(false);
      toast.error(getErrorMessage(err, "Couldn't log you out of all devices. Please try again."));
    }
  };

  return { logOutEverywhere, busy };
}

// In the staff top bar, for admins: they have no profile page
export function LogOutEverywhereButton() {
  const { logOutEverywhere, busy } = useLogOutEverywhere();
  return (
    <Button variant="ghost" size="sm" onClick={logOutEverywhere} disabled={busy} aria-label="Log out of all devices">
      <MonitorSmartphone size={16} aria-hidden="true" />
      <span className="hidden md:inline">Log out everywhere</span>
    </Button>
  );
}

// The card on the account and profile pages
export default function LogOutEverywhereCard() {
  const { logOutEverywhere, busy } = useLogOutEverywhere();
  return (
    <Card aria-labelledby="log-out-everywhere">
      <CardHeader
        id="log-out-everywhere"
        title="Logged-in devices"
        description="Lost a phone, or logged in on a shared computer? Log out everywhere at once."
      />
      <Button variant="secondary" onClick={logOutEverywhere} disabled={busy}>
        <MonitorSmartphone size={16} aria-hidden="true" />
        Log out of all devices
      </Button>
    </Card>
  );
}

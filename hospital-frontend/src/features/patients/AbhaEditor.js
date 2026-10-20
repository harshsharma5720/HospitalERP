import React, { useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { ABHA_ERROR, abhaInputProps, formatAbha, isAbhaOrEmpty } from "../../shared/utils/abha";
import { Button, Input } from "../../shared/ui";

// One ABHA number at the front desk (docs/ABHA_NUMBER_PLAN.md): shown, and changed in place. who: whose it is, for
// the labels ("Asha Rao"). onSave(text) saves it ("" removes it) and throws with the server's reason when refused.

export default function AbhaEditor({ id, number, who, onSave }) {
  const [editing, setEditing] = useState(false);
  const [text, setText] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const start = () => {
    setText(formatAbha(number));
    setError("");
    setEditing(true);
  };

  const save = async (e) => {
    e.preventDefault();
    if (!isAbhaOrEmpty(text)) {
      setError(ABHA_ERROR);
      return;
    }
    setSaving(true);
    try {
      await onSave(text.trim());
      setEditing(false);
    } catch (err) {
      setError(getErrorMessage(err, "Could not save the ABHA number."));
    } finally {
      setSaving(false);
    }
  };

  if (!editing) {
    return (
      <span className="flex flex-wrap items-center gap-x-2">
        <span className="tabular-nums">{number ? formatAbha(number) : "—"}</span>
        <Button size="sm" variant="ghost" onClick={start} aria-label={`${number ? "Change" : "Add"} ${who}'s ABHA number`}>
          {number ? "Change" : "Add"}
        </Button>
      </span>
    );
  }

  return (
    <form onSubmit={save} noValidate className="flex max-w-xs flex-col gap-2">
      <Input
        id={id}
        aria-label={`${who}'s ABHA number`}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setError("");
        }}
        aria-invalid={error ? true : undefined}
        aria-describedby={`${id}-help`}
        autoFocus
        {...abhaInputProps}
      />
      <p id={`${id}-help`} className={`text-xs ${error ? "text-danger-ink" : "text-ink-muted"}`} role={error ? "alert" : undefined}>
        {error || "14 digits. Empty removes it."}
      </p>
      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={saving}>
          {saving ? "Saving..." : "Save"}
        </Button>
        <Button size="sm" variant="secondary" onClick={() => setEditing(false)}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

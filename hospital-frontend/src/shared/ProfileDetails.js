import React, { useEffect, useState } from "react";
import { Camera } from "lucide-react";
import { API_BASE_URL } from "../app/config";
import { Button, Card, CardHeader, Field, Input, Select } from "./ui";

// A person's own details (the patient's account, the receptionist's profile): shown as a list, edited in place,
// with the photo. fields: [{ key, label, type?, options?: [[value, label]], readOnly?, show?(value), hint?, inputProps? }].
// onSave(details, photoFile) resolves when saved (it throws to keep the form open).

const initials = (name) =>
  (name || "?")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join("");

export default function ProfileDetails({ title = "Your details", name, data, fields, onSave }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(data);
  const [photo, setPhoto] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => setDraft(data), [data]);

  const preview = photo ? URL.createObjectURL(photo) : data.profileImage ? `${API_BASE_URL}${data.profileImage}` : null;

  const cancel = () => {
    setDraft(data);
    setPhoto(null);
    setEditing(false);
  };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave(draft, photo);
      setPhoto(null);
      setEditing(false);
    } catch {
      // the caller has said why; the form stays open
    } finally {
      setSaving(false);
    }
  };

  const change = (key) => (e) => setDraft({ ...draft, [key]: e.target.value });

  return (
    <Card aria-labelledby="profile-details">
      <CardHeader
        id="profile-details"
        title={title}
        actions={
          !editing && (
            <Button size="sm" variant="secondary" onClick={() => setEditing(true)}>
              Edit
            </Button>
          )
        }
      />
      <form onSubmit={save}>
        <div className="mb-6 flex items-center gap-4">
          {preview ? (
            <img src={preview} alt="" className="h-16 w-16 rounded-full object-cover ring-4 ring-primary-ink/10" />
          ) : (
            <span aria-hidden="true" className="flex h-16 w-16 items-center justify-center rounded-full bg-primary-ink/10 text-xl font-semibold text-primary-ink">
              {initials(name)}
            </span>
          )}
          <div className="min-w-0">
            <p className="text-lg font-semibold">{name}</p>
            {editing && (
              <>
                <input
                  id="profile-photo"
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  className="sr-only"
                  onChange={(e) => setPhoto(e.target.files[0] || null)}
                />
                <label htmlFor="profile-photo" className="inline-flex cursor-pointer items-center gap-1.5 text-sm font-medium text-primary-ink hover:underline">
                  <Camera size={16} aria-hidden="true" />
                  {preview ? "Change photo" : "Add a photo"}
                </label>
              </>
            )}
          </div>
        </div>

        {editing ? (
          <div className="grid gap-4 sm:grid-cols-2">
            {fields.map(({ key, label, type = "text", options, readOnly, show, hint, inputProps }) =>
              readOnly ? (
                <div key={key}>
                  <p className="text-sm font-medium">{label}</p>
                  <p className="mt-2 text-sm text-ink-muted">{(show ? show(data[key]) : data[key]) || "—"}</p>
                </div>
              ) : options ? (
                <Field key={key} label={label} htmlFor={`profile-${key}`}>
                  <Select id={`profile-${key}`} value={draft[key] ?? ""} onChange={change(key)}>
                    <option value="">Choose</option>
                    {options.map(([value, text]) => (
                      <option key={value} value={value}>
                        {text}
                      </option>
                    ))}
                  </Select>
                </Field>
              ) : (
                <Field key={key} label={label} htmlFor={`profile-${key}`} hint={hint}>
                  <Input id={`profile-${key}`} type={type} value={draft[key] ?? ""} onChange={change(key)} {...inputProps} />
                </Field>
              )
            )}
          </div>
        ) : (
          <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2">
            {fields.map(({ key, label, options, show }) => {
              const value = data[key];
              const text = show ? show(value) : options ? options.find(([v]) => v === value)?.[1] : value;
              return (
                <div key={key}>
                  <dt className="text-sm text-ink-muted">{label}</dt>
                  <dd className="mt-0.5 font-medium">{text || text === 0 ? text : "—"}</dd>
                </div>
              );
            })}
          </dl>
        )}

        {editing && (
          <div className="mt-6 flex justify-end gap-2 border-t border-line pt-4">
            <Button variant="secondary" onClick={cancel}>
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Saving..." : "Save changes"}
            </Button>
          </div>
        )}
      </form>
    </Card>
  );
}

// The multipart request the profile endpoints expect: the details as JSON under dtoKey (without the photo's path),
// a new photo as profileImage
export const profileRequest = (dtoKey, details, photo) => {
  const { profileImage, ...dto } = details;
  const form = new FormData();
  form.append(dtoKey, new Blob([JSON.stringify(dto)], { type: "application/json" }));
  if (photo) form.append("profileImage", photo);
  return form;
};

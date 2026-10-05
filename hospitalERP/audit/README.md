# audit (`hospital-audit`)

The audit log: **who viewed or changed which patient record, and when** ([docs/AUDIT_LOG_PLAN.md](../../docs/AUDIT_LOG_PLAN.md)). Entries are append-only and kept forever: the app never changes or deletes them.

It records what other modules tell it to; it knows nothing about consultations or patients itself.

**Depends on:** common, identity · **Used by:** patients, clinical, administration (from steps A.2–A.4), app

## Public API — `com.itmonteur.hospitalerp.audit`

| Class | Purpose |
|---|---|
| `AuditLog` | `record(action, patientId, targetId, details)`: records that the logged-in user did something; `search(filter, page, size)`: entries newest first, at most 100 per page. |
| `AuditAction` | What happened, e.g. `CONSULTATION_VIEWED`, `PATIENT_PROFILE_UPDATED`, `ACCOUNT_DEACTIVATED`. Each action says what its target id points to (`APPOINTMENT`, `PATIENT`, `USER` or `NONE`). |
| `AuditFilter` | Search filters: patient, username, action, from / to day; each optional. |
| `AuditEntryDTO` | One entry as the admin sees it; `patientName` is filled in by administration. |

## How an entry is recorded

- `record` takes the actor (user id, username, role), the time and the client's IP address **at the call**.
- The entry is written **after the surrounding transaction commits** (right away without one), in its own transaction. A change that rolls back leaves no entry; read-only transactions work too.
- If the entry can't be written, the error is logged and the caller carries on: a doctor must be able to open a record even if the audit table has a problem.
- `details` is short context such as the names of changed fields — **never medical content**. It is cut to 255 characters.
- The IP address is the request's own, unless the request comes from a private or local address (the Docker nginx): then the `X-Real-IP` header that nginx sets is used. A caller from outside can't fake it.

## Events

- **Publishes:** `AuditEntryRecorded` — internal only, between `AuditLog` and its writer.
- **Listens to:** its own `AuditEntryRecorded` (`AuditEntryWriter`, after commit).

## Endpoints

None. Admins see the log through administration (`GET /api/admin/audit-log`, step A.4).

## Internal — `audit.internal`

`AuditEntry` (the `@Immutable` entity, table `audit_log`), `AuditEntryRepository` (save and search only, no update or delete methods), `AuditEntryWriter` (after-commit writer), `ClientAddress` (IP address rule).

## Database

`audit_log`, created by `db/migration/audit/V2026_10_05_1__audit_log.sql` (in `app`). No foreign keys, so entries outlive deleted accounts; indexes for patient, username and time. `action` is text, not a MySQL enum, so a new action needs no migration.

## Configuration

None.

## Tests

`ClientAddressTest` (IP rule), `AuditEntryWriterTest` (own transaction, a failed write never reaches the caller); `AuditLogH2Test` in `app` (written only after commit, read-only and no transaction, actor / time / IP, failed writes, search filters and paging); `DatabaseMigrationMySqlTest` checks the migration on MySQL.

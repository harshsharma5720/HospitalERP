# common (`hospital-common`)

The shared kernel: the few things every module needs and that belong to no business area. It is marked as a *shared module* (`@Modulithic(sharedModules = "common")`), so every module may use it.

**Depends on:** nothing · **Used by:** all modules

Keep it small. Anything with business meaning belongs in a business module.

## Public API — `com.itmonteur.hospitalerp.common`

| Class | Purpose |
|---|---|
| `BadRequestException`, `ConflictException`, `ForbiddenException`, `ResourceNotFoundException`, `TooManyRequestsException` | Throw these from any module. `GlobalExceptionHandler` turns them into HTTP 400 / 409 / 403 / 404 / 429 responses. |
| `ApiResponse` | JSON error body (`message`, `success`) returned by `GlobalExceptionHandler`. |
| `Gender` | Enum used by patients, relatives and receptionists, on appointments and on the prescription PDF. |
| `FileStorageService` | `storeProfileImage(...)`: stores JPEG/PNG/WEBP up to 2 MB under a random name in the upload folder. |

## Events

None published or consumed.

## Web — `common.web`

| Class | Purpose |
|---|---|
| `GlobalExceptionHandler` | `@RestControllerAdvice` for the whole application: maps the exceptions above, validation errors, authentication failures, upload-size errors and data-integrity errors to JSON error responses. |
| `WebConfig` | Serves uploaded files at `/uploads/**`. |

No REST endpoints of its own.

## Configuration

| Property | Env variable | Default | Meaning |
|---|---|---|---|
| `app.upload-dir` | `UPLOAD_DIR` | `uploads` | Folder for uploaded profile images (relative to the working directory, `hospitalERP/`). |

## Tests

`FileStorageServiceTest`

Module diagram: [docs/modules/module-common.puml](../../docs/modules/module-common.puml). Rules for all modules: [docs/MULTI_MODULE_PLAN.md](../../docs/MULTI_MODULE_PLAN.md).

# RepeaterBook API compliance

RepeaterBook suspended export access for the TD-H3 nicFW 2.5 Android app after reviewing the public v2.4.0 APK. This note is the correction record. It does not contain credential values.

## What was wrong

v2.4.0 copied `REPEATERBOOK_APP_TOKEN` from `local.properties` into `BuildConfig` and then into the APK. Leaving that file out of Git did not keep the value off devices.

The HTTP client could also place the credential in the query string (`key`, `token`, or `api_key`). The in-app 401 hint and the user guide told people to try those modes.

HTTP 429 handling capped `Retry-After` at 60 seconds and could send another request early.

## What v2.4.1 does

- The token is not a build input. Gradle fails if `REPEATERBOOK_APP_TOKEN` or `REPEATERBOOK_AUTH_MODE` is set, or if a public identity property contains a token-shaped value.
- Each user pastes a personal `rbuapp_` token under **Search RepeaterBook → ⋮ → RepeaterBook token**. Shared `app_` tokens are refused. The value is stored with AndroidX encrypted preferences, excluded from backup rules, and not shown again.
- Export calls send only the `X-RB-App-Token` header, and only to `https://www.repeaterbook.com/api/`. Proximity HTML requests do not receive the token. Credential query parameters are stripped if a URL already has them.
- A 429 records the full `Retry-After` delay (delta-seconds or HTTP-date, no 60-second cap). The app waits that delay before one inline retry when the delay is at most 120 seconds. A longer delay is reported, and no further export call is made until it has elapsed.
- HTTP 401, HTTP 403, and the error codes `auth_missing`, `auth_invalid`, `auth_inactive`, `auth_revoked`, `auth_scope_denied`, and `ua_mismatch` are not retried. Tone enrichment stops on those failures instead of walking more queries.
- Search screens still say “Data courtesy of RepeaterBook.com” and link to [RepeaterBook.com](https://www.repeaterbook.com) and the [API terms](https://www.repeaterbook.com/wiki/doku.php?id=api).

`UserGuide.md` is the guide source. `UserGuide.pdf` in this tree was rebuilt from it and no longer tells anyone to put a credential in a query string or in `local.properties`. The live GitHub Pages copy updates when that guide is published from the project repository.

Public User-Agent identity may still be set with `REPEATERBOOK_CONTACT_EMAIL`, `REPEATERBOOK_APP_URL`, or an exact `REPEATERBOOK_USER_AGENT`. Those are not credentials. When the exact string is unset, the app sends:

`NicFwChannelEditor/<versionName> (+<app url>; <contact email>)`

The default URL is `https://github.com/jnyer27/NICFW-H3-25-CHIRP-ADAPTER`.

## Released builds

Scanned the published release APKs, not source. The scanner reports class, count, and length. It does not print the value.

| Release | Shared-token-form strings in `classes.dex` | Personal-token-form strings |
|---|---|---|
| 2.0.0 | 1 value, prefix `app_`, length 31 | 0 |
| 2.1.0 | same value | 0 |
| 2.3.0 | same value | 0 |
| 2.3.1 | same value | 0 |
| 2.4.0 | same value | 0 |

There is no GitHub release APK for 2.2.0. v1.x predates RepeaterBook search.

RepeaterBook did not claim this value matched a reviewed token or that it had been misused. It is still a credential-shaped string shipped in those APKs. Revoke or rotate it from the RepeaterBook dashboard. Do not email the value, and do not paste it into this file, an issue, or a commit.

## Verify a new APK

From the repository root:

```bash
python3 scripts/verify_no_repeaterbook_shared_credential.py
python3 scripts/verify_no_repeaterbook_shared_credential.py --apk path/to/app-release.apk
```

A clean tree prints `source_ok`. An APK that still contains a token-shaped string exits 1 and prints counts only.

A signed release APK still has to be built on a machine with the release keystore. This tree does not include that keystore, so this change does not attach a replacement APK.

## GitHub issue

File this on `jnyer27/NICFW-H3-25-CHIRP-ADAPTER`. This environment is not logged into GitHub, so the issue was not opened there.

**Title:** RepeaterBook access suspended: stop embedding credentials and query-string auth

**Body:**

RepeaterBook (Garrett, KD6KPC) suspended API access for the TD-H3 nicFW 2.5 Android app after a compliance review of the public v2.4.0 APK.

The APK contains a shared-token-form credential because the build copies `REPEATERBOOK_APP_TOKEN` from `local.properties` into the application. Keeping that file out of Git does not protect the value in the APK. The HTTP client and the troubleshooting text also allow credentials in the query string. RepeaterBook did not test the embedded value and is not claiming it matches a reviewed token or has been misused.

v2.4.1 removes that design:

- no shared credential in the build
- personal `rbuapp_` token entered at runtime and stored encrypted
- `X-RB-App-Token` header only; query-string authentication and the related troubleshooting text are gone
- full `Retry-After` delay, with no retry of authentication, scope, or User-Agent failures
- attribution and link-back kept

Published APKs 2.0.0, 2.1.0, 2.3.0, 2.3.1, and 2.4.0 each contain the same `app_`-prefixed value (length 31) in `classes.dex`. Revoke or rotate that live credential from the RepeaterBook dashboard. Do not put the value in this issue.

Verification: `python3 scripts/verify_no_repeaterbook_shared_credential.py --apk app-release.apk`.

A signed replacement APK still needs a local release build. See `AndroidNICFW_CH_EDITOR/REPEATERBOOK_API.md`.

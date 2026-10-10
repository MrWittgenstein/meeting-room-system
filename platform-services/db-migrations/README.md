# Meeting room service data migration

The two extracted services use dedicated databases:

```text
meetingroom_users
meetingroom_notifications
```

Import the required table schemas into these databases before starting the
services. Then run
`V1__meetingroom_user_bridge.sql` against the same MySQL instance. It adds
`legacy_user_id`, copies the meeting-room users into the new user database,
and preserves their BCrypt password hashes, encrypted legacy email values,
and legacy email login hashes. The user service accepts BCrypt passwords and
resolves email accounts against the imported MD5/SHA-256 hash, so an email and
the original password remain usable even when the legacy email encryption key
is unavailable.

Run `com.example.usermgmt.userservice.migration.LegacyMeetingEmailBackfill`
from the `user-service` module after importing users. Set `MEETING_DB_PASSWORD`
and `USER_DB_PASSWORD`; set `MEETINGROOM_MASTER_KEY` to the actual key used by
the source deployment when available. The migration validates the legacy hash
before writing plaintext `mail`. If the key is unavailable or does not decrypt
the source values, the script leaves `mail` empty while preserving the email
hash and ciphertext. Password login still works; email delivery to those users
requires recovering the correct legacy key and rerunning the backfill.

The source project databases remain separate and are not used at runtime.

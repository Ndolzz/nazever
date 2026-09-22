# NazeVer — Design

> Dokumen ini WAJIB mengikuti `requirements.md`. Semua FR/SR/NFR di bawah merujuk ID requirement tersebut.

## 1. Architecture Overview

Stack mengikuti repositori: **Android (Kotlin, Jetpack Compose, MVVM, Hilt)** + **Supabase** (Postgres, Auth, Realtime, Storage, Edge Functions) + **WebRTC** (panggilan) via library WebRTC Android.

```
Android App (Kotlin/Compose)
  ├─ UI Layer (screens, components, scrapbook UI kit)
  ├─ Domain Layer (use cases, repositories interface)
  ├─ Data Layer (repositories impl, local Room DB, remote Supabase)
  ├─ Realtime (Supabase Realtime channel per couple)
  └─ WebRTC (voice/video/screen share) + Supabase Edge Function signaling
Supabase
  ├─ Auth (email/password, JWT)
  ├─ Postgres + RLS (single source of truth)
  ├─ Realtime (postgres_changes + broadcast)
  ├─ Storage (media, wallpaper, avatar; signed URL)
  └─ Edge Functions (pairing, once-view, call signaling, security events, rate limiting)
```

### Modul Gradle (sesuai struktur repo)
- `app` — application, navigation, DI, screens
- `core-data` — repository implementasi + local cache (Room)
- `core-domain` — model + repository interface (Pairing, Auth, dst.)
- `core-network` — Supabase client provider, API layer
- `core-security` — SecureSessionStore (EncryptedSharedPreferences), token/session, security event helper
- `supabase` — schema.sql + edge functions

## 2. Data Flow per Fitur Penting
- **Chat**: compose → local pending row (status=pending, idempotency key UUID) → insert ke Postgres via realtime → status delivered/read via broadcast → retry on failure. Realtime postgres_changes stream → dedup by message id → UI.
- **Wallpaper**: pilih dari gallery → kompres → upload Storage `couples/{id}/wallpaper` → broadcast event `wallpaper_changed` → pasangan download → cache lokal → fallback default pattern jika gagal.
- **Pairing**: Edge Function `create_pairing_invite` (kode 6 digit, TTL 15 mnt) → pasangan `consume_pairing_invite` → `confirm_pairing`; `request_unpair` / `revoke_pairing_invite` untuk unpair/batal. Kode sekali pakai + expired.
- **Once-view**: upload ke Storage path `once/{coupleId}/{messageId}` (private) → saat penerima membuka: Edge Function `mark_once_viewed` → blob dihapus server-side, row status=viewed → akses ulang ditolak (FR-05.3).
- **Call**: WebRTC peer-to-peer; signaling (offer/answer/ICE) lewat Edge Function + Realtime broadcast channel `call:{coupleId}`. STUN Google; TURN opsional (NFR free → fallback relawan publik TURN atau audio-only via fallback di jaringan symmetric NAT).
- **Location**: foreground service Android saat share aktif → update position → Realtime broadcast (interval adaptif 5–15 s) → TTL expiry per FR-09 → stop service.

## 3. Database Design (Postgres, RLS wajib)

RLS policy umum: `auth.uid()` adalah member dari `couples` terkait (helper function `is_couple_member(couple_id)`).

| Tabel | PK | Kolom kunci | FK / Relasi | Index | Cascade |
|---|---|---|---|---|---|
| users | id (uuid, auth.users) | username, avatar_url, created_at | — | username unique | — |
| profiles | user_id | display_name, avatar_url, privacy flags | → users 1:1 | — | on delete cascade |
| couples | id | couple_name, couple_avatar_url, relationship_date, wallpaper_url, wallpaper_version | — | — | — |
| couple_members | (couple_id, user_id) | role, joined_at | → couples, users | unique(couple_id,user_id), max 2 row enforced via trigger | cascade |
| pairing_requests | id | code_hash, requester_id, invitee_id, status(pending/accepted/rejected/expired/used), expires_at | → users | code_hash, expires_at | cascade dari users |
| devices | id | user_id, device_name, platform, last_active_at, fcm_token | → users | user_id | cascade |
| sessions | id | user_id, device_id, refresh_token_hash, created_at, revoked_at | → users, devices | user_id | cascade |
| messages | id | couple_id, sender_id, type(text/media/voice/location/note_ref), body, reply_to_id, status, edited_at, deleted_at, once_view, idempotency_key, created_at | → couple_members, messages(self reply) | (couple_id, created_at desc), idempotency_key unique | cascade dari couple |
| message_attachments | id | message_id, media_id | → messages, media | message_id | cascade |
| media | id | couple_id, uploader_id, kind, storage_path, thumb_path, size, duration, mime, once_viewed_at, metadata | → couples | couple_id, created_at | cascade; blob deleted saat once_viewed |
| calls | id | couple_id, kind(voice/video/screen), initiator_id, started_at, ended_at, status(ringing/accepted/rejected/missed/ended/failed) | → couples | (couple_id, started_at desc) | cascade |
| call_participants | id | call_id, user_id, joined_at, left_at, connection_stats | → calls, users | call_id | cascade |
| locations | id | couple_id, sharer_id, lat, lng, accuracy, shared_until, stopped_at, is_last_known | → couples | (couple_id, created_at desc) | cascade |
| notes | id | couple_id, author_id, title, body, pinned, template_id, updated_at, deleted_at | → couples | (couple_id, pinned desc, updated_at desc) | cascade |
| note_templates | id | name, animal_key, layout_json, asset_bundle | — | — | — |
| moods | id | user_id, couple_id, mood_key, note, created_at | → couples, users | (couple_id, created_at desc) | cascade |
| calendar_events | id | couple_id, creator_id, title, description, event_date, event_time, remind_at, deleted_at | → couples | (couple_id, event_date) | cascade |
| notifications | id | user_id, kind, payload, read_at, created_at | → users | (user_id, created_at desc) | cascade |
| wallpapers | id | couple_id, storage_path, version, updated_by | → couples unique(couple_id) | — | cascade |
| privacy_settings | user_id | show_online, show_last_seen, show_typing, show_read_receipt | → users 1:1 | — | cascade |
| security_events | id | user_id, kind(new_device_login, session_revoked, unpair, deletion), detail, created_at | → users | (user_id, created_at desc) | cascade |

Data retention: pesan/media milik couple — retained sampai unpair atau deletion request; once-view blob dihapus segera; security_events 90 hari; locations purge saat share berakhir.

## 4. API & Edge Functions

Semua edge function memverifikasi JWT dan `is_couple_member` (SR-02).
- `create_pairing_invite`, `consume_pairing_invite`, `confirm_pairing`, `request_unpair`, `revoke_pairing_invite`
- `mark_once_viewed` (FR-05), `call_signal` (offer/answer/ICE, rate-limited), `revoke_session` (logout device lain)
- Client-side API: Supabase SDK (PostgREST) untuk CRUD biasa, selalu scoped `couple_id` milik user (RLS memaksa ini).

## 5. Authentication & Authorization
- Supabase Auth email/password; access token short-lived; refresh disimpan di EncryptedSharedPreferences (SecureSessionStore, modul core-security).
- Authorization: RLS pada semua tabel + verifikasi membership di edge function + interceptor repository client-side. Ownership: data couple milik couple, bukan individu; edit/delete message hanya sender (atau keduanya untuk notes/calendar/couple profile).
- Login detection: devices row baru → security_event `new_device_login` → notifikasi ke user lain / perangkat lain (FR-01.7).

## 6. Realtime Design
- Satu Realtime channel `couple:{coupleId}` per app instance; events: `message`, `typing`, `presence`, `read`, `wallpaper`, `profile`, `note`, `mood`, `calendar`, `location`, `call_signal`.
- Duplicate event: dedup by event id + version monotonic per entity.
- Out-of-order: urutkan by `created_at` + seq; conflict: last-write-wins (updated_at), delete = tombstone (deleted_at) menang.
- Reconnect: presence + `postgres_changes` resume; on reconnect → full delta sync by `updated_at > last_sync`.
- Offline changes: queue lokal (WorkManager) dengan idempotency key; sync failure → tetap pending + notifikasi retry.

## 7. Offline Design
Room sebagai mirror cache (messages, notes, calendar, profile, moods). State machine pesan: `pending → sent → delivered → read` dan `pending_failed → retrying`. Upload/download gagal → tombol retry eksplisit; tidak ada status "terkirim" palsu (NFR-02).

## 8. Media Design
Kompresi (image: WebP/quality target, video: bitrate rendah), thumbnail server-side generated, progress via chunked upload, cancelable. Storage private + signed URL short expiry. Media gallery = query media by couple_id.

## 9. Calls & Screen Sharing (WebRTC)
- Voice/video: WebRTC PeerConnection; audio unit; camera2 capture; mute/speaker/camera-switch pada MediaStream track.
- Screen share Android: MediaProjection API → video track; bila perangkat < Android 5 atau user tolak permission → tampilkan pesan keterbatasan + fallback (tidak dipaksa-palsukan).
- STUN: `stun:stun.l.google.com:19302`. TURN: opsional via env `TURN_URL/TURN_USER/TURN_CRED` (GitHub Secrets untuk CI; Supabase env untuk runtime) — tanpa TURN berbayar, fallback: reconnect + indikator kualitas.

## 10. Notifications
FCM push (background) + Realtime (foreground). Animal sounds dari asset legal (CC0) di `res/raw` (cat, dog, bird, rabbit), dipilih di Settings. Security event → high-priority notification.

## 11. UI/UX Screens (semua gaya scrapbook, soft pastel, font aplikasi tunggal)

Font aplikasi: satu font rounded/soft custom legal (mis. Baloo 2 / Quicksand — SIL OFL) digunakan konsisten di seluruh surface. No user font-size setting (FR-16.3).

Chat bubble spec (fixed, FR-16.4): text 16sp, radius 20dp, padding H 16dp V 10dp, max width 72% layar, spacing antar bubble 8dp, timestamp 11sp.

| Screen | Isi |
|---|---|
| Splash | Logo animal illustration + shared wallpaper fade-in |
| Login / Register | Kartu scrapbook dekoratif, input soft, animal mascot |
| Pairing | Buat kode (6 digit + TTL countdown) / masukkan kode; status pasangan |
| Home | Sederhana, bukan dashboard: wallpaper bersama penuh, kartu mood & last-seen pasangan, quick action (Chat, Notes, Calendar, Gallery, Call), countdown hari relationship date |
| Chat | Bubble spec di atas, reply/edit/delete/copy, status centang, typing, search bar |
| Media / Gallery | Grid thumbnail, filter foto/video/voice/dokumen, preview viewer, once-view overlay khusus |
| Voice/Video Call | Fullscreen soft gradient, avatar pasangan, kontrol bulat besar; screen share: PiP pipisan + tombol stop |
| Location | Peta + durasi pilihan (15m/1h/sampai matikan), indikator share aktif, last known badge |
| Notes | Kartu scrapbook bertemplate hewan, pin, search, edit realtime |
| Mood | Pemilih emoji animal + riwayat mini dua kolom (A dan pasangan) |
| Calendar | Month grid soft, event chip, form event dengan reminder |
| Couple Profile | Couple name, avatar ganda, relationship date, wallpaper picker dari gallery |
| Settings | Suara notifikasi animal, privacy toggles, devices, unpair, hapus akun |
| Privacy | Toggle online/last seen/typing/read receipt |
| Devices | Daftar perangkat + logout remote |
| Security | Riwayat security event |

Fallback wallpaper: default pattern soft + retry silent; layout tidak bergantung wallpaper (readability dijaga overlay scrim).

## 12. Project Structure
```
nazever/
├── app/                  # screens, navigation, DI, viewmodels
├── core-domain/          # model + repository interfaces
├── core-data/            # repository impl, Room cache, offline queue
├── core-network/         # Supabase client, API, realtime, WebRTC signaling
├── core-security/        # SecureSessionStore, security events
├── gradle/               # version catalog
├── supabase/             # schema.sql, migrations, edge functions
├── docs/                 # requirements.md, design.md, tasks.md
├── .github/workflows/    # CI (lihat §14)
└── app/src/test, androidTest  # unit + integration tests
```

## 13. Security & Performance
- TLS default; RLS; rate limit pairing/auth/signaling (edge function per-user counter); input validation (server-side mandatory); secret hanya env/Secrets.
- Performance: pagination 50/page, Room index paralel dengan DB index, WorkManager untuk upload, thumbnail WebP, presence hemat baterai (interval adaptive), image memory cache LRU.

## 14. GitHub Actions Design
Satu workflow utama `ci.yml` (trigger PR & push) + satu `security.yml` (schedule/PR):

ci.yml jobs:
1. checkout → setup JDK 17 + Gradle cache
2. struktur validasi (script cek modul wajib ada)
3. `ktlint` / formatter check
4. unit test (`testDebugUnitTest`) → report JUnit XML
5. build (`assembleDebug`)
6. upload artifact (report, log) saat gaganggagal — selalu upload test report

security.yml: secret scanning (gitleaks), dependency check (Dependency Guard), validasi tidak ada hardcoded key (grep env pattern).

Rules: exit code benar (Gradle fail → job fail), error jelas di log, secret via \`SECRETS\`/env, tidak disimpan di source.

## 15. Testing Design
- Unit: viewmodel, repository, offline queue, idempotency, conflict resolver.
- Integration/API: Supabase lokal emulator/test project — pairing lifecycle, once-view, RLS (authorization: user luar couple ditolak).
- Wajib (dari requirements §15): akses data couple lain (harus 403/deny), media couple lain ditolak, session expired/invalid token, pairing expired/used, reconnect realtime, send fail, upload fail, once-view dibuka 2x (kedua ditolak), location stop, logout device lain.
- UI test: Compose UI test untuk happy path chat/pairing.

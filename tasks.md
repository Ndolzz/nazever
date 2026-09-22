# NazeVer — Tasks

> Task list ini WAJIB dibaca setelah `requirements.md` dan `design.md`. Setiap task merujuk FR/SR/NFR. Tidak ada task tanpa requirement, tidak ada fitur baru di luar requirement.

## Konvensi
- Priority: P0 (blocker) / P1 (core) / P2 (penting) / P3 (nice)
- Definition of Done (setiap task): implementasi + tests lulus + lint + build + CI hijau + authorization/offline/realtime diuji bila relevan.
- Debug workflow per fitur: Requirement → Design → Task → Implementation → Local validation → Push → GitHub Actions → Test/Lint/Build/Security → Error detection → Fix → Push ulang → CI pass → Final review.

## Phase 1 — Foundation
- **TASK-001** P0 — Setup Gradle multi-modul & version catalog. Repo: app, core-domain, core-data, core-network, core-security ter-compile. Dep: —. Files: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml. AC: `assembleDebug` sukses. CI: build job.
- **TASK-002** P0 — CI pipeline `ci.yml` (NFR-04): checkout, JDK, struktur, ktlint, unit test, build, test report artifact. Dep: TASK-001. Files: .github/workflows/ci.yml.
- **TASK-003** P0 — Supabase project & schema.sql awal (design §3) + RLS helper `is_couple_member` (SR-02.1/2.3). Files: supabase/schema.sql. AC: migration apply sukses.
- **TASK-004** P1 — Design system & UI kit: theme, shared font (FR-16.3), chat bubble spec (FR-16.4), scrapbook components (FR-16.5). Files: app/ui/theme, components. Test: UI snapshot sederhana.

## Phase 2 — Authentication (FR-01, SR-01)
- **TASK-005** P0 — Supabase auth client + SecureSessionStore (token di EncryptedSharedPreferences, SR-01). Files: core-security, core-network.
- **TASK-006** P0 — Register/Login/Logout screens + viewmodel. Dep: TASK-005. Test: unit (form validation), auth test sukses/gagal.
- **TASK-007** P1 — devices & sessions table + session management; logout device lain (edge `revoke_session`). Test: revoke session dari device lain.
- **TASK-008** P2 — Login detection → security_events + notifikasi (FR-01.7). Test: login device baru memicu event.

## Phase 3 — Pairing (FR-02)
- **TASK-009** P0 — Edge `create/consume/confirm_pairing_invite` + revoke (kode 6 digit, TTL 15 mnt, sekali pakai, rate limit SR-03.5). Files: supabase/functions. Test: pairing expired, kode terpakai, user ketiga ditolak (FR-02.8).
- **TASK-010** P0 — Pairing screen + status couple. Dep: TASK-009. Test: UI happy path + reject path.

## Phase 4 — Database (SR-02, NFR-01)
- **TASK-011** P0 — RLS policy semua tabel + authorization test suite (baca data couple lain DITOLAK; IDOR negatif). Test: wajib semua skenario §15 requirements.

## Phase 5 — Couple Profile (FR-11, FR-16.2)
- **TASK-012** P1 — Couple profile model + screen (name, avatar, relationship date). Sync realtime profile.
- **TASK-013** P1 — Shared wallpaper: gallery picker → kompres → upload → broadcast → pasangan update + fallback pattern. Test: wallpaper gagal dimuat → fallback (FR-16.2).

## Phase 6 — Chat (FR-03, NFR-02/03)
- **TASK-014** P0 — Messages CRUD + realtime stream + pagination (50/page) + Room cache. Files: core-data, app/presentation/chat. Test: unit repository, dedup, out-of-order.
- **TASK-015** P0 — Status sent/delivered/read + typing + presence + last seen (respect privacy FR-10). Test: realtime status.
- **TASK-016** P1 — Reply/edit/delete/copy + tombstone delete + search. Test: edit sync, delete tombstone menang.
- **TASK-017** P0 — Offline queue + retry + idempotency key (NFR-02). Test: kirim offline → sync; duplicate ditolak; gagal tampil failed bukan sent.

## Phase 7 — Media (FR-04)
- **TASK-018** P1 — Upload photo/video/voice/document: kompresi, thumbnail, progress, cancel, retry. Files: core-data media, WorkManager. Test: upload fail → retry sukses.
- **TASK-019** P1 — Media gallery + preview + file info. Dep: TASK-018.

## Phase 8 — Once View (FR-05)
- **TASK-020** P1 — Edge `mark_once_viewed` + blob deletion server-side + UI overlay. Test: dibuka 2x → akses kedua DITOLAK server (FR-05.3).

## Phase 9 — Voice Call (FR-06)
- **TASK-021** P1 — WebRTC setup + signaling via edge/realtime + STUN (env TURN). Test: signaling unit, connection state. Files: core-network/webrtc.
- **TASK-022** P1 — Call screens (incoming/outgoing/active), accept/reject/end, reconnect. Test: reject/missed/end state; call history rows.

## Phase 10 — Video Call (FR-07)
- **TASK-023** P2 — Video track, permission handling, camera switch, mute, speaker. Test: permission denied flow.

## Phase 11 — Screen Sharing (FR-08)
- **TASK-024** P2 — MediaProjection capture → video track + fallback pesan keterbatasan. Test: unsupported device path.

## Phase 12 — Location (FR-09)
- **TASK-025** P2 — Foreground service share (15m/1h/until-off), realtime broadcast adaptif, expiry, last known, stop. Test: stop sharing → update berhenti; TTL expiry (SR-05.4: no silent tracking).

## Phase 13 — Notes (FR-12)
- **TASK-026** P1 — Notes CRUD + pin + search + animal templates (note_templates) + scrapbook decoration + realtime. Test: sync & conflict LWW.

## Phase 14 — Mood (FR-13)
- **TASK-027** P2 — Mood picker + realtime tampil ke pasangan + riwayat mini. Non-diagnosis (FR-13.3).

## Phase 15 — Calendar (FR-14)
- **TASK-028** P1 — Calendar events CRUD + reminder notif lokal + realtime sync. Test: event sync + reminder fired.

## Phase 16 — Notifications (FR-15)
- **TASK-029** P1 — FCM + realtime foreground router: message, call, pairing, location, notes, calendar, mood, security event. Animal sounds CC0 (FR-16.6) di settings. Test: routing per kind.

## Phase 17 — Privacy (FR-10)
- **TASK-030** P1 — privacy_settings + enforcement di presence/typing/read (server-side + client). Test: toggle off → data tidak dibroadcast.

## Phase 18 — Security (SR-01..05)
- **TASK-031** P0 — Rate limiting auth/pairing/signaling + input validation server + secret scanning CI (`security.yml`: gitleaks, dependency guard). Test: rate limit triggered; CI security job hijau.
- **TASK-032** P1 — Account deletion + unpair data policy + security_events screen (FR-01.8, FR-02.6). Test: deletion cascade benar.

## Phase 19 — UI/UX polish (FR-16)
- **TASK-033** P2 — Semua screen sesuai design §11: splash, home non-dashboard, scrapbook detail, fallback dekorasi, readability. Test: Compose UI test navigasi utama.

## Phase 20 — Offline & Realtime hardening (NFR-02/03)
- **TASK-034** P1 — Delta sync on reconnect, conflict resolver, duplicate/out-of-order test suite. Test: skenario §8 requirements penuh.

## Phase 21 — Performance (NFR-01)
- **TASK-035** P2 — Audit: lazy loading, cache LRU, thumbnail, indexing (design §13), battery (presence interval). AC: gallery 1000 item scroll 60fps target; memory stabil.

## Phase 22 — Automated Testing (§15 requirements)
- **TASK-036** P0 — Full test matrix: auth, authorization (wajib prioritas), realtime, database, media, call, location, offline, security, UI test. Test: semua skenario wajib §15 tercantum & hijau di CI.

## Phase 23 — GitHub Actions refinement (NFR-04)
- **TASK-037** P1 — Test report artifact selalu di-upload, log jelas per failure, exit code benar, secret hanya Secrets/env. AC: pipeline gagal bisa didiagnosis dari log.

## Phase 24 — Debugging (§13 requirements)
- **TASK-038** P1 — Debug loop terdokumentasi & dipraktikkan: setiap fix → push → CI → verifikasi laporan → fix ulang sampai hijau. AC: riwayat CI menunjukkan siklus fix berjalan.

## Phase 25 — Final Review
- **TASK-039** P0 — Traceability audit: setiap FR/SR/NFR → design section → task → test. Tidak ada requirement tanpa coverage. Files: docs/*. AC: traceability matrix lengkap.
- **TASK-040** P0 — Final validation: semua Definition of Done §17; regresi fitur; release build.

## Traceability Ringkas
| Requirement | Design | Task |
|---|---|---|
| FR-01 Account | §5 | TASK-005..008 |
| FR-02 Pairing | §4 | TASK-009,010,032 |
| FR-03..05 Chat/Media/Once-view | §2,6,7,8 | TASK-014..020 |
| FR-06..08 Calls | §9 | TASK-021..024 |
| FR-09 Location | §2 | TASK-025 |
| FR-10..15 Shared features | §6,10,11 | TASK-012,013,026..030 |
| FR-16 Visual | §11 | TASK-004,033 |
| SR-01..05 | §3,5,13 | TASK-005,007,011,031,032 |
| NFR-01..05 | §6,7,13,14 | TASK-017,034..038,002 |

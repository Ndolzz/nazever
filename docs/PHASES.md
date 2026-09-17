# NazeVer — Breakdown Fase

Pola sama seperti proyek NAZE TOOLS milikmu: satu phase = satu potongan
yang bisa selesai, ter-test, dan tidak meninggalkan fitur dummy.

- [x] **Phase 1 — Setup & Foundation** (selesai)
  Struktur modul, Supabase schema dasar (`profiles`, `pairs`,
  `pairing_invites`, `session_audit`), secure pairing (token 128-bit +
  approval), secure token storage (Keystore), auth email/password,
  network security config, ProGuard baseline.

- [ ] **Phase 2 — Realtime Chat (tanpa E2EE dulu)**
  Tabel `messages`, `message_reactions`, `attachments`. Reply, edit,
  delete, pin, search, read status, upload/download progress, offline
  queue dasar. RLS pola `pair_id in my_active_pairs()`.

- [ ] **Phase 3 — End-to-End Encryption untuk Chat**
  Key exchange per pair (disimpan di Keystore tiap device), enkripsi di
  device pengirim, dekripsi di device penerima. Pakai library
  kriptografi teruji (mis. libsignal atau Tink), bukan buatan sendiri.

- [ ] **Phase 4 — Media Messaging + View Once**
  Photo/video/voice note/dokumen lewat Photo Picker & system file
  picker. Tabel `view_once_media` dengan lifecycle OPENED, private
  storage bucket, cache cleanup.

- [ ] **Phase 5 — Voice & Video Call**
  WebRTC (self-hosted signaling lewat Supabase Realtime, STUN/TURN
  gratis/self-host, mis. coturn). Mute indicator dua sisi, screen
  share via system capture permission.

- [ ] **Phase 6 — Location**
  Current/live/history location, consent eksplisit, tidak ada hidden
  tracking. Tabel `locations`, `location_history` + retention setting.

- [ ] **Phase 7 — Daily Mood + Mood Calendar**
  Popup terjadwal (default 08:00/13:00/20:00, bisa diubah), tabel
  `moods`, kontrol sharing ke pasangan.

- [ ] **Phase 8 — Shared Calendar**
  Tabel `calendar_events`, view month/week/day/agenda, MY/PARTNER/SHARED
  event, countdown, reminder (WorkManager + notification channel).

- [ ] **Phase 9 — Status, Digital Letter, Shared To Do**
  Tiga fitur lebih kecil digabung satu phase karena pola CRUD + realtime
  mirip: `statuses`, `digital_letters`, `todos`.

- [ ] **Phase 10 — Music Room**
  `playlists`, `playlist_items`, realtime reorder/favorite. Sumber musik
  legal (metadata saja atau integrasi layanan yang user sudah punya
  akun, bukan distribusi file berhak cipta).

- [ ] **Phase 11 — Custom Animal Companion**
  `animal_config` shared, animasi ringan (Lottie/Compose), state
  idle/walk/sleep/happy/message/call/mood/task-complete, sinkron
  realtime ke kedua device.

- [ ] **Phase 12 — Shared Appearance (Wallpaper, Font, Theme)**
  Upload wallpaper dari Gallery (Photo Picker), local compress+resize
  sebelum upload, `shared_theme` realtime.

- [ ] **Phase 13 — Notifications (Animal Sound, Privacy)**
  Notification channel per kategori, custom sound, show/hide preview di
  lock screen.

- [ ] **Phase 14 — Shared Home + Memories + Activity Timeline**
  Layar utama gabungan (avatar, countdown, mood terakhir, status
  lokasi, animal). `memories` scrapbook timeline, `activities` log
  ringan dengan delete history.

- [ ] **Phase 15 — Privacy Center**
  App lock (PIN/biometric), Active Devices (pakai `session_audit` dari
  Phase 1), unpair dua-sisi (mengganti implementasi sepihak Phase 1),
  export/delete data.

- [ ] **Phase 16 — Hardening & Quota Protection**
  Rate limiting menyeluruh, exponential backoff, storage quota
  monitoring, dependency vulnerability scan, release build audit
  (obfuscation, tamper protection sesuai kemampuan platform).

Setiap phase menghasilkan: migrasi SQL baru (append, bukan rewrite
`schema.sql` lama), Edge Function baru jika perlu, modul `feature-*`
baru, dan update ke `docs/ARCHITECTURE.md` bagian "known limitations"
begitu limitation itu selesai dikerjakan.

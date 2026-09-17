# NazeVer — Arsitektur (Phase 1: Setup, Auth, Pairing)

## 1. Prinsip yang dipegang

- **Private first / Secure first / Free first / Local first / Efficient first.**
- Authorization SELALU di server (RLS + Edge Function), tidak pernah
  dipercayakan ke logika client.
- Tidak ada fake feature: modul yang belum diimplementasi TIDAK
  di-include di `settings.gradle.kts` dan bagian UI-nya ditandai TODO
  eksplisit, bukan pura-pura jalan.

## 2. Pembagian modul

```
app             -> presentation (Compose, ViewModel, navigation, DI wiring)
core-domain     -> model + repository interface, murni Kotlin, tanpa Android/SDK
core-data       -> implementasi repository di atas Supabase SDK
core-network    -> provider SupabaseClient, tidak tahu apa-apa soal domain
core-security   -> Keystore-backed storage, tidak tahu apa-apa soal Supabase
supabase/       -> schema.sql + Edge Functions (Deno/TS) - hidup di luar APK
```

Aturan dependency: `app` -> semua core-*; `core-data` -> `core-domain` +
`core-network` + `core-security`; `core-domain` TIDAK bergantung ke modul
lain sama sekali (supaya gampang di-unit-test tanpa Android).

Modul fitur (`feature-chat`, `feature-call`, dst) akan ditambahkan satu
per satu di phase berikutnya, masing-masing bergantung ke `core-domain`
dan `core-data` saja — tidak saling bergantung satu sama lain, supaya
tetap modular.

## 3. Kenapa pairing tidak pakai kode pendek

Kode numerik pendek (4-6 digit) bisa ditebak / brute-force dalam waktu
wajar walau dibatasi expiry. Desain di sini:

1. Token 128-bit acak **dibuat di server** (Edge Function), bukan client.
2. Hanya **hash** token yang disimpan di DB (`pairing_invites.token_hash`),
   token mentah cuma dikirim sekali ke pembuat invite.
3. Token dibagikan lewat kanal out-of-band (share sheet OS / QR), bukan
   lewat server aplikasi ini sendiri.
4. Consume oleh User B **tidak langsung** membuat pair aktif — statusnya
   jadi `AWAITING_CONFIRMATION`, User A harus approve eksplisit di
   perangkatnya. Ini mengatasi skenario link ter-screenshot/tersebar.
5. Insert ke tabel `pairs` hanya bisa lewat fungsi SQL
   `finalize_pairing` yang di-grant khusus ke `service_role` — client
   (bahkan yang authenticated) tidak punya izin INSERT langsung.

## 4. Known limitations di phase ini (didokumentasikan, bukan disembunyikan)

- **Gradle Wrapper (`gradlew`) belum di-commit.** CI (`.github/workflows/build.yml`)
  menginstal Gradle langsung lewat `gradle/actions/setup-gradle`, jadi
  tidak terpengaruh. Untuk build lokal (Termux/Android Studio), commit
  wrapper resmi begitu ada akses network: `gradle wrapper --gradle-version 8.10.2`,
  lalu commit `gradlew`, `gradlew.bat`, dan `gradle/wrapper/`.
- `request_unpair` saat ini memutus pairing sepihak (siapa pun di pair
  bisa unpair langsung). Alur "kedua user harus konfirmasi" direncanakan
  di phase Privacy Center, butuh tabel `pair_unpair_requests` baru.
- QR code rendering di `PairingScreen` belum diimplementasi (masih
  menampilkan deep link sebagai teks) — ditandai TODO, akan dipakai
  library QR lokal (tanpa upload ke server pihak ketiga).
- Push notification untuk "ada permintaan pairing masuk" belum ada;
  saat ini hanya mengandalkan Supabase Realtime (butuh app dalam
  keadaan foreground/terhubung). Ditambahkan di phase Notifications.
- End-to-end encryption untuk isi pesan BELUM diimplementasikan di
  phase ini (baru fondasi auth+pairing). Jangan dianggap chat sudah
  terenkripsi sampai phase Chat + E2EE selesai — lihat section 5 master
  prompt asli soal E2EE.

## 5. Zero-cost posture

Semua yang dipakai di phase ini ada di free tier Supabase (Auth,
Postgres dengan RLS, Edge Functions dengan quota gratis). Tidak ada
dependency berbayar wajib. Lihat `docs/PHASES.md` untuk service apa
yang akan dipakai tiap phase dan free-tier-nya.

## 6. Cara jalankan (development)

1. Buat project Supabase baru (free tier).
2. `supabase db push` dari folder `supabase/` untuk apply `schema.sql`
   (atau jalankan isinya lewat SQL editor dashboard).
3. `supabase functions deploy create_pairing_invite consume_pairing_invite confirm_pairing revoke_pairing_invite request_unpair`
4. Set secret Edge Function: `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_URL`,
   `SUPABASE_ANON_KEY` lewat dashboard (Project Settings > Edge Functions).
5. Salin `local.properties.example` -> `local.properties`, isi
   `SUPABASE_URL` dan `SUPABASE_ANON_KEY` (anon key, BUKAN service role).
6. `./gradlew :app:assembleDebug`

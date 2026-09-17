# NazeVer

> Private two-person communication, digital scrapbook & shared life app.
> Android native (Kotlin), backend Supabase (free tier).

**Status:** Phase 1 dari 16 — Setup, Auth, Secure Pairing. Lihat
[`docs/PHASES.md`](docs/PHASES.md) untuk roadmap lengkap dan
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) untuk keputusan desain.

## Prinsip

Private first · Secure first · Free first · Local first · Efficient first.

Tidak ada fitur dummy — bagian yang belum diimplementasi ditandai TODO
eksplisit di kode/dokumentasi, bukan dibuat seolah sudah jalan.

## Struktur modul

```
app             presentation (Compose, ViewModel, navigation, DI)
core-domain     model + repository interface, pure Kotlin
core-data       implementasi repository di atas Supabase SDK
core-network    provider SupabaseClient
core-security   Keystore-backed secure storage
supabase/       schema.sql + Edge Functions (Deno/TS)
```

## Setup development

1. Buat project Supabase baru (free tier).
2. Apply `supabase/schema.sql` (SQL editor dashboard atau `supabase db push`).
3. Deploy Edge Functions di `supabase/functions/`:
   ```
   supabase functions deploy create_pairing_invite consume_pairing_invite confirm_pairing revoke_pairing_invite request_unpair
   ```
4. Set secret di dashboard Supabase (Project Settings > Edge Functions):
   `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`.
5. Salin `local.properties.example` → `local.properties`, isi
   `SUPABASE_URL` dan `SUPABASE_ANON_KEY` (anon key saja, bukan service role).
6. Build lokal: `gradle :app:assembleDebug` (Gradle Wrapper belum
   di-commit — lihat catatan di `.github/workflows/build.yml` — jadi
   pastikan Gradle 8.10.x terinstal, atau build lewat CI di bawah ini).

## Build otomatis (GitHub Actions)

Setiap push/PR ke `main` otomatis build APK debug lewat
`.github/workflows/build.yml`. Sebelum push pertama kali, set dua
secret di repo GitHub: **Settings → Secrets and variables → Actions →
New repository secret**:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

(Isi keduanya dengan anon key dari dashboard Supabase — bukan service
role key.) Setelah workflow selesai, APK bisa diunduh dari tab
**Actions** pada run terkait, di bagian **Artifacts** →
`nazever-debug-apk`.

## Keamanan

- Tidak ada API key/secret yang di-hardcode di source code.
- Session token disimpan lewat Android Keystore (`core-security`), tidak
  pernah plaintext, tidak pernah masuk Android auto-backup.
- Semua authorization ditegakkan di server (Row Level Security + Edge
  Function), bukan di client.
- Lihat `docs/ARCHITECTURE.md` bagian "Known limitations" untuk hal yang
  masih dalam pengerjaan.

## Lisensi

Private project — belum ditentukan lisensinya.

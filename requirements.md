# NazeVer — Requirements

> NazeVer adalah private couple communication app untuk tepat **dua pengguna yang telah dipasangkan**. Bukan social media, bukan platform publik, bukan group chat, tanpa komunitas, tanpa follower/following, tanpa iklan.

Dokumen ini adalah sumber requirement tunggal. `design.md` dan `tasks.md` WAJIB mengikuti dokumen ini.

## 1. Identitas Produk

| Aspek | Nilai |
|---|---|
| Nama | NazeVer |
| Tipe | Private couple communication app (2 pengguna) |
| Platform | Android (Kotlin) — sesuai stack repositori |
| Prioritas | Privacy → Security → Realtime sync → Simple interaction → Cute scrapbook aesthetic → Shared couple experience → Reliable offline behavior → Low resource usage → Free to operate (development) |

### Non-goals (dilarang)
- Social media, public profile, feed, follower/following
- Group chat / multi-party space
- Iklan / monetisasi selama development
- Font size / bubble style yang dapat diubah bebas oleh pengguna
- Fitur diagnosis kesehatan/psikologis pada Mood
- Silent tracking lokasi
- API key / secret yang di-hardcode

## 2. Functional Requirements

### FR-01 Account
- FR-01.1 Register (email+password), FR-01.2 Login, FR-01.3 Logout, FR-01.4 Session persist & refresh
- FR-01.5 Profile: username, avatar
- FR-01.6 Device management: daftar perangkat, logout dari perangkat lain
- FR-01.7 Session management + login detection (notifikasi login dari perangkat baru)
- FR-01.8 Account deletion (menghapus data user; couple space ditangani sesuai FR-02)

### FR-02 Pairing
- FR-02.1 Pairing code (dibuat oleh satu user), FR-02.2 Invitation/accept, FR-02.3 Reject
- FR-02.4 Expiration kode (default 15 menit), FR-02.5 Pair status (unpaired/paired)
- FR-02.6 Unpair (kedua pihak melihat statusnya), FR-02.7 Re-pair
- FR-02.8 **Hanya dua user dalam satu couple space.** User ketiga ditolak; kode yang sudah dipakai tidak bisa dipakai lagi.

### FR-03 Chat
- FR-03.1 Text message; FR-03.2 Reply; FR-03.3 Edit; FR-03.4 Delete; FR-03.5 Copy
- FR-03.6 Timestamp; FR-03.7 Status: sent → delivered → read
- FR-03.8 Typing indicator; FR-03.9 Online status; FR-03.10 Last seen
- FR-03.11 Message search; FR-03.12 Chat history + pagination (lazy)
- FR-03.13 Offline queue; FR-03.14 Retry failed message

### FR-04 Media
- FR-04.1 Photo; FR-04.2 Video; FR-04.3 Voice message; FR-04.4 Document/file
- FR-04.5 Preview; FR-04.6 Upload progress; FR-04.7 Download progress
- FR-04.8 Cancel upload; FR-04.9 Retry upload
- FR-04.10 Media gallery bersama; FR-04.11 File information (nama, ukuran, tipe, durasi)

### FR-05 Once View
- FR-05.1 Media dapat dikirim sebagai once-view
- FR-05.2 Setelah dibuka: status `viewed`, tidak bisa dibuka kembali di client
- FR-05.3 **Server wajib memblokir akses ulang** (bukan hanya UI)
- FR-05.4 Metadata mengikuti privacy policy; konten dihapus setelah viewed

### FR-06 Voice Call
- FR-06.1 One-to-one voice call; FR-06.2 Incoming; FR-06.3 Outgoing; FR-06.4 Accept; FR-06.5 Reject; FR-06.6 End
- FR-06.7 Reconnect; FR-06.8 Connection state indicator; FR-06.9 Call history

### FR-07 Video Call
- FR-07.1 One-to-one video call; FR-07.2 Camera permission; FR-07.3 Mic permission
- FR-07.4 Camera switch; FR-07.5 Mute; FR-07.6 Speaker; FR-07.7 End; FR-07.8 Reconnect

### FR-08 Screen Sharing
- FR-08.1 Start/stop screen share, permission, connection state, failure handling
- FR-08.2 Android MediaProjection didukung native; jika perangkat/versi tidak mendukung → tampilkan pesan keterbatasan, jangan fake implementasi (detail di design.md)

### FR-09 Live Location
- FR-09.1 Share realtime dengan durasi: 15 menit / 1 jam / sampai dimatikan
- FR-09.2 Start; FR-09.3 Stop; FR-09.4 Expiration otomatis
- FR-09.5 Realtime update; FR-09.6 Last known location
- FR-09.7 Permission handling; FR-09.8 Connection failure handling
- FR-09.9 **Tidak ada silent tracking** — hanya share saat user aktif membagikan.

### FR-10 Last Seen / Privacy settings
- FR-10.1 Setting per user: online status, last seen, typing indicator, read receipt (on/off masing-masing)

### FR-11 Shared Couple Profile
- FR-11.1 Couple name; FR-11.2 Couple avatar; FR-11.3 Relationship date
- FR-11.4 Shared wallpaper (sinkron ke pasangan); FR-11.5 Shared visual identity
- FR-11.6 Perubahan tersinkron realtime ke kedua perangkat

### FR-12 Shared Notes
- FR-12.1 Create; FR-12.2 Edit; FR-12.3 Delete; FR-12.4 Pin; FR-12.5 Search
- FR-12.6 Animal templates; FR-12.7 Scrapbook decoration; FR-12.8 Realtime sync

### FR-13 Mood
- FR-13.1 Pilih mood (ekspresi singkat, emoji/animal icon); FR-13.2 Pasangan melihatnya realtime
- FR-13.3 Mood history ringkas (bukan analisis/diagnosis)

### FR-14 Calendar
- FR-14.1 Create event; FR-14.2 Edit; FR-14.3 Delete; FR-14.4 Reminder (notif lokal)
- FR-14.5 Date, time, description; FR-14.6 Realtime sync

### FR-15 Notifications
- FR-15.1 Notifikasi realtime untuk: message, call, pairing, location, notes, calendar, mood, security event
- FR-15.2 Suara notifikasi animal-themed (cat, dog, bird, rabbit) — hanya asset legal (lihat FR-16)

### FR-16 Visual Identity (WAJIB dipertahankan)
- FR-16.1 Gaya: cute, private, personal, scrapbook, modern, clean, soft, interactive. **Bukan** clone WhatsApp/Telegram/Discord.
- FR-16.2 Shared Wallpaper: pilih dari gallery perangkat; simpan → sinkron → diterima pasangan → tampilan pasangan berubah. Fallback (default soft background) bila gagal dimuat.
- FR-16.3 Shared Font: satu font aplikasi konsisten di semua surface; tanpa pengaturan ukuran font bebas.
- FR-16.4 Chat bubble konsisten (text size, radius, padding, max width, spacing, timestamp size — nilai fixed di design.md).
- FR-16.5 Scrapbook elements: animal illustration, animal templates, stickers, patterns, decorative cards, small elements, soft background decoration — tidak boleh menurunkan readability/fungsi.
- FR-16.6 Animal notification sounds dari asset legal (CC0/license jelas), tanpa audio berhak cipta.

## 3. Security & Privacy Requirements

### SR-01 Authentication & Session
- SR-01.1 Auth berbasis token (refresh + access), session per device
- SR-01.2 Logout dari device lain, invalidasi session
- SR-01.3 Suspicious login detection (device baru → notifikasi security event)

### SR-02 Authorization (KRITIS)
- SR-02.1 **Setiap endpoint/data couple space wajib memverifikasi user adalah anggota couple tsb.**
- SR-02.2 User A tidak boleh bisa membaca data couple lain hanya dengan mengetahui ID resource (IDOR dilarang).
- SR-02.3 RLS (row-level security) database aktif untuk semua tabel couple data.

### SR-03 Data Protection
- SR-03.1 Encryption in transit (TLS) dan at rest (database default)
- SR-03.2 Secure file access (signed URL, expiry)
- SR-03.3 Storage access control; database access control
- SR-03.4 Input validation di client & server (edge function)
- SR-03.5 Rate limiting pada endpoint sensitif (auth, pairing, call signaling)

### SR-04 Secret Management
- SR-04.1 Tidak ada API key/password/token di repository
- SR-04.2 GitHub Secrets / Variables + environment variables untuk CI/CD
- SR-04.3 Secret scanning di CI

### SR-05 Privacy & Data Lifecycle
- SR-05.1 Privacy settings per user (FR-10)
- SR-05.2 Data deletion: account deletion menghapus data pribadi; couple data mengikuti kebijakan unpair
- SR-05.3 Once-view metadata mengikuti privacy policy
- SR-05.4 Location hanya dibagikan saat eksplisit

## 4. Non-Functional Requirements

### NFR-01 Performance (mobile-first)
- Lazy loading, pagination, caching, image compression, thumbnail
- Upload/download optimization, offline queue, DB indexing
- Hemat memory & baterai; koneksi realtime efisien; hindari dependency tak perlu

### NFR-02 Offline Behavior
- Pesan/media yang gagal tetap berstatus pending/retry — **tidak pernah tampil sebagai terkirim sebelum server konfirmasi**
- Queue offline, sync saat reconnect, conflict resolution (last-write-wins + tombstone untuk delete)
- Cached data untuk chat terakhir, notes, calendar; failed upload/download dapat di-retry

### NFR-03 Realtime
- Semua fitur bersama (chat, typing, read, wallpaper, profile, notes, mood, calendar, location, call signaling) realtime
- Handled: connection lost, reconnect, duplicate event, out-of-order event, conflict, offline changes, retry, sync failure

### NFR-04 CI/CD (GitHub Actions)
- Checkout, install deps, struktur valid, formatter check, linter, type check, unit test, integration test, build, security check, test report, upload log/artifact saat gagal
- Exit code benar; error jelas di log; secret hanya via GitHub Secrets/env

### NFR-05 Acceptance per fitur
Fitur selesai hanya jika: implementasi + requirement terpenuhi + design sesuai + tests ada & lulus + lint lulus + build lulus + security validation lulus + CI hijau + tidak merusak fitur lain + offline & realtime & authorization diuji.

## 5. Error Handling Requirements
- Semua kegagalan network/Api/server punya state UI eksplisit (pending/failed/retry)
- Tidak ada data hilang diam-diam; error user-facing memakai bahasa sederhana
- Server menolak duplicate event dengan idempotency key

## 6. Traceability Matrix (Requirement → Design → Task)
Setiap FR/SR/NFR dipetakan di design.md (arsitektur & data) dan tasks.md (TASK-xxx). Tidak boleh ada requirement tanpa design+task.

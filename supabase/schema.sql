-- =========================================================================
-- PAIRSPACE — SUPABASE SCHEMA (Phase 1: setup, auth, pairing)
-- =========================================================================
-- Prinsip:
--   - Setiap tabel shared-data punya kolom pair_id -> pairs.id
--   - RLS WAJIB aktif di semua tabel, tanpa kecuali.
--   - Authorization murni server-side: policy mengecek auth.uid() ada di
--     salah satu sisi (user_a_id/user_b_id) pair yang bersangkutan.
--   - Tidak ada tabel yang bisa diakses anonymous (anon key doang tidak
--     cukup, harus lolos auth.uid() IS NOT NULL + membership check).
--   - Tabel lain (messages, calls, locations, moods, dst) akan
--     ditambahkan di phase-phase berikutnya; file ini fokus fondasi:
--     users, pairs, pairing_invites, sessions_audit.
-- =========================================================================

create extension if not exists "pgcrypto";

-- -------------------------------------------------------------------------
-- 1. PROFILES  (auth.users adalah tabel bawaan Supabase Auth; ini profil publiknya)
-- -------------------------------------------------------------------------
create table public.profiles (
    id              uuid primary key references auth.users(id) on delete cascade,
    display_name    text not null check (char_length(display_name) between 1 and 60),
    avatar_url      text,
    created_at      timestamptz not null default now()
);

alter table public.profiles enable row level security;

-- User boleh baca profil dirinya sendiri DAN profil partner-nya (via pairs),
-- tapi tidak boleh baca profil user lain yang tidak berelasi dengannya.
create policy "profiles_select_self_or_partner"
    on public.profiles for select
    using (
        id = auth.uid()
        or exists (
            select 1 from public.pairs p
            where p.status = 'ACTIVE'
              and ((p.user_a_id = auth.uid() and p.user_b_id = profiles.id)
                or (p.user_b_id = auth.uid() and p.user_a_id = profiles.id))
        )
    );

create policy "profiles_update_self"
    on public.profiles for update
    using (id = auth.uid())
    with check (id = auth.uid());

create policy "profiles_insert_self"
    on public.profiles for insert
    with check (id = auth.uid());

-- -------------------------------------------------------------------------
-- 2. PAIRS  (private pair space setelah pairing sukses)
-- -------------------------------------------------------------------------
create table public.pairs (
    id              uuid primary key default gen_random_uuid(),
    user_a_id       uuid not null references auth.users(id) on delete cascade,
    user_b_id       uuid not null references auth.users(id) on delete cascade,
    status          text not null default 'ACTIVE' check (status in ('ACTIVE', 'UNPAIRED')),
    created_at      timestamptz not null default now(),
    constraint pairs_distinct_users check (user_a_id <> user_b_id),
    -- Setiap user maksimal berada dalam SATU pair aktif pada satu waktu.
    constraint pairs_user_a_unique_active unique (user_a_id, status) deferrable initially immediate,
    constraint pairs_user_b_unique_active unique (user_b_id, status) deferrable initially immediate
);

alter table public.pairs enable row level security;

create policy "pairs_select_members_only"
    on public.pairs for select
    using (auth.uid() = user_a_id or auth.uid() = user_b_id);

-- INSERT/UPDATE ke pairs TIDAK dibuka untuk client sama sekali.
-- Hanya Edge Function (service role) yang boleh menulis, supaya pairing
-- selalu lewat validasi token + alur konfirmasi, tidak bisa dipalsukan
-- dari client dengan langsung insert row.
revoke insert, update, delete on public.pairs from authenticated;

-- -------------------------------------------------------------------------
-- 3. PAIRING INVITES
-- -------------------------------------------------------------------------
create table public.pairing_invites (
    id                  uuid primary key default gen_random_uuid(),
    inviter_id          uuid not null references auth.users(id) on delete cascade,
    -- HANYA hash yang disimpan. Raw token tidak pernah menyentuh database.
    token_hash          text not null,
    status              text not null default 'PENDING'
                         check (status in ('PENDING', 'AWAITING_CONFIRMATION', 'ACTIVE', 'EXPIRED', 'REVOKED')),
    consumed_by         uuid references auth.users(id) on delete set null,
    attempt_count       int not null default 0,
    max_attempts        int not null default 5,
    expires_at          timestamptz not null,
    created_at          timestamptz not null default now()
);

create index pairing_invites_inviter_idx on public.pairing_invites (inviter_id);
create unique index pairing_invites_token_hash_idx on public.pairing_invites (token_hash);

alter table public.pairing_invites enable row level security;

-- User hanya boleh lihat invite miliknya sendiri (sebagai inviter) atau
-- yang sudah dia consume (sebagai consumer) — tidak pernah bisa
-- enumerasi invite milik orang lain.
create policy "pairing_invites_select_own"
    on public.pairing_invites for select
    using (auth.uid() = inviter_id or auth.uid() = consumed_by);

-- Semua write (create/consume/confirm/revoke) lewat Edge Function saja.
revoke insert, update, delete on public.pairing_invites from authenticated;

-- -------------------------------------------------------------------------
-- 4. SESSION AUDIT  (untuk fitur "Active Devices" di Privacy Center)
-- -------------------------------------------------------------------------
create table public.session_audit (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references auth.users(id) on delete cascade,
    device_label    text not null,        -- contoh: "Android 14 · Pixel 8" (bukan IMEI/serial)
    platform        text not null default 'ANDROID',
    created_at      timestamptz not null default now(),
    last_active_at  timestamptz not null default now(),
    revoked_at      timestamptz
);

alter table public.session_audit enable row level security;

create policy "session_audit_select_own"
    on public.session_audit for select
    using (auth.uid() = user_id);

create policy "session_audit_update_own_revoke_only"
    on public.session_audit for update
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

revoke insert, delete on public.session_audit from authenticated;

-- -------------------------------------------------------------------------
-- 5. HELPER FUNCTION dipakai policy tabel shared-data di phase berikutnya
-- -------------------------------------------------------------------------
-- Contoh pemakaian di tabel messages/calendar_events/dst nanti:
--   using (pair_id in (select id from public.my_active_pairs()))
create or replace function public.my_active_pairs()
returns setof uuid
language sql
stable
security definer
set search_path = public
as $$
    select id from public.pairs
    where status = 'ACTIVE'
      and (user_a_id = auth.uid() or user_b_id = auth.uid());
$$;

-- -------------------------------------------------------------------------
-- 6. FINALIZE PAIRING  (dipanggil via RPC HANYA oleh Edge Function confirm_pairing,
--    memakai service role — bukan dipanggil langsung oleh client)
-- -------------------------------------------------------------------------
create or replace function public.finalize_pairing(
    p_invite_id uuid,
    p_user_a uuid,
    p_user_b uuid
)
returns public.pairs
language plpgsql
security definer
set search_path = public
as $$
declare
    v_pair public.pairs;
begin
    -- Transaksi implisit per function call: insert pair + update invite
    -- sukses bersamaan, atau keduanya rollback.
    insert into public.pairs (user_a_id, user_b_id, status)
    values (p_user_a, p_user_b, 'ACTIVE')
    returning * into v_pair;

    update public.pairing_invites
    set status = 'ACTIVE'
    where id = p_invite_id;

    return v_pair;
end;
$$;

-- Hanya service_role yang boleh eksekusi (dipanggil dari Edge Function,
-- bukan dari client langsung).
revoke execute on function public.finalize_pairing from authenticated, anon;
grant execute on function public.finalize_pairing to service_role;

-- =========================================================================
-- CATATAN UNTUK PHASE BERIKUTNYA (belum dibuat di file ini):
--   messages, message_reactions, attachments, view_once_media, calls,
--   locations, location_history, moods, calendar_events, statuses,
--   digital_letters, todos, playlists, playlist_items, memories,
--   activities, shared_theme, animal_config, notification_settings.
-- Semua akan punya kolom `pair_id uuid references public.pairs(id)`
-- dan policy pola yang sama: `pair_id in (select id from my_active_pairs())`.
-- =========================================================================

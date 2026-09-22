-- ============================================================================
-- NazeVer — Initial database schema (TASK-003)
-- Ref: requirements.md (FR-01..FR-05, SR-02, SR-03), design.md (§3 Data Model)
--
-- Conventions:
--   * All primary/foreign keys are UUIDs (Supabase architecture).
--   * Timestamps are timestamptz, default now().
--   * Status/type columns are text + CHECK constraints (no custom enum types,
--     keeping migrations deterministic and re-runnable).
--   * No PostgreSQL extension is required: gen_random_uuid() is built into
--     PostgreSQL 13+ (Supabase runs PG15+). No pgcrypto needed.
--   * Soft deletes use tombstone columns (deleted_at) per design §6 (NFR-02).
--   * RLS is enabled on EVERY table holding private data (SR-02.1/2.3).
--     Authorization is enforced in the database, never only in the frontend.
--
-- Insert paths that require elevated trust (creating couples, pairing codes,
-- consuming pairing invites) are intentionally NOT granted to the
-- "authenticated" role: per design §4 they happen in Supabase Edge Functions
-- using the service_role key, which bypasses RLS. Clients get SELECT/UPDATE
-- policies only where ownership is verifiable (SR-02.2: no IDOR).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. updated_at maintenance helper
-- ----------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ----------------------------------------------------------------------------
-- 1. profiles (FR-01.5)
-- 1:1 with auth.users. Passwords live in Supabase Auth — never stored here.
-- ----------------------------------------------------------------------------
create table if not exists public.profiles (
  user_id      uuid primary key references auth.
users (id) on delete cascade,
  username     text not null check (char_length(username) between 3 and 32),
  display_name text not null check (char_length(display_name) between 1 and 64),
  avatar_url   text,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create unique index if not exists profiles_username_key
  on public.profiles (username);

create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

-- ----------------------------------------------------------------------------
-- 2. couples (FR-02, FR-11)
-- A couple is a private space for exactly two users (FR-02.8).
-- ----------------------------------------------------------------------------
create table if not exists public.couples (
  id                uuid primary key default gen_random_uuid(),
  couple_name       text check (couple_name is null or char_length(couple_name) <= 64),
  couple_avatar_url text,
  relationship_date date,
  wallpaper_url     text,
  wallpaper_version integer not null default 0,
  status            text not null default 'active'
                    check (status in ('active', 'unpaired')),
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now()
);

create trigger couples_set_updated_at
  before update on public.couples
  for each row execute function public.set_updated_at();

-- ----------------------------------------------------------------------------
-- 3. couple_members (design §3: max 2 rows enforced via trigger)
-- Membership is the root of ALL authorization (SR-02).
-- One user belongs to at most ONE couple at a time (FR-02.8, re-pair after
-- unpair is modelled by deleting membership rows, then inserting new ones).
-- ----------------------------------------------------------------------------
create table if not exists public.couple_members (
  couple_id  uuid not null references public.
couples (id) on delete cascade,
  user_id    uuid not null references auth.users (id) on delete cascade,
  role       text not null default 'member' check (role in ('member')),
  joined_at  timestamptz not null default now(),
  primary key (couple_id, user_id)
);

-- A user can only be in one couple at a time.
create unique index if not exists couple_members_user_id_key
  on public.couple_members (user_id);

create index if not exists couple_members_couple_id_idx
  on public.couple_members (couple_id);

-- Enforce "exactly two users max" (FR-02.8) at the database level.
create or replace function public.enforce_max_two_couple_members()
returns trigger
language plpgsql
as $$
declare
  member_count integer;
begin
  select count(*) into member_count
    from public.couple_members
   where couple_id = new.couple_id;
  if member_count >= 2 then
    raise exception 'couple % already has two members (FR-02.8)', new.couple_id;
  end if;
  return new;
end;
$$;

create trigger couple_members_max_two
  before insert on public.couple_members
  for each row execute function public.enforce_max_two_couple_members();

-- ----------------------------------------------------------------------------
-- 4. is_couple_member helper (TASK-003, SR-02.1/2.3)
-- Used by RLS policies everywhere. SECURITY DEFINER + pinned search_path.
-- Because the function owner (migration role = table owner) is not subject to
-- RLS (no FORCE ROW LEVEL SECURITY is used), this cannot recurse into the
-- couple_members policy, and cannot be abused to bypass RLS: it only returns
-- a boolean about the CALLER's own membership (auth.uid()), never data.
-- ----------------------------------------------------------------------------
create or replace function public.is_couple_member(target_couple_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
      from public.couple_members
     where couple_id = target_couple_id
       and user_id = aut
h.uid()
  );
$$;

-- Companion helper: is the target user my (one) partner right now?
-- Used by the profiles read policy so partners can see each other's profile.
create or replace function public.is_couple_partner(target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
      from public.couple_members me
      join public.couple_members other on me.couple_id = other.couple_id
     where me.user_id = auth.uid()
       and other.user_id = target_user_id
       and me.user_id <> other.user_id
  );
$$;

-- ----------------------------------------------------------------------------
-- 5. pairing_requests (FR-02)
-- The 6-digit code is generated and HASHED by the edge function
-- (design §2); only code_hash is stored — never the plaintext code (SR-03.4).
-- Client may only read requests addressed to/from them. All INSERT/UPDATE is
-- done by the pairing edge functions (service_role).
-- ----------------------------------------------------------------------------
create table if not exists public.pairing_requests (
  id           uuid primary key default gen_random_uuid(),
  requester_id uuid not null references auth.users (id) on delete cascade,
  invitee_id   uuid references auth.users (id) on delete cascade,
  couple_id    uuid references public.couples (id) on delete set null,
  code_hash    text not null,
  status       text not null default 'pending'
                 check (status in ('pending', 'accepted', 'rejected', 'expired', 'used')),
  expires_at   timestamptz not null,
  accepted_at  timestamptz,
  rejected_at  timestamptz,
  created_at   timestamptz not null default now()
);

create index if not exists pairing_requests_requester_idx
  on public.pairing_requests (requester_id);
create index if not exists pairing_requests_invitee_idx
  on public.pairing_requests (invitee_id);
create index if not exists pairing_requests_expires_idx
  on public.pairing_requests (expires_at);
create
 unique index if not exists pairing_requests_code_hash_active_key
  on public.pairing_requests (code_hash)
  where status = 'pending';

-- ----------------------------------------------------------------------------
-- 6. devices (FR-01.6) and 7. sessions (SR-01)
-- ----------------------------------------------------------------------------
create table if not exists public.devices (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users (id) on delete cascade,
  device_name    text not null,
  platform       text not null default 'android',
  last_active_at timestamptz not null default now(),
  fcm_token      text,
  created_at     timestamptz not null default now()
);

create index if not exists devices_user_id_idx on public.devices (user_id);

create table if not exists public.sessions (
  id                 uuid primary key default gen_random_uuid(),
  user_id            uuid not null references auth.users (id) on delete cascade,
  device_id          uuid references public.devices (id) on delete cascade,
  refresh_token_hash text not null,
  revoked_at         timestamptz,
  created_at         timestamptz not null default now()
);

create index if not exists sessions_user_id_idx on public.sessions (user_id);

-- ----------------------------------------------------------------------------
-- 8. conversations (FR-03)
-- Exactly ONE conversation per couple (single private space per design §2/§3;
-- unique(couple_id)). messages keep a direct couple_id column as per
-- design §3 so membership checks never require joins.
-- ----------------------------------------------------------------------------
create table if not exists public.conversations (
  id         uuid primary key default gen_random_uuid(),
  couple_id  uuid not null unique references public.couples (id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create trigger conversa
tions_set_updated_at
  before update on public.conversations
  for each row execute function public.set_updated_at();

-- ----------------------------------------------------------------------------
-- 9. messages (FR-03, design §3)
-- Tombstone deletes (deleted_at) per NFR-02; idempotency_key per design §2
-- (server rejects duplicate events — requirements §5).
-- ----------------------------------------------------------------------------
create table if not exists public.messages (
  id               uuid primary key default gen_random_uuid(),
  couple_id        uuid not null references public.couples (id) on delete cascade,
  conversation_id  uuid not null references public.conversations (id) on delete cascade,
  sender_id        uuid not null references auth.users (id) on delete cascade,
  type             text not null default 'text'
                     check (type in ('text', 'media', 'voice', 'location', 'note_ref')),
  body             text,
  reply_to_id      uuid references public.messages (id) on delete set null,
  status           text not null default 'sent'
                     check (status in ('pending', 'sent', 'delivered', 'read')),
  once_view        boolean not null default false,
  idempotency_key  uuid not null,
  edited_at        timestamptz,
  deleted_at       timestamptz,
  created_at       timestamptz not null default now()
);

-- One idempotency key per couple: retries cannot duplicate a message (§5).
create unique index if not exists messages_idempotency_key_key
  on public.messages (couple_id, idempotency_key);

create index if not exists messages_couple_created_idx
  on public.messages (couple_id, created_at desc);
create index if not exists messages_conversation_created_idx
  on public.messages (conversation_id, created_at desc);
create index if not exists messages_sender_idx
  on public.messages (sender_id);

-- ----------------------------------------------------------------------------
-- 10. media (FR-04, FR-05) and message_attac
hments (design §3)
-- Only METADATA lives here; binary content lives in Supabase Storage.
-- once_viewed_at drives server-side block of re-access (FR-05.3, edge fn).
-- ----------------------------------------------------------------------------
create table if not exists public.media (
  id            uuid primary key default gen_random_uuid(),
  couple_id     uuid not null references public.couples (id) on delete cascade,
  uploader_id   uuid not null references auth.users (id) on delete cascade,
  kind          text not null check (kind in ('photo', 'video', 'voice', 'document')),
  storage_path   text not null,
  thumb_path    text,
  size          bigint not null check (size >= 0),
  duration      interval,
  mime          text,
  once_viewed_at timestamptz,
  metadata      jsonb,
  created_at    timestamptz not null default now()
);

create index if not exists media_couple_created_idx
  on public.media (couple_id, created_at desc);

create table if not exists public.message_attachments (
  id         uuid primary key default gen_random_uuid(),
  message_id uuid not null references public.messages (id) on delete cascade,
  media_id   uuid not null references public.media (id) on delete cascade
);

create index if not exists message_attachments_message_idx
  on public.message_attachments (message_id);

-- ----------------------------------------------------------------------------
-- 11. privacy_settings (FR-10)
-- ----------------------------------------------------------------------------
create table if not exists public.privacy_settings (
  user_id           uuid primary key references auth.users (id) on delete cascade,
  show_online       boolean not null default true,
  show_last_seen    boolean not null default true,
  show_typing       boolean not null default true,
  show_read_receipt boolean not null default true,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now()
);

create trigger privacy_se
ttings_set_updated_at
  before update on public.privacy_settings
  for each row execute function public.set_updated_at();

-- ============================================================================
-- ROW LEVEL SECURITY (SR-02)
-- Enabled on every table. No policy uses USING (true). "Authenticated" is
-- never treated as "authorized": every policy verifies ownership or
-- membership via is_couple_member / is_couple_partner.
-- ============================================================================

alter table public.profiles          enable row level security;
alter table public.couples           enable row level security;
alter table public.couple_members   enable row level security;
alter table public.pairing_requests enable row level security;
alter table public.devices           enable row level security;
alter table public.sessions          enable row level security;
alter table public.conversations     enable row level security;
alter table public.messages          enable row level security;
alter table public.media             enable row level security;
alter table public.message_attachments enable row level security;
alter table public.privacy_settings  enable row level security;

-- profiles: read own or partner's; update only own.
create policy profiles_select on public.profiles
  for select to authenticated
  using (user_id = auth.uid() or public.is_couple_partner(user_id));

create policy profiles_update on public.profiles
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- profiles rows are created by a trigger/edge function on signup (service_role).

-- couples: only members can read or update their couple (SR-02.2: knowing
-- the UUID of another couple grants nothing). Creation happens in the pairing
-- edge function (service_role) — no client INSERT policy by design §4.
create policy couples_select on public.couples
  for select to authenticated
  using (public.is_couple_member(id));

create
  for update to authenticated
  using (public.is_couple_member(id))
  with check (public.is_couple_member(id));

-- couple_members: a user can see only their own membership row.
-- (Partner membership is exposed through helper functions, not raw rows.)
-- Insert/delete happens via the pairing edge function only.
create policy couple_members_select on public.couple_members
  for select to authenticated
  using (user_id = auth.uid());

-- pairing_requests: only requester or invitee can read their own requests.
-- Codes are created/consumed by the pairing edge function (service_role),
-- so there is no client INSERT/UPDATE policy (prevents forging invitees).
create policy pairing_requests_select on public.pairing_requests
  for select to authenticated
  using (requester_id = auth.uid() or invitee_id = auth.uid());

-- devices & sessions: strictly per-user (FR-01.6, SR-01).
create policy devices_select on public.devices
  for select to authenticated
  using (user_id = auth.uid());

create policy devices_insert on public.devices
  for insert to authenticated
  with check (user_id = auth.uid());

create policy devices_update on public.devices
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

create policy devices_delete on public.devices
  for delete to authenticated
  using (user_id = auth.uid());

create policy sessions_select on public.sessions
  for select to authenticated
  using (user_id = auth.uid());

-- conversations: members of the couple only (FR-02.8, SR-02.2).
create policy conversations_select on public.conversations
  for select to authenticated
  using (public.is_couple_member(couple_id));

-- messages:
--  * read: must be a member of the message's couple (direct couple_id check,
--    no join needed).
--  * insert: must be a member AND sender_id must be auth.uid() — a user can
--    never forge a message as someone else (SR-02.2), and the conversation
--    must bel
ong to the same couple (no cross-couple injection).
--  * update: only the sender (edit + tombstone soft delete, design §6).
--    WITH CHECK keeps the row valid after edit.
--  * delete: no policy — hard delete is reserved for service_role only.
create policy messages_select on public.messages
  for select to authenticated
  using (public.is_couple_member(couple_id));

create policy messages_insert on public.messages
  for insert to authenticated
  with check (
    sender_id = auth.uid()
    and public.is_couple_member(couple_id)
    and exists (
      select 1 from public.conversations c
       where c.id = conversation_id
         and c.couple_id = couple_id
    )
  );

create policy messages_update on public.messages
  for update to authenticated
  using (sender_id = auth.uid() and public.is_couple_member(couple_id))
  with check (sender_id = auth.uid() and public.is_couple_member(couple_id));

-- media: visible to couple members only (FR-04.10 shared gallery).
-- Uploads are written by client with storage metadata; uploader must be a
-- member and media must belong to their couple.
create policy media_select on public.media
  for select to authenticated
  using (public.is_couple_member(couple_id));

create policy media_insert on public.media
  for insert to authenticated
  with check (uploader_id = auth.uid() and public.is_couple_member(couple_id));

create policy media_update on public.media
  for update to authenticated
  using (public.is_couple_member(couple_id))
  with check (public.is_couple_member(couple_id));

-- message_attachments: follow the parent message's couple.
create policy message_attachments_select on public.message_attachments
  for select to authenticated
  using (
    exists (
      select 1 from public.messages m
       where m.id = message_id
         and public.is_couple_member(m.couple_id)
    )
  );

create policy message_attachments_insert on public.message_attachments
  for insert to authenticated
  with check (
    exists (
      select
       where m.id = message_id
         and m.sender_id = auth.uid()
         and public.is_couple_member(m.couple_id)
    )
  );

-- privacy_settings: strictly per-user (FR-10).
create policy privacy_settings_select on public.privacy_settings
  for select to authenticated
  using (user_id = auth.uid());

create policy privacy_settings_insert on public.privacy_settings
  for insert to authenticated
  with check (user_id = auth.uid());

create policy privacy_settings_update on public.privacy_settings
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- Supabase public API lockdown for project zzhmvoudkavrywmepqqp.
--
-- Run this in Supabase Dashboard > SQL Editor.
-- The Spring backend uses a server-side Postgres connection, so this blocks
-- anon/authenticated API role access without changing backend JDBC access.

begin;

do $$
declare
    table_record record;
begin
    for table_record in
        select tablename
        from pg_tables
        where schemaname = 'public'
    loop
        execute format('alter table public.%I enable row level security', table_record.tablename);
        execute format('revoke all privileges on table public.%I from anon, authenticated', table_record.tablename);
    end loop;
end $$;

revoke all privileges on schema public from anon, authenticated;
revoke all privileges on all tables in schema public from anon, authenticated;
revoke all privileges on all sequences in schema public from anon, authenticated;
alter default privileges in schema public revoke all on tables from anon, authenticated;
alter default privileges in schema public revoke all on sequences from anon, authenticated;

commit;

select
    schemaname,
    tablename,
    rowsecurity as rls_enabled
from pg_tables
where schemaname = 'public'
order by tablename;

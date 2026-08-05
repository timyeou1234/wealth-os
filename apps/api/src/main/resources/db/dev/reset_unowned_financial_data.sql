-- Development only. This intentionally removes legacy financial data that has no owner.
-- Run it explicitly before migrations V10-V12, then re-import through an authenticated user.
begin;

delete from snapshot_asset_positions;
delete from snapshot_liability_positions;
delete from snapshots;
delete from assets;
delete from liabilities;

commit;

# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 13
- Branch: release/v1.5.3
- Commit: 985a861
- Time: 2026-11-23T18:18:38+01:00
- Deviations: none

## Where we are
v1.5.3 (versionCode 11) is ready on release/v1.5.3 and is being merged into master locally. It holds master's four reliability fixes, which were device-tested earlier: widget hardening, the 48 h change cap edge, the snapshot fix and the BootReceiver re-arm. The changelog (changelogs/11.txt, 374 bytes) uses the text Oliver agreed. Gate: compileDebugKotlin, testDebugUnitTest --rerun (216 tests, 0 failures) and assembleRelease all pass; R8 minification and resource shrinking are on for release. The in-app What's new card is unchanged and still shows the v1.5.2 notes, as designed for releases that add no notes of their own.

## Blocked on you
Tag v1.5.3 on the release merge commit and push the tag, after pushing master.

## Red flags
None

## Pending decisions
None

## Next step for chat
Push master, then tag v1.5.3 and push the tag.

## Unapplied ROADMAP UPDATE blocks
None

# Privamatic — Claude Code rules

## Project
- Package `com.techtrest.privamatic`; debug build `com.techtrest.privamatic.debug`.
- Path: `"/home/techtresthome/Local App Projects/Privamatic"` — always quote (spaces).
- Repo `techtrest/privamatic`, default branch `master`.
- Sources of truth:
  - `CONVENTIONS.md` — code standards. Follow it; don't restate it here.
  - `docs/specs/<feature>.md` — scope, one spec per feature.
  - `STATE.md` — progress and evidence.

## Session
- Very first, before other work, check `docs/roadmap-inbox.md` (local, gitignored):
  - If it holds ROADMAP UPDATE blocks: apply them to ROADMAP.md per `~/Documents/project-tracker/bootstrap/contract.md`,
    update "Last updated", remove the applied blocks from the inbox, and list every change under **Changed** in the report.
    CHANGE / PARK / DONE lines need not match verbatim: match each to the single roadmap item it clearly refers to
    (same subject; the quote may be partial or paraphrased), apply it, and report "matched '<quote>' → line N".
    A line that matches no item, or more than one, stays in the inbox and goes under **Blocked on me**.
    A DONE for an item that is already ticked is not an error: drop it from the inbox and mention it.
  - If it is empty, ask Oliver once: "Any decisions from chats since the roadmap was last updated (<date from ROADMAP.md>)?
    Paste a ROADMAP UPDATE block, or say none." Then continue with the session's actual task.
- At start, read CLAUDE.md, STATE.md and the relevant spec.
- Investigate before changing code. Record findings in STATE.md.
- Make minimal changes. No rewrites.
- STATE.md is the task list: tick items with evidence, add what you find.
- Keep STATE.md under ~150 lines: move older runs to `docs/state-archive/YYYY-MM.md`.

## Keep going / stop
- When a step doesn't need Oliver, keep going. Put status notes in the same message as the next action.
- Never end with "want me to continue?".
- Stop only when:
  1. you can't continue without Oliver;
  2. before anything destructive, or anything outside this repository;
  3. at a STOP point in the spec.
- Spec items marked "Oliver's call": build the stated default, report it under Found.

## Park, don't patch
- If a fix round creates new problems, stop after the second round.
- Propose a different approach instead of patching again.

## Evidence
- Reading code is a hypothesis, not evidence.
- A change is done only when its gate passes on a running build.
- Mark anything you could not verify on a device, and say why.
- Never say "fixed" without device evidence.

## Run types
- **Implementation run**: change code per the spec, verify on the Pixel 8, update STATE.md.
- **Read-only design run**: investigate, write a decision paper in `docs/decisions/`, change no code.
- **Release run**:
  - May edit `versionCode`, `versionName`, the fastlane changelog and descriptions, once Oliver has agreed the text.
  - Verifies `assembleRelease`.
  - Then STOPS and hands Oliver the tag commands. Claude never tags.

## Git
- One feature branch per change. Never commit on `master`.
- Order: `git diff` → `git status` → `git add` → `git commit`.
- Prefixes `feat:` / `fix:` / `chore:` / `release:`. Messages say why.
- Before merging:
  - Review the diff against `master`.
  - List only problems you'd block the merge for: file, line, why, how to show it fails.
  - Fix them, re-run the gate, merge locally with `git merge --no-ff`.
- Never push, never tag, never `git clean -f`, never `git reset --hard` on `master`. Oliver pushes.

## External review
- Applies to every feature-sized run, and always to risky code (list below). Test-only and copy-only runs skip it.
- In review file names, `<branch>` is the branch name with `/` replaced by `-`
  (`fix/backup-exclusions` → `fix-backup-exclusions.diff`).
- After your own diff review, write:
  - `docs/reviews/<branch>.diff` (`git diff master...<branch>`);
  - `docs/reviews/<branch>-prompt-<round>.md`: `docs/reviews/REVIEW_PROMPT.md` filled in with a one-line summary,
    context from earlier rounds and 2–4 run-specific invariants.
- Then STOP at "external review". Oliver runs it in Cursor with a non-Claude model and saves `docs/reviews/<branch>-findings-<round>.md`.
- Every finding is a claim. Before fixing it, prove it with a failing test or a reproducible device check.
  Or reject it, with reasons recorded in the findings file. A rejected High goes into the next round's prompt for judgement.
- Stop review rounds when a round has no High findings; then merge.

## GitHub
- `gh` is read-only.
- Never comment on, close or create issues, PRs or releases.
- Oliver posts replies, and only after the F-Droid build is live.

## Devices
- Every adb call uses `-s <serial>`. Never bare `adb` (only `adb devices` / `adb devices -l` are fine).
- The serials are in `.claude/devices.local` (gitignored, one per machine) as `TEST_DEVICE_SERIAL` (Pixel 8) and `DAILY_PHONE_SERIAL` (Pixel 9).
  - At session start, read that file and type its values literally into adb commands; the guard hook can't resolve variables.
  - Never write a serial into a tracked file. Use the role name instead, also in evidence and in pasted `adb devices` output.
  - If the file is missing or a serial is empty, the hook denies every adb command that targets a device. Create it with these two
    `NAME=value` lines; Claude's writes to it need Oliver's confirmation.
- **Pixel 8** — test device. Free for builds, installs, tests, UI automation.
- **Pixel 9** — Oliver's daily phone, GrapheneOS, real score history.
  - Use only when Oliver asks in this run.
  - Allowed: `screencap`, app-filtered `logcat`, `getprop`, `dumpsys package`.
  - When asked: install the debug build with `install --user 0`.
  - Never: `uninstall`, `pm clear`, `rm`, `run-as`, `input`, `settings put`, `pm disable`.
  - Never touch `com.techtrest.privamatic` (the F-Droid install).
  - If it shows `offline` or `unauthorized`: stop and ask Oliver to unlock it. No retry loops.
- **Any other or unknown device**: treat as protected, ask first.

## Secrets
- Never read or print keystores, `local.properties` values, `gradle.properties` values,
  or env vars named `*PASSWORD*` / `*KEY*` / `*SECRET*` / `*TOKEN*`. Report names only.
- No `ssh` / `scp` / `rsync` — no server belongs to this project.

## Build & test
Use these exact commands. They match the allow list in `.claude/settings.local.json`; other forms prompt.
JDK is always `/home/techtresthome/.local/share/jdk/temurin-21` (JDK 25 breaks Gradle 8.10).

```
JAVA_HOME=/home/techtresthome/.local/share/jdk/temurin-21 ./gradlew :app:compileDebugKotlin
JAVA_HOME=/home/techtresthome/.local/share/jdk/temurin-21 ./gradlew :app:testDebugUnitTest --rerun
JAVA_HOME=/home/techtresthome/.local/share/jdk/temurin-21 ./gradlew :app:assembleDebug
JAVA_HOME=/home/techtresthome/.local/share/jdk/temurin-21 ./gradlew :app:assembleRelease
```
- Run `compileDebugKotlin` before every commit.
- Unit tests always run with `--rerun`. Without it Gradle may report UP-TO-DATE and run nothing.
- The "SDK XML version 4" warning is harmless.
- Install on the Pixel 8:
  `adb -s <test serial from .claude/devices.local> install -r app/build/outputs/apk/debug/app-debug.apk`
- Before judging any UI change, force-stop, then cold-start:
  `adb -s <test serial from .claude/devices.local> shell am force-stop com.techtrest.privamatic.debug`
  `adb -s <test serial from .claude/devices.local> shell am start -n com.techtrest.privamatic.debug/com.techtrest.privamatic.MainActivity`
- Backup tests: after `pm clear` the app is stopped, and Android won't back it up ("Backup is not allowed").
  Run `pm unstop` (or launch the app) before `bmgr backupnow`.

## Risky code — needs the extra review once it's set up
- Room database and migrations (`PrivacyDatabase`: version 1, `exportSchema = false`, no migrations yet).
- DataStore and SharedPreferences keys and file names.
- Score history (`score_history`, `privacy_history.db`).
- Trusted list (`trusted_apps_prefs`).
- Backup and data-extraction rules (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).

## End of run
End every run with three headings:
- **Blocked on me**
- **Changed**
- **Found**: ends with a "Roadmap:" line summarising what changed in ROADMAP.md (or "Roadmap: unchanged").

At the end of every run, per `~/Documents/project-tracker/bootstrap/contract.md`:
- Update `ROADMAP.md` (gitignored, local only): tick, add, move items, and set the "Last updated" date.
- Keep STATE.md's `## Next` and `## Blocked on me` current.

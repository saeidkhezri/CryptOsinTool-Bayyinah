# BAYYINAH / بیِّنة — FINAL HARDENING EDITS PACKAGE

## Purpose

This package is the consolidated hardening pass following EDITS 01.

It addresses the observed post-replacement failure scenario and the remaining high-risk areas that can be repaired without redesigning the whole application:

1. Prevent the heavy Investigation Workspace from mounting while the acquisition pipeline is still running.
2. Make the active investigation object the reactive source for the displayed case.
3. Harden the on-chain → off-chain handoff coroutine so an exception there cannot escape as an uncaught ViewModel coroutine failure.
4. Move handoff computation off the UI dispatcher.
5. Remove the nested full-size Box/LazyColumn structure from the forensic progress dialog.
6. Prevent invalid progress values from reaching the progress indicator.
7. Remove remaining nullable assertions in several frequently used UI paths.
8. Fix the RTL temporal-analysis heading constraint that caused Persian characters to stack vertically.
9. Stop treating zero balance as proof of a dead-end when transaction discovery has not actually been confirmed.
10. Remove destructive migration fallback from the OSINT cache by using an explicit schema-preserving version migration.
11. Remove synthetic offline-database byte generation and require a real HTTPS dataset source plus SHA-256 verification.
12. Remove fake storage-capacity defaults and initialize storage telemetry from the real device.
13. Prevent secure-storage failure from falling back to plaintext Base64.
14. Disable cleartext HTTP at the Android application level.

## Important

These files are intended to REPLACE the corresponding existing files at the exact project-relative paths listed in TARGET_MAP.csv.

The Markdown and CSV files in this package are control documents only. They must NOT be copied into the Android source tree.

Do not modify unrelated project files during the replacement operation.

## Verification status

This environment does not provide a complete Android Gradle build environment for the supplied project. Therefore this package must not be described as independently build-verified until Google AI Studio or Android Studio actually runs the build and tests.

## Manual smoke test after replacement

1. Launch the application.
2. Open New Investigation.
3. Enter a valid Bitcoin public address.
4. Start Guided Investigation.
5. Confirm that only the bounded progress dialog is visible during acquisition.
6. Confirm that the application remains responsive.
7. Confirm that provider failures appear as errors/partial results rather than crashes.
8. Confirm that a zero-balance address is not automatically treated as a dead-end unless discovery explicitly confirms an empty ledger.
9. Confirm that the investigation workspace opens only after the running operation finishes.
10. Open the temporal-analysis screen in Persian on a compact-width device and confirm that the title does not stack one character per line.
11. Open the graph and test node selection/path tools.
12. Open API Settings and exercise validation/help dialogs.
13. Open an existing case from history.
14. Reopen the application after closing it and verify that the local databases do not disappear because of a destructive fallback.

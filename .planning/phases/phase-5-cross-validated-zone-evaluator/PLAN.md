# Phase 5 Plan: Cross-Validated Zone Evaluator

## Goal

Check whether the offline Wi-Fi zone evaluator generalizes across 30-second
buckets instead of only matching fingerprints built from the same data.

## Scope

- Add leave-one-bucket-out cross-validation to the analyzer.
- Export `zone-cross-validation-evaluation.csv`.
- Export `zone-cross-validation-predictions.csv`.
- Compare naive same-data accuracy with cross-validated accuracy.
- Document the result and implication for runtime detection.

## Verification

- Run analyzer on the 2026-05-19 10:45 controlled run with named windows.
- Confirm cross-validation outputs are generated.
- Record accuracy and misclassification pattern.

## Out Of Scope

- No Android app code changes.
- No runtime detection logic.
- No threshold tuning in gameplay.

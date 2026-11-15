#!/usr/bin/env bash
# Runs your code against the visible tests plus the hidden tests from the `solution` branch.
# Your working copy is not changed.  Usage: ./grade.sh
set -euo pipefail
repo="$(cd "$(dirname "$0")" && pwd)"
ref=solution
git -C "$repo" rev-parse -q --verify "$ref" > /dev/null || ref=origin/solution
git -C "$repo" rev-parse -q --verify "$ref" > /dev/null || { echo "No 'solution' branch found. Run: git fetch origin solution"; exit 1; }
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
rsync -a --exclude target --exclude .git "$repo/" "$work/"
# only the Hidden*Test files; your own tests stay as they are
git -C "$repo" archive "$ref" src/test/java/com/example/booking | tar -x -C "$work" --include='*/Hidden*'
cd "$work"
if ./mvnw test > out.log 2>&1; then
  grep -E "^\[INFO\] Tests run:" out.log | tail -1
  echo "All tests passed"
else
  # compile errors, failing tests and the totals; a startup error shows as every test erroring
  grep -E "COMPILATION ERROR|\.java:\[[0-9]+|<<< (FAILURE|ERROR)!|expected|but was|^\[ERROR\] Tests run:" out.log | head -80 || true
  grep -m1 -E "^Caused by: .*(Exception|Error):" out.log | cut -c1-300 || true
  echo "NOT all tests passed"
  exit 1
fi

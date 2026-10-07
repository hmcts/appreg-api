#!/usr/bin/env python3
"""Read-only CSDS backup comparison. Uses only Python's standard library and psql."""

import argparse
import csv
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

TABLES = {
    "application_codes": ("ac_id", "changed_date"),
    "resolution_codes": ("rc_id", "changed_date"),
    "fee": ("fee_id", "fee_changed_date"),
    "standard_applicants": ("sa_id", "changed_date"),
    "national_court_houses": ("nch_id", "changed_date"),
}

DATE_COLUMNS = {
    "application_codes": ("application_code_start_date", "application_code_end_date"),
    "resolution_codes": ("resolution_code_start_date", "resolution_code_end_date"),
    "fee": ("fee_start_date", "fee_end_date"),
    "standard_applicants": (),
    "national_court_houses": ("start_date", "end_date"),
}


def query(schema, table):
    if not re.fullmatch(r"[a-z_][a-z0-9_]*", schema):
        raise ValueError("Schema must contain only lowercase letters, digits and underscores.")
    key, stamp = TABLES[table]
    current = f'"{schema}"."{table}"'
    backup = f'"{schema}"."{table}_staging"'
    dates = DATE_COLUMNS[table]
    date_names = ", ".join(f"'{name}'" for name in dates) or "NULL"
    def row_json(alias):
        # Promote dates to midnight; never truncate a staging timestamp's time component.
        fields = ", ".join(f"'{name}', {alias}.{name}::timestamp" for name in dates)
        return f"(to_jsonb({alias}) || jsonb_build_object({fields}))" if dates else f"to_jsonb({alias})"
    current_json, backup_json = row_json("c"), row_json("b")
    # Allow only the known date/timestamp mismatch in legacy staging schemas.
    return f"""
DO $check$
BEGIN
  IF EXISTS (
    SELECT 1 FROM (
      SELECT attname, atttypid, atttypmod FROM pg_attribute
      WHERE attrelid = '{current}'::regclass AND attnum > 0 AND NOT attisdropped
    ) c FULL JOIN (
      SELECT attname, atttypid, atttypmod FROM pg_attribute
      WHERE attrelid = '{backup}'::regclass AND attnum > 0 AND NOT attisdropped
    ) b USING (attname)
    WHERE (c.atttypid IS DISTINCT FROM b.atttypid
       OR c.atttypmod IS DISTINCT FROM b.atttypmod)
      AND NOT coalesce((c.attname IN ({date_names})
        AND c.atttypid IN ('date'::regtype, 'timestamp'::regtype)
        AND b.atttypid IN ('date'::regtype, 'timestamp'::regtype)), false)
      AND NOT coalesce((c.atttypid = 'numeric'::regtype
        AND b.atttypid = 'numeric'::regtype), false)
  ) THEN RAISE EXCEPTION 'Column mismatch for {table}'; END IF;
  IF EXISTS (SELECT 1 FROM {current} GROUP BY {key}
             HAVING {key} IS NULL OR count(*) > 1)
     OR EXISTS (SELECT 1 FROM {backup} GROUP BY {key}
                HAVING {key} IS NULL OR count(*) > 1)
  THEN RAISE EXCEPTION 'Null or duplicate keys for {table}'; END IF;
END $check$;
WITH compared AS (
  SELECT coalesce(c.{key}, b.{key}) AS key,
         CASE WHEN b.{key} IS NULL THEN 'added'
              WHEN c.{key} IS NULL THEN 'removed'
              WHEN ({current_json} - '{stamp}') IS DISTINCT FROM
                   ({backup_json} - '{stamp}') THEN 'changed'
              ELSE 'unchanged' END AS status,
         {backup_json} AS before, {current_json} AS after,
         b.{stamp} AS before_timestamp, c.{stamp} AS after_timestamp
  FROM {current} c FULL JOIN {backup} b USING ({key})
)
SELECT jsonb_build_object(
  'table', '{table}', 'timestamp_column', '{stamp}',
  'counts', jsonb_build_object(
    'added', count(*) FILTER (WHERE status = 'added'),
    'removed', count(*) FILTER (WHERE status = 'removed'),
    'changed', count(*) FILTER (WHERE status = 'changed'),
    'unchanged', count(*) FILTER (WHERE status = 'unchanged')),
  'rows', coalesce(jsonb_agg(to_jsonb(compared) ORDER BY key)
                  FILTER (WHERE status <> 'unchanged'), '[]'::jsonb)
) FROM compared;
"""


def fetch(schema, tables):
    sql = "BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;\n"
    sql += "SET LOCAL statement_timeout = '120s';\n"
    sql += "SET LOCAL TIME ZONE 'UTC';\n"
    sql += "".join(query(schema, table) for table in tables) + "COMMIT;\n"
    result = subprocess.run(
        ["psql", "-X", "-qAt", "-w", "-v", "ON_ERROR_STOP=1"],
        input=sql, text=True, capture_output=True, check=False,
    )
    if result.returncode:
        # Surface only our controlled validation errors, never arbitrary database diagnostics.
        for table in tables:
            for reason in ("Column mismatch", "Null or duplicate keys"):
                if f"{reason} for {table}" in result.stderr:
                    raise ValueError(f"{reason} for {table}; no report was published.")
        # Do not echo database diagnostics: they can contain connection/auth details.
        raise ValueError("Database comparison failed. Check connectivity, permissions, table/column "
                         "compatibility and unique non-null keys. No report was published.")
    reports = [json.loads(line) for line in result.stdout.splitlines() if line.strip()]
    if [report["table"] for report in reports] != list(tables):
        raise ValueError("Incomplete database response; no report was published.")
    return reports


def details(report):
    for row in report["rows"]:
        before, after = row["before"], row["after"]
        for column in sorted(set(before or {}) | set(after or {})):
            if column == report["timestamp_column"]:
                continue
            old = before[column] if before is not None else None
            new = after[column] if after is not None else None
            if row["status"] == "changed" and old == new:
                continue
            yield [report["table"], row["status"], row["key"], column,
                   json.dumps(old, ensure_ascii=False), json.dumps(new, ensure_ascii=False),
                   row["before_timestamp"] or "", row["after_timestamp"] or ""]


def cell(value):
    # Escape markup and table delimiters; JSON encoding keeps null/empty/whitespace distinct.
    return (str(value).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("|", "&#124;")
            .replace("`", "&#96;").replace("\n", "<br>"))


def write_report(reports, destination, schema):
    headers = ["Table", "Change", "Key", "Column", "Before", "After",
               "Previous import timestamp", "Current import timestamp"]
    with (destination / "details.csv").open("w", newline="", encoding="utf-8") as csv_file, \
         (destination / "report.md").open("w", encoding="utf-8") as md:
        writer = csv.writer(csv_file)
        writer.writerow(headers)
        md.write(f"# CSDS differences\n\nGenerated: {datetime.now(timezone.utc).isoformat()}\n\n")
        md.write(f"Schema: {cell(schema)}. Baseline: corresponding `_staging` tables.\n\n")
        md.write("Import timestamps are shown but excluded from comparison. Business dates are compared "
                 "as midnight timestamps to match legacy staging types (non-midnight times are preserved). "
                 "Timestamp values are database change stamps, not proof of a particular import run. "
                 "Timezone-aware values are rendered in UTC; timezone-naive values retain their stored value.\n\n")
        md.write("| Table | Added | Removed | Changed | Unchanged | Ignored column |\n")
        md.write("| --- | ---: | ---: | ---: | ---: | --- |\n")
        for report in reports:
            counts = report["counts"]
            md.write("| " + " | ".join(map(cell, [report["table"], counts["added"],
                     counts["removed"], counts["changed"], counts["unchanged"],
                     report["timestamp_column"]])) + " |\n")
        for report in reports:
            md.write(f"\n## {report['table']}\n\n")
            if not report["rows"]:
                md.write("No business-data differences.\n")
                continue
            md.write("| " + " | ".join(headers) + " |\n")
            md.write("| " + " | ".join(["---"] * len(headers)) + " |\n")
            for row in details(report):
                writer.writerow(row)
                md.write("| " + " | ".join(map(cell, row)) + " |\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__, epilog=(
        "Run through csds_diff.sh to load csds.config (or CSDS_CONFIG_FILE). "
        "Requires Python 3 and psql. Use --md to display Markdown only when mcat is installed. "
        "Exit 0 means report created, even when differences exist. "
        "Reports may contain sensitive reference data; store and share appropriately."))
    parser.add_argument("--table", choices=TABLES, help="Compare one table (default: all five)")
    parser.add_argument("--schema", default=os.environ.get("CSDS_DB_SCHEMA", "appreg"))
    parser.add_argument("--output-dir", type=Path,
                        default=Path(__file__).resolve().parents[3] / "data" / "csds-diff-reports",
                        help="Report parent directory (default: appreg/data/csds-diff-reports)")
    parser.add_argument("--md", action="store_true",
                        help="Display Markdown only if mcat is installed")
    parser.add_argument("--self-check", action="store_true", help="Run offline checks; no database needed")
    args = parser.parse_args()
    if args.self_check:
        self_check()
        return
    os.umask(0o077)
    tables = [args.table] if args.table else list(TABLES)
    reports = fetch(args.schema, tables)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    # Unique private directory avoids overwriting earlier reports. Only publish when complete.
    with tempfile.TemporaryDirectory(prefix=".csds-diff-", dir=args.output_dir) as working:
        destination = Path(working)
        write_report(reports, destination, args.schema)
        target = args.output_dir / (datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ-")
                                   + destination.name.removeprefix(".csds-diff-"))
        destination.rename(target)
    print(f"Report: {target / 'report.md'}\nCSV: {target / 'details.csv'}", flush=True)
    if args.md:
        import shutil
        if shutil.which("mcat"):
            try:
                with (target / "report.md").open("rb") as markdown:
                    subprocess.run(["mcat"], stdin=markdown, check=True)
            except (OSError, subprocess.CalledProcessError):
                print("Could not display report through mcat; saved files are still available above.", file=sys.stderr)


def self_check():
    from unittest.mock import patch

    row = {"key": 1, "status": "changed", "before": {"value": None, "changed_date": "old"},
           "after": {"value": "", "changed_date": "new"},
           "before_timestamp": "old", "after_timestamp": "new"}
    report = {"table": "application_codes", "timestamp_column": "changed_date",
              "counts": {"added": 0, "removed": 0, "changed": 1, "unchanged": 0}, "rows": [row]}
    assert list(details(report))[0][3:] == ["value", "null", '""', "old", "new"]
    row["after"]["value"] = None
    assert not list(details(report))  # Timestamp-only difference.
    row["after"]["value"] = " "
    assert len(list(details(report))) == 1
    for status, missing in [("added", "before"), ("removed", "after")]:
        copy = {**row, "status": status, missing: None}
        assert len(list(details({**report, "rows": [copy]}))) == 1
    assert cell("<x>|`\n") == "&lt;x&gt;&#124;&#96;<br>"
    for table in TABLES:
        sql = query("appreg", table)
        assert "FULL JOIN" in sql and "IS DISTINCT FROM" in sql
        assert "Null or duplicate keys" in sql and "Column mismatch" in sql
    try:
        query("bad;schema", "fee")
        raise AssertionError("Unsafe schema accepted")
    except ValueError:
        pass
    with tempfile.TemporaryDirectory() as directory:
        write_report([report, {**report, "rows": []}], Path(directory), "appreg")
        assert "No business-data differences" in (Path(directory) / "report.md").read_text()
        with (Path(directory) / "details.csv").open() as stream:
            assert len(list(csv.reader(stream))) == 2
    with patch("subprocess.run") as run:
        run.return_value = subprocess.CompletedProcess([], 0, json.dumps(report) + "\n", "")
        assert fetch("appreg", ["application_codes"]) == [report]
        assert "REPEATABLE READ READ ONLY" in run.call_args.kwargs["input"]
        for status, output in [(1, ""), (0, "")]:
            run.return_value = subprocess.CompletedProcess([], status, output, "")
            try:
                fetch("appreg", ["application_codes"])
                raise AssertionError("Failed database response accepted")
            except ValueError:
                pass
    # Exercise the CLI publication path without a database or persistent output.
    import io
    with tempfile.TemporaryDirectory() as directory, patch("sys.stdout", new_callable=io.StringIO), \
         patch("sys.argv", ["csds_diff.py", "--table", "application_codes", "--output-dir", directory]), \
         patch.dict(globals(), {"fetch": lambda schema, tables: [report]}), \
         patch("shutil.which", return_value="/test/mcat") as which, \
         patch("subprocess.run") as render:
        main()
        render.assert_not_called()
        which.assert_not_called()
        sys.argv.append("--md")
        main()
        assert render.call_args.args == (["mcat"],)
        assert Path(render.call_args.kwargs["stdin"].name).read_text().startswith("# CSDS differences")
        assert len(list(Path(directory).glob("*/report.md"))) == 2
        assert len(list(Path(directory).glob("*/details.csv"))) == 2
        assert not list(Path(directory).glob(".csds-diff-*"))
        for failure in (FileNotFoundError(), subprocess.CalledProcessError(1, "mcat")):
            render.side_effect = failure
            with patch("sys.stderr", new_callable=io.StringIO) as warning:
                main()
                assert "saved files are still available" in warning.getvalue()
        assert len(list(Path(directory).glob("*/report.md"))) == 4
        which.return_value = None
        render.reset_mock()
        with patch("sys.stdout", new_callable=io.StringIO) as output:
            main()
            assert "# CSDS differences" not in output.getvalue()
            assert "Report:" in output.getvalue()
        render.assert_not_called()
    print("CSDS diff offline checks passed.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError) as error:
        # File/connection exceptions can contain private paths or values; keep diagnostics bounded.
        message = str(error) if isinstance(error, ValueError) and not isinstance(error, json.JSONDecodeError) \
            else "Unable to generate report; check psql, database schema and output directory access."
        print(message, file=sys.stderr)
        sys.exit(1)

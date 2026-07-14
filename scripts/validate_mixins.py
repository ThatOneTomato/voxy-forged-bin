#!/usr/bin/env python3
"""Validate that every mixin declared in a *.mixins.json config is actually
present in the built JAR (catches phantom references that would throw
ClassNotFoundException at runtime).

Pure stdlib (json + zipfile) so it runs the same on Windows, macOS and Linux —
no jq, no bash, no WSL. Replaces the old validate_mixins.sh, which silently
"failed" wherever jq wasn't installed.
"""
import glob
import json
import os
import sys
import zipfile

# Best-effort UTF-8 stdout so the ✓/✗ glyphs don't blow up on Windows' cp1252.
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

OK = "✓"   # ✓
BAD = "✗"  # ✗

# Non-distributable jars we should never validate against.
_SKIP = ("-sources.jar", "-javadoc.jar", "-dev.jar", "-dev-shadow.jar", "-shadow.jar", "-slim.jar")


def find_jar():
    jars = [j for j in glob.glob("build/libs/*.jar") if not j.endswith(_SKIP)]
    if not jars:
        return None
    return max(jars, key=os.path.getmtime)


def main():
    print("=== MIXIN CONFIGURATION VALIDATION ===\n")

    jar = find_jar()
    if not jar:
        print(f"{BAD} No JAR file found in build/libs/ — run './gradlew build' first")
        return 1
    print(f"Validating JAR: {os.path.basename(jar)}\n")

    try:
        jar_entries = set(zipfile.ZipFile(jar).namelist())
    except Exception as e:
        print(f"{BAD} Could not read JAR: {e}")
        return 1

    configs = sorted(glob.glob("src/main/resources/**/*.mixins.json", recursive=True))
    if not configs:
        print("WARNING: No mixin configuration files found")
        return 0

    total = 0
    errors = 0
    for cfg in configs:
        name = os.path.basename(cfg)
        print(f"Checking {name}...")
        try:
            with open(cfg, encoding="utf-8") as fh:
                data = json.load(fh)
        except (OSError, ValueError) as e:
            print(f"  {BAD} Invalid JSON: {e}")
            errors += 1
            continue

        package = data.get("package")
        if not package:
            print(f"  {BAD} Missing package declaration")
            errors += 1
            continue

        pkg_path = package.replace(".", "/")
        for array_type in ("mixins", "client", "server"):
            entries = data.get(array_type) or []
            if not entries:
                continue
            print(f"  Validating .{array_type}[]...")
            for mixin in entries:
                total += 1
                class_path = f"{pkg_path}/{mixin.replace('.', '/')}.class"
                if class_path in jar_entries:
                    print(f"    {OK} {mixin}")
                else:
                    print(f"    {BAD} {mixin}")
                    print(f"      Expected: {class_path}")
                    print(f"      Status: NOT FOUND IN JAR")
                    errors += 1
        print()

    print("=== VALIDATION SUMMARY ===")
    print(f"Total mixins checked: {total}")
    print(f"JAR file: {os.path.basename(jar)}")
    print(f"JAR size: {os.path.getsize(jar) // (1024 * 1024)}M\n")

    if errors == 0:
        print(f"{OK} ALL CHECKS PASSED")
        print("All mixin references are valid and present in JAR")
        return 0

    print(f"{BAD} VALIDATION FAILED")
    print(f"Found {errors} phantom mixin reference(s)\n")
    print("These mixins are declared in JSON configs but missing from the JAR.")
    print("This will cause ClassNotFoundException at runtime.\n")
    print("Common causes:")
    print("  1. Mixin class excluded from compilation (check build.gradle sourceSets)")
    print("  2. Mixin class deleted but not removed from JSON")
    print("  3. Typo in mixin class name")
    return 1


if __name__ == "__main__":
    sys.exit(main())

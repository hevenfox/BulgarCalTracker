#!/usr/bin/env python3
"""Smoke-test the Room 3 -> 4 migration without a device.

The ALTER statements are read out of AppDatabase.kt itself (never duplicated
here), applied to a SQLite database built with the version 3 schema, and checked:
existing food, diary, weight and water rows must survive and the new columns must
come back as 0 / ''.
"""

from __future__ import annotations

import os
import re
import sqlite3
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
APP_DATABASE = os.path.join(
    ROOT, "app", "src", "main", "java", "com", "bulgarcal", "tracker", "data", "AppDatabase.kt"
)
ENTITIES = os.path.join(
    ROOT, "app", "src", "main", "java", "com", "bulgarcal", "tracker", "data", "Entities.kt"
)

# The version 3 schema, exactly as Room created it in the shipped 1.0.0 build.
V3_SCHEMA = """
CREATE TABLE foods (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    calories INTEGER NOT NULL,
    carbs REAL NOT NULL,
    protein REAL NOT NULL,
    fats REAL NOT NULL,
    saturatedFats REAL NOT NULL,
    sugars REAL NOT NULL,
    fiber REAL NOT NULL,
    glycemicIndex REAL NOT NULL,
    salt REAL NOT NULL,
    isCustom INTEGER NOT NULL,
    isRecipe INTEGER NOT NULL,
    description TEXT NOT NULL,
    servingGrams REAL NOT NULL,
    isFavorite INTEGER NOT NULL
);
CREATE TABLE meal_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    foodId INTEGER NOT NULL,
    foodName TEXT NOT NULL,
    mealType TEXT NOT NULL,
    dateEpochDay INTEGER NOT NULL,
    grams REAL NOT NULL,
    calories INTEGER NOT NULL,
    carbs REAL NOT NULL,
    protein REAL NOT NULL,
    fats REAL NOT NULL,
    saturatedFats REAL NOT NULL,
    sugars REAL NOT NULL,
    fiber REAL NOT NULL,
    glycemicIndex REAL NOT NULL,
    salt REAL NOT NULL
);
CREATE TABLE weight_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    dateEpochDay INTEGER NOT NULL, weightKg REAL NOT NULL);
CREATE TABLE water_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    dateEpochDay INTEGER NOT NULL, milliliters INTEGER NOT NULL);
"""

EXPECTED_NEW_COLUMNS = {
    "foods": ["monoFats", "polyFats", "transFats", "cholesterol", "potassium",
              "water", "glycemicLoad", "searchKey", "source"],
    "meal_logs": ["monoFats", "polyFats", "transFats", "cholesterol", "potassium",
                  "water", "glycemicLoad"],
}


def migration_statements() -> list[str]:
    with open(APP_DATABASE, encoding="utf-8") as fh:
        text = fh.read()
    block = re.search(r"MIGRATION_3_4.*?\n        \}", text, re.S)
    if not block:
        raise SystemExit("could not find MIGRATION_3_4 in AppDatabase.kt")
    return re.findall(r'"(ALTER TABLE [^"]+)"', block.group(0))


def main() -> int:
    statements = migration_statements()
    failures: list[str] = []

    expected_count = sum(len(v) for v in EXPECTED_NEW_COLUMNS.values())
    if len(statements) != expected_count:
        failures.append(
            f"migration has {len(statements)} statements, expected {expected_count}"
        )

    with open(ENTITIES, encoding="utf-8") as fh:
        entities = fh.read()
    for table, columns in EXPECTED_NEW_COLUMNS.items():
        for column in columns:
            if not re.search(rf'@ColumnInfo\(defaultValue = "0"\) val {column}: Float = 0f', entities) \
                    and not re.search(rf'@ColumnInfo\(defaultValue = "\'\'"\) val {column}: String = ""', entities):
                failures.append(f"{column} is not declared with a matching @ColumnInfo default")
            if not any(f"ADD COLUMN {column} " in s for s in statements):
                failures.append(f"migration does not add {table}.{column}")

    with tempfile.TemporaryDirectory() as tmp:
        db_path = os.path.join(tmp, "v3.db")
        conn = sqlite3.connect(db_path)
        conn.executescript(V3_SCHEMA)
        conn.execute(
            "INSERT INTO foods (name, category, calories, carbs, protein, fats, saturatedFats,"
            " sugars, fiber, glycemicIndex, salt, isCustom, isRecipe, description, servingGrams,"
            " isFavorite) VALUES ('Кисело мляко 2%','Общи',50,4.5,3.2,2.0,1.3,4.5,0,35,0.1,0,0,'',100,1)"
        )
        conn.execute(
            "INSERT INTO meal_logs (foodId, foodName, mealType, dateEpochDay, grams, calories,"
            " carbs, protein, fats, saturatedFats, sugars, fiber, glycemicIndex, salt)"
            " VALUES (1,'Кисело мляко 2%','BREAKFAST',20000,150,75,6.8,4.8,3.0,2.0,6.8,0,35,0.15)"
        )
        conn.execute("INSERT INTO weight_entries (dateEpochDay, weightKg) VALUES (20000, 78.5)")
        conn.execute("INSERT INTO water_logs (dateEpochDay, milliliters) VALUES (20000, 500)")
        conn.commit()

        for statement in statements:
            try:
                conn.execute(statement)
            except sqlite3.Error as exc:
                failures.append(f"{statement} -> {exc}")
        conn.commit()

        for table, columns in EXPECTED_NEW_COLUMNS.items():
            present = {row[1] for row in conn.execute(f"PRAGMA table_info({table})")}
            for column in columns:
                if column not in present:
                    failures.append(f"{table}.{column} missing after migration")

        row = conn.execute("SELECT name, calories, monoFats, searchKey, source FROM foods").fetchone()
        if row is None or row[0] != "Кисело мляко 2%" or row[1] != 50:
            failures.append(f"food row did not survive the migration: {row}")
        elif row[2] != 0 or row[3] != "" or row[4] != "":
            failures.append(f"new food columns do not default to 0/'': {row}")

        log = conn.execute("SELECT foodName, calories, monoFats, glycemicLoad FROM meal_logs").fetchone()
        if log is None or log[0] != "Кисело мляко 2%" or log[1] != 75:
            failures.append(f"diary row did not survive the migration: {log}")
        elif log[2] != 0 or log[3] != 0:
            failures.append(f"new diary columns do not default to 0: {log}")

        weights = conn.execute("SELECT COUNT(*) FROM weight_entries").fetchone()[0]
        water = conn.execute("SELECT COUNT(*) FROM water_logs").fetchone()[0]
        if weights != 1 or water != 1:
            failures.append(f"weight/water rows lost: weights={weights} water={water}")
        conn.close()

    print(f"migration statements applied: {len(statements)}")
    if failures:
        print("FAILED CHECKS:")
        for failure in failures:
            print(" -", failure)
        return 1
    print("migration 3 -> 4 keeps existing data and adds every new column")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

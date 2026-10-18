package com.bulgarcal.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodEntity::class,
        MealLogEntity::class,
        UserProfileEntity::class,
        WeightEntryEntity::class,
        RecipeIngredientEntity::class,
        AppMetaEntity::class,
        WaterLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun appMetaDao(): AppMetaDao
    abstract fun waterLogDao(): WaterLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Version 3 -> 4 adds the micronutrients the details screen already shows
         * (mono-/poly-unsaturated and trans fats, cholesterol, potassium, water,
         * glycemic load) plus the Latin search key and the data source. Existing
         * food, diary, weight and water rows are all preserved.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "ALTER TABLE foods ADD COLUMN monoFats REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE foods ADD COLUMN polyFats REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE foods ADD COLUMN transFats REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE foods ADD COLUMN cholesterol REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE foods ADD COLUMN potassium REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE foods ADD COLUMN water REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE foods ADD COLUMN glycemicLoad REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE foods ADD COLUMN searchKey TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE foods ADD COLUMN source TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE meal_logs ADD COLUMN monoFats REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE meal_logs ADD COLUMN polyFats REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE meal_logs ADD COLUMN transFats REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE meal_logs ADD COLUMN cholesterol REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE meal_logs ADD COLUMN potassium REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE meal_logs ADD COLUMN water REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE meal_logs ADD COLUMN glycemicLoad REAL NOT NULL DEFAULT 0"
                ).forEach(db::execSQL)
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bulgar_cal.db"
                ).addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

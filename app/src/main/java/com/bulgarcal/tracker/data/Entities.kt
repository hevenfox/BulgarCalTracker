package com.bulgarcal.tracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [barcode] is indexed because a scan looks a product up by it directly; the
 * column holds a digits-only GTIN (8, 12, 13 or 14 digits) or "".
 */
@Entity(tableName = "foods", indices = [Index(value = ["barcode"])])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val calories: Int,
    val carbs: Float,
    val protein: Float,
    val fats: Float,
    val saturatedFats: Float = 0f,
    val sugars: Float = 0f,
    val fiber: Float = 0f,
    val glycemicIndex: Float = 0f,
    val salt: Float = 0f,
    val isCustom: Boolean = false,
    val isRecipe: Boolean = false,
    val description: String = "",
    val servingGrams: Float = 100f,
    val isFavorite: Boolean = false,
    /** Added in database version 4; the defaults are declared so the 3 -> 4
     *  migration and a fresh install produce the same schema. */
    @ColumnInfo(defaultValue = "0") val monoFats: Float = 0f,
    @ColumnInfo(defaultValue = "0") val polyFats: Float = 0f,
    @ColumnInfo(defaultValue = "0") val transFats: Float = 0f,
    @ColumnInfo(defaultValue = "0") val cholesterol: Float = 0f,
    @ColumnInfo(defaultValue = "0") val potassium: Float = 0f,
    @ColumnInfo(defaultValue = "0") val water: Float = 0f,
    @ColumnInfo(defaultValue = "0") val glycemicLoad: Float = 0f,
    @ColumnInfo(defaultValue = "''") val searchKey: String = "",
    @ColumnInfo(defaultValue = "''") val source: String = "",
    /** Added in database version 5: the retail barcode printed on the package. */
    @ColumnInfo(defaultValue = "''") val barcode: String = ""
)

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long,
    val foodName: String,
    val mealType: String,
    val dateEpochDay: Long,
    val grams: Float,
    val calories: Int,
    val carbs: Float,
    val protein: Float,
    val fats: Float,
    val saturatedFats: Float = 0f,
    val sugars: Float = 0f,
    val fiber: Float = 0f,
    val glycemicIndex: Float = 0f,
    val salt: Float = 0f,
    @ColumnInfo(defaultValue = "0") val monoFats: Float = 0f,
    @ColumnInfo(defaultValue = "0") val polyFats: Float = 0f,
    @ColumnInfo(defaultValue = "0") val transFats: Float = 0f,
    @ColumnInfo(defaultValue = "0") val cholesterol: Float = 0f,
    @ColumnInfo(defaultValue = "0") val potassium: Float = 0f,
    @ColumnInfo(defaultValue = "0") val water: Float = 0f,
    @ColumnInfo(defaultValue = "0") val glycemicLoad: Float = 0f
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingComplete: Boolean = false,
    val goal: String,
    val sex: String,
    val birthDateEpochDay: Long,
    val heightCm: Float,
    val weightKg: Float,
    val targetWeightKg: Float,
    val activityLevel: String,
    val pace: String,
    val dailyCalories: Int,
    val carbsPercent: Int = 50,
    val proteinPercent: Int = 20,
    val fatPercent: Int = 30,
    val carbsGrams: Float,
    val proteinGrams: Float,
    val fatGrams: Float,
    val caloriesOverridden: Boolean = false
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Float
)

@Entity(tableName = "recipe_ingredients")
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeFoodId: Long,
    val ingredientFoodId: Long,
    val ingredientName: String,
    val grams: Float
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val milliliters: Int
)

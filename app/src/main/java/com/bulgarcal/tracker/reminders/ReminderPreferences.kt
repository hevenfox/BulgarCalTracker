package com.bulgarcal.tracker.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore("meal_reminders")

data class ReminderSettings(
    val enabled: Boolean = false,
    val breakfastHour: Int = MealReminderType.BREAKFAST.defaultHour,
    val breakfastMinute: Int = MealReminderType.BREAKFAST.defaultMinute,
    val lunchHour: Int = MealReminderType.LUNCH.defaultHour,
    val lunchMinute: Int = MealReminderType.LUNCH.defaultMinute,
    val dinnerHour: Int = MealReminderType.DINNER.defaultHour,
    val dinnerMinute: Int = MealReminderType.DINNER.defaultMinute
)

class ReminderPreferences(private val context: Context) {

    private val enabledKey = booleanPreferencesKey("reminders_enabled")

    val settings: Flow<ReminderSettings> = context.reminderDataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[enabledKey] ?: false,
            breakfastHour = prefs[intPreferencesKey(MealReminderType.BREAKFAST.prefHourKey)]
                ?: MealReminderType.BREAKFAST.defaultHour,
            breakfastMinute = prefs[intPreferencesKey(MealReminderType.BREAKFAST.prefMinuteKey)]
                ?: MealReminderType.BREAKFAST.defaultMinute,
            lunchHour = prefs[intPreferencesKey(MealReminderType.LUNCH.prefHourKey)]
                ?: MealReminderType.LUNCH.defaultHour,
            lunchMinute = prefs[intPreferencesKey(MealReminderType.LUNCH.prefMinuteKey)]
                ?: MealReminderType.LUNCH.defaultMinute,
            dinnerHour = prefs[intPreferencesKey(MealReminderType.DINNER.prefHourKey)]
                ?: MealReminderType.DINNER.defaultHour,
            dinnerMinute = prefs[intPreferencesKey(MealReminderType.DINNER.prefMinuteKey)]
                ?: MealReminderType.DINNER.defaultMinute
        )
    }

    suspend fun current(): ReminderSettings = settings.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun setTime(type: MealReminderType, hour: Int, minute: Int) {
        context.reminderDataStore.edit {
            it[intPreferencesKey(type.prefHourKey)] = hour.coerceIn(0, 23)
            it[intPreferencesKey(type.prefMinuteKey)] = minute.coerceIn(0, 59)
        }
    }

    fun hourMinute(settings: ReminderSettings, type: MealReminderType): Pair<Int, Int> = when (type) {
        MealReminderType.BREAKFAST -> settings.breakfastHour to settings.breakfastMinute
        MealReminderType.LUNCH -> settings.lunchHour to settings.lunchMinute
        MealReminderType.DINNER -> settings.dinnerHour to settings.dinnerMinute
    }
}

package com.bulgarcal.tracker.reminders

enum class MealReminderType(
    val requestCode: Int,
    val titleBg: String,
    val messageBg: String,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    BREAKFAST(
        requestCode = 1001,
        titleBg = "Време за закуска",
        messageBg = "Запиши какво си ял на закуска в дневника.",
        defaultHour = 8,
        defaultMinute = 0
    ),
    LUNCH(
        requestCode = 1002,
        titleBg = "Време за обяд",
        messageBg = "Запиши какво си ял на обяд в дневника.",
        defaultHour = 13,
        defaultMinute = 0
    ),
    DINNER(
        requestCode = 1003,
        titleBg = "Време за вечеря",
        messageBg = "Запиши какво си ял на вечеря в дневника.",
        defaultHour = 19,
        defaultMinute = 0
    );

    val prefHourKey: String get() = "${name.lowercase()}_hour"
    val prefMinuteKey: String get() = "${name.lowercase()}_minute"
}

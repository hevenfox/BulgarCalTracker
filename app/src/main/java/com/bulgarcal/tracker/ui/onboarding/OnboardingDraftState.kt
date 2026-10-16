package com.bulgarcal.tracker.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.Sex
import java.time.LocalDate

class OnboardingDraftState {
    var goal by mutableStateOf<GoalType?>(null)
    var sex by mutableStateOf(Sex.MALE)
    var birthDate by mutableStateOf(LocalDate.of(1990, 1, 1))
    var heightCm by mutableStateOf("175")
    var weightKg by mutableStateOf("75")
    var targetKg by mutableStateOf("70")
    var activity by mutableStateOf(ActivityLevel.LIGHT)
    var pace by mutableStateOf(PaceOption.SLOW)
}

@Composable
fun rememberOnboardingDraft(): OnboardingDraftState = remember { OnboardingDraftState() }

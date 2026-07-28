package com.saarthi.app.feature.medicines

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.saarthi.app.core.designsystem.SaarthiTheme
import com.saarthi.app.core.ui.SaarthiButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderActivity : ComponentActivity() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: ReminderNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val medicineId = ReminderScheduler.medicineId(intent) ?: return finish()
        val time = ReminderScheduler.time(intent) ?: return finish()

        setContent {
            SaarthiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReminderConfirmScreen(
                        medicineIdProvider = { medicineId },
                        timeProvider = { time },
                        repository = repository,
                        onTaken = {
                            lifecycleScope.launch {
                                repository.markTaken(medicineId, time)
                                scheduler.confirmedStopNagging(medicineId, time)
                                notifier.cancel(medicineId, time)
                                finishAndRemoveTask()
                            }
                        },
                        onSnooze = {
                            scheduler.nagAgain(medicineId, time, delayMs = 5 * 60 * 1000L)
                            notifier.cancel(medicineId, time)
                            finishAndRemoveTask()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderConfirmScreen(
    medicineIdProvider: () -> String,
    timeProvider: () -> String,
    repository: MedicineRepository,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
) {
    var name by remember { mutableStateOf("your medicine") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        repository.getById(medicineIdProvider())?.let { name = it.name }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "💊", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Time for your medicine", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        SaarthiButton(text = "✅ I took it", onClick = onTaken)
        Spacer(modifier = Modifier.height(12.dp))
        SaarthiButton(text = "⏰ Remind me in 5 minutes", onClick = onSnooze)
    }
}

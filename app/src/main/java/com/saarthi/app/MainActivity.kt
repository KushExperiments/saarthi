package com.saarthi.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Calendar

/** The languages Saarthi can listen and speak in. Add more freely. */
val LANGS = listOf(
    "en-IN" to "English", "hi-IN" to "हिन्दी Hindi", "bn-IN" to "বাংলা Bengali",
    "ta-IN" to "தமிழ் Tamil", "te-IN" to "తెలుగు Telugu", "mr-IN" to "मराठी Marathi",
    "gu-IN" to "ગુજરાતી Gujarati", "kn-IN" to "ಕನ್ನಡ Kannada", "ml-IN" to "മലയാളം Malayalam",
    "pa-IN" to "ਪੰਜਾਬੀ Punjabi", "ur-IN" to "اردو Urdu", "ar-SA" to "العربية Arabic",
    "es-ES" to "Español", "fr-FR" to "Français", "de-DE" to "Deutsch",
    "pt-BR" to "Português", "ru-RU" to "Русский", "zh-CN" to "中文",
    "ja-JP" to "日本語", "id-ID" to "Indonesia"
)

enum class Screen { HOME, MEDS, CONTACTS, SETUP }

class MainActivity : ComponentActivity() {

    private lateinit var voice: Voice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voice = Voice(this)
        Reminders.scheduleAll(this)
        setContent { MaterialTheme(colorScheme = lightGreen()) { App(voice) } }
    }

    override fun onDestroy() { super.onDestroy(); voice.release() }
}

@Composable
fun App(voice: Voice) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }
    var settings by remember { mutableStateOf(Store.settings(ctx)) }
    val meds = remember { mutableStateListOf<Medicine>().apply { addAll(Store.medicines(ctx)) } }
    val contacts = remember { mutableStateListOf<Contact>().apply { addAll(Store.contacts(ctx)) } }
    var heard by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }

    // Ask for the important permissions once.
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) {
        val need = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE, Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS)
        val ask = need.filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
        if (ask.isNotEmpty()) perms.launch(ask.toTypedArray())
    }

    fun say(t: String) { heard = t; voice.speak(t) }

    // Runs a parsed command.
    fun run(a: Action) {
        when (a) {
            is Action.Say -> say(a.text)
            is Action.Call -> { say("Calling ${a.contact.name}."); DeviceControl.call(ctx, a.contact.phone) }
            is Action.WhatsApp -> { say("Opening WhatsApp for ${a.contact.name}."); DeviceControl.whatsapp(ctx, a.contact.phone) }
            is Action.Sms -> { say("Message to ${a.contact.name}."); DeviceControl.sms(ctx, a.contact.phone) }
            is Action.Torch -> { val on = a.on ?: true; DeviceControl.setTorch(ctx, on); say(if (on) "Torch on." else "Torch off.") }
            is Action.YouTube -> { say(if (a.query.isBlank()) "Opening YouTube." else "Playing ${a.query}."); DeviceControl.youtube(ctx, a.query) }
            is Action.Volume -> { when (a.dir) { "up" -> DeviceControl.volumeUp(ctx); "down" -> DeviceControl.volumeDown(ctx); else -> DeviceControl.volumeMax(ctx) }; say("Done.") }
            Action.MedicineTaken -> {
                val done = markEarliestDue(ctx, meds)
                say(if (done) "Well done. I marked your medicine as taken." else "Okay. No medicine is due right now.")
            }
            Action.OpenMedicines -> { screen = Screen.MEDS }
            Action.OpenContacts -> { say("Who should I contact? Please add them here."); screen = Screen.CONTACTS }
            Action.Help -> { say("I can help you. Tap the green microphone and just talk. Say: call, torch, or YouTube."); }
        }
    }

    fun startListening() {
        listening = true; heard = ""
        voice.listen(
            onHeard = { t -> heard = "“$t”"; run(CommandParser.parse(t, contacts)) },
            onDone = { listening = false },
            onError = { e -> heard = e; listening = false }
        )
    }

    // greet once
    LaunchedEffect(Unit) {
        val n = if (settings.userName.isNotBlank()) ", ${settings.userName}" else ""
        voice.speak("Hello$n. I am ${settings.appName}. I am here to help you.")
    }

    when (screen) {
        Screen.HOME -> HomeScreen(settings, heard, listening,
            onTalk = { startListening() },
            onCard = { act ->
                when (act) {
                    "meds" -> screen = Screen.MEDS
                    "call" -> screen = Screen.CONTACTS
                    "torch" -> run(Action.Torch(null))
                    "youtube" -> DeviceControl.youtube(ctx, "")
                    "help" -> run(Action.Help)
                    "louder" -> run(Action.Volume("up"))
                }
            },
            onSetup = { settings = Store.settings(ctx); screen = Screen.SETUP })

        Screen.MEDS -> MedsScreen(meds,
            onBack = { screen = Screen.HOME },
            onAdd = { m -> meds.add(m); Store.saveMedicines(ctx, meds); Reminders.scheduleAll(ctx) },
            onDelete = { m -> meds.remove(m); Store.saveMedicines(ctx, meds); Reminders.scheduleAll(ctx) },
            onTook = { m -> m.times.forEach { Reminders.markTaken(ctx, m.id, it) }; refresh(ctx, meds); voice.speak("Well done.") })

        Screen.CONTACTS -> ContactsScreen(contacts, ctx,
            onBack = { screen = Screen.HOME },
            onAdd = { c -> contacts.add(c); Store.saveContacts(ctx, contacts) },
            onDelete = { c -> contacts.remove(c); Store.saveContacts(ctx, contacts) })

        Screen.SETUP -> SetupScreen(settings, voice,
            onBack = { screen = Screen.HOME },
            onSave = { s -> settings = s; Store.saveSettings(ctx, s) })
    }
}

private fun refresh(ctx: android.content.Context, meds: MutableList<Medicine>) {
    meds.clear(); meds.addAll(Store.medicines(ctx))
}

/** Mark the earliest due-but-not-taken dose as taken. Returns true if one was found. */
private fun markEarliestDue(ctx: android.content.Context, meds: MutableList<Medicine>): Boolean {
    val now = Calendar.getInstance()
    val cur = "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    for (m in meds) for (t in m.times) {
        if (!Reminders.isTaken(m, t) && t <= cur) {
            Reminders.markTaken(ctx, m.id, t); refresh(ctx, meds); return true
        }
    }
    return false
}

/* ----------------------------- HOME ----------------------------- */
@Composable
fun HomeScreen(s: Settings, heard: String, listening: Boolean,
               onTalk: () -> Unit, onCard: (String) -> Unit, onSetup: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (s.userName.isNotBlank()) "Hello, ${s.userName} 👋" else "Hello 👋",
                fontSize = 28.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onSetup) { Text("⚙️", fontSize = 28.sp) }
        }
        Spacer(Modifier.height(10.dp))

        Button(onClick = onTalk, shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎤", fontSize = 56.sp)
                Text(if (listening) "Listening… speak now" else "Tap and talk to me", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(heard, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().heightIn(min = 26.dp))
        Spacer(Modifier.height(8.dp))

        val cards = listOf("meds" to ("💊" to "My Medicines"), "call" to ("📞" to "Call Someone"),
            "torch" to ("🔦" to "Torch"), "youtube" to ("▶️" to "YouTube"),
            "help" to ("❓" to "Help Me"), "louder" to ("🔊" to "Louder"))
        cards.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (act, v) ->
                    Card(onClick = { onCard(act) }, modifier = Modifier.weight(1f).height(130.dp)) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(v.first, fontSize = 44.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(v.second, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/* --------------------------- MEDICINES --------------------------- */
@Composable
fun MedsScreen(meds: List<Medicine>, onBack: () -> Unit, onAdd: (Medicine) -> Unit,
               onDelete: (Medicine) -> Unit, onTook: (Medicine) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    ScreenScaffold("My Medicines", onBack) {
        if (meds.isEmpty()) Text("No medicines yet. Tap the button below to add one.", fontSize = 18.sp)
        meds.forEach { m ->
            val done = m.times.all { Reminders.isTaken(m, it) }
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💊", fontSize = 34.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(m.times.joinToString("  •  ") { fmt12(it) }.ifBlank { "no time set" }, fontSize = 16.sp)
                    }
                    if (done) Text("Taken ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    else TextButton(onClick = { onTook(m) }) { Text("✅ Took it", fontSize = 18.sp) }
                    IconButton(onClick = { onDelete(m) }) { Icon(Icons.Default.Delete, "delete") }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        BigAddButton("➕ Add a medicine") { showAdd = true }
    }
    if (showAdd) AddMedicineDialog(onDismiss = { showAdd = false }, onSave = { onAdd(it); showAdd = false })
}

@Composable
fun AddMedicineDialog(onDismiss: () -> Unit, onSave: (Medicine) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    val times = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a medicine") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Medicine name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Times to take:")
                times.forEach { Text("• ${fmt12(it)}", fontSize = 18.sp) }
                TextButton(onClick = {
                    val now = Calendar.getInstance()
                    TimePickerDialog(ctx, { _, h, min ->
                        val t = "%02d:%02d".format(h, min); if (!times.contains(t)) { times.add(t); times.sort() }
                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
                }) { Text("➕ Add a time") }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(Medicine(genId(), name.trim(), times.toList())) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/* --------------------------- CONTACTS --------------------------- */
@Composable
fun ContactsScreen(contacts: List<Contact>, ctx: android.content.Context, onBack: () -> Unit,
                   onAdd: (Contact) -> Unit, onDelete: (Contact) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    ScreenScaffold("Call Someone", onBack) {
        if (contacts.isEmpty()) Text("No people yet. Add family so you can say \"call beta\".", fontSize = 18.sp)
        contacts.forEach { c ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🧑", fontSize = 34.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(c.phone, fontSize = 16.sp)
                    }
                    TextButton(onClick = { DeviceControl.call(ctx, c.phone) }) { Text("📞", fontSize = 26.sp) }
                    TextButton(onClick = { DeviceControl.whatsapp(ctx, c.phone) }) { Text("🟢", fontSize = 26.sp) }
                    IconButton(onClick = { onDelete(c) }) { Icon(Icons.Default.Delete, "delete") }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        BigAddButton("➕ Add a person") { showAdd = true }
    }
    if (showAdd) AddContactDialog(onDismiss = { showAdd = false }, onSave = { onAdd(it); showAdd = false })
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onSave: (Contact) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a person") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name (e.g. Beta, Vishal, Doctor)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone (with country code)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && phone.isNotBlank(), onClick = { onSave(Contact(genId(), name.trim(), phone.trim())) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/* ----------------------------- SETUP ----------------------------- */
@Composable
fun SetupScreen(initial: Settings, voice: Voice, onBack: () -> Unit, onSave: (Settings) -> Unit) {
    val ctx = LocalContext.current
    var appName by remember { mutableStateOf(initial.appName) }
    var lang by remember { mutableStateOf(initial.lang) }
    var rate by remember { mutableStateOf(initial.rate) }
    var userName by remember { mutableStateOf(initial.userName) }
    var langOpen by remember { mutableStateOf(false) }

    fun persist() = onSave(Settings(appName.ifBlank { "Saarthi" }, lang, rate, userName))

    ScreenScaffold("Setup", onBack) {
        SetupCard("Assistant's name (you can change it)") {
            OutlinedTextField(appName, { appName = it; persist() }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        SetupCard("Name to greet") {
            OutlinedTextField(userName, { userName = it; persist() }, singleLine = true,
                placeholder = { Text("e.g. Amma, Dad, Kamala") }, modifier = Modifier.fillMaxWidth())
        }
        SetupCard("Language to speak & understand") {
            Box {
                OutlinedButton(onClick = { langOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(LANGS.firstOrNull { it.first == lang }?.second ?: lang, fontSize = 18.sp)
                }
                DropdownMenu(langOpen, onDismissRequest = { langOpen = false }) {
                    LANGS.forEach { (code, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { lang = code; langOpen = false; persist(); voice.speak("This is my new voice.") })
                    }
                }
            }
        }
        SetupCard("How slow should I talk?") {
            Slider(value = rate, onValueChange = { rate = it }, onValueChangeFinished = { persist() }, valueRange = 0.5f..1.1f)
            Button(onClick = { persist(); voice.speak("Hello. This is how I will talk to you.") }) { Text("🔊 Test voice") }
        }
        SetupCard("Screen brightness control") {
            if (DeviceControl.canWriteSettings(ctx)) Text("✅ Allowed", color = MaterialTheme.colorScheme.primary)
            else Button(onClick = { DeviceControl.requestWriteSettings(ctx) }) { Text("Allow brightness control") }
        }
        Text("Tip: set up medicines and people here, then hand the phone to your elder. They only need the big buttons and the 🎤.",
            fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

/* --------------------------- SHARED UI --------------------------- */
@Composable
fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("⬅️", fontSize = 26.sp) }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun SetupCard(label: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun BigAddButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(64.dp)) {
        Text(text, fontSize = 20.sp)
    }
}

fun fmt12(hm: String): String {
    val p = hm.split(":"); if (p.size < 2) return hm
    val h = p[0].toInt(); val m = p[1]
    val ap = if (h < 12) "AM" else "PM"; val h12 = (h % 12).let { if (it == 0) 12 else it }
    return "$h12:$m $ap"
}

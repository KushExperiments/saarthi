package com.saarthi.app

/**
 * Turns a spoken sentence into an action.
 * The phone already transcribes the spoken language; here we match
 * common command words across many languages, then look for a saved
 * contact name inside the sentence ("call beta", "beta ko phone karo").
 */
sealed class Action {
    data class Say(val text: String) : Action()
    data class Call(val contact: Contact) : Action()
    data class WhatsApp(val contact: Contact, val message: String = "") : Action()
    data class Sms(val contact: Contact, val message: String = "") : Action()
    data class Torch(val on: Boolean?) : Action()     // null = toggle
    data class YouTube(val query: String) : Action()
    data class Volume(val dir: String) : Action()      // up / down / max
    object MedicineTaken : Action()
    object OpenMedicines : Action()
    object OpenContacts : Action()
    object Help : Action()
}

object CommandParser {

    private val CALL     = listOf("call","phone","dial","ring","कॉल","फोन","फ़ोन","बुलाओ","फोन करो","अझ","அழை","కాల్","ಕರೆ","ফোন","llama","llamar","appelle","anruf","ligar","позвони","打电话","電話","telepon","tawagan")
    private val WHATSAPP = listOf("whatsapp","whats app","व्हाट्सएप","वॉट्सऐप","వాట్సాప్","வாட்ஸ்அப்")
    private val MESSAGE  = listOf("message","text","sms","संदेश","मैसेज","मेसेज","மெசேஜ்","mensaje","nachricht","messaggio")
    private val TORCH    = listOf("torch","flashlight","flash","light","lamp","टॉर्च","रोशनी","लाइट","बत्ती","விளக்கு","linterna","lampe","taschenlampe","手电","灯")
    private val OFF      = listOf("off","band","बंद","बुझा","ऑफ","close","apaga","aus","关","எடு")
    private val YT       = listOf("youtube","you tube","video","यूट्यूब","वीडियो","गाना","song","music","गीत","भजन","வீடியோ")
    private val TAKEN    = listOf("taken","took","done","khaya","खा लिया","ले लिया","खाई","हो गया","finished","खा ली","le liya","tomé","genommen")
    private val VOL_UP   = listOf("louder","volume up","increase volume","आवाज बढ़ा","तेज","zor","volumen alto")
    private val VOL_DOWN = listOf("quieter","volume down","lower volume","आवाज कम","धीमा","dhima","volumen bajo")
    private val VOL_MAX  = listOf("full volume","maximum volume","पूरी आवाज","full sound")
    private val HELP     = listOf("help","madad","मदद","सहायता","bachao","बचाओ","ayuda","hilfe","emergency","救命")
    private val MEDS     = listOf("medicine","medicines","pill","tablet","dawai","दवा","दवाई","गोली","medicina","медицина")

    private fun norm(s: String) = " " + s.lowercase().replace(Regex("[.,!?]"), " ").replace(Regex("\\s+"), " ") + " "
    private fun has(t: String, list: List<String>) = list.any { t.contains(it) }

    private fun findContact(t: String, contacts: List<Contact>): Contact? =
        contacts.filter { it.name.isNotBlank() && t.contains(it.name.lowercase()) }
                .maxByOrNull { it.name.length }

    fun parse(raw: String, contacts: List<Contact>): Action {
        if (raw.isBlank()) return Action.Say("Sorry, I did not hear you. Please try again.")
        val t = norm(raw)

        if (has(t, HELP)) return Action.Help
        if (has(t, TAKEN)) return Action.MedicineTaken

        if (has(t, VOL_MAX))  return Action.Volume("max")
        if (has(t, VOL_UP))   return Action.Volume("up")
        if (has(t, VOL_DOWN)) return Action.Volume("down")

        if (has(t, TORCH)) return Action.Torch(if (has(t, OFF)) false else true)

        if (has(t, YT)) {
            val q = raw.replace(Regex("(?i)you\\s*tube|youtube|यूट्यूब"), "")
                       .replace(Regex("(?i)\\b(open|play|search|find|चलाओ|खोलो|lagao|लगाओ)\\b"), "")
                       .trim()
            return Action.YouTube(q)
        }

        val wantsCall = has(t, CALL)
        val wantsWA   = has(t, WHATSAPP)
        val wantsMsg  = has(t, MESSAGE)
        if (wantsCall || wantsWA || wantsMsg) {
            val c = findContact(t, contacts) ?: return Action.OpenContacts
            return when {
                wantsWA  -> Action.WhatsApp(c)
                wantsMsg -> Action.Sms(c)
                else     -> Action.Call(c)
            }
        }

        if (has(t, MEDS)) return Action.OpenMedicines

        return Action.Say("Sorry, I did not understand. You can say: call, torch, YouTube, or medicine.")
    }
}

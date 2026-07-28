package com.saarthi.app.feature.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * ACTION_DIAL, not ACTION_CALL — opens the dialer pre-filled rather than
 * calling directly. No CALL_PHONE permission needed at all. A direct-call
 * upgrade (matching the original prototype's DeviceControl.kt) is a
 * documented, deliberate fast-follow, not an oversight — see feature
 * README notes. Elder safety-critical calling deserves a runtime
 * permission flow done carefully, not squeezed into this batch.
 */
object ContactActions {
    private fun clean(phone: String) = phone.replace(Regex("[^0-9+]"), "")

    fun dial(context: Context, contact: Contact) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${clean(contact.phone)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openWhatsApp(context: Context, contact: Contact) {
        val number = clean(contact.phone).removePrefix("+")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // No browser/WhatsApp available to resolve the intent — fail
            // quietly rather than crash (Architecture §16: fail gracefully).
        }
    }
}

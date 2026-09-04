package com.luminor.actionbox.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import java.time.ZoneId

object ExternalActions {
    fun openCalendar(context: Context, action: DetectedAction) {
        val start = (action.scheduledAt ?: java.time.LocalDateTime.now().plusHours(1))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, action.title)
            putExtra(CalendarContract.Events.DESCRIPTION, action.content)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 60 * 60 * 1000)
        }
        safeStart(context, intent)
    }

    fun openMaps(context: Context, query: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        safeStart(context, Intent(Intent.ACTION_VIEW, uri))
    }

    fun openDialer(context: Context, phone: String) {
        safeStart(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}")))
    }

    fun insertContact(context: Context, name: String?, phone: String) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, name.orEmpty())
            putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        }
        safeStart(context, intent)
    }

    fun openUrl(context: Context, url: String) {
        safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun copy(context: Context, label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }

    private fun safeStart(context: Context, intent: Intent) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

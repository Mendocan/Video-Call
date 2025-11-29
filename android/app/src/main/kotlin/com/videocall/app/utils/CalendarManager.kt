package com.videocall.app.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.videocall.app.model.ScheduledCall
import java.util.Calendar
import java.util.TimeZone

/**
 * Android Calendar entegrasyonu
 * Randevuları takvime ekler ve takvimden otomatik randevu okur
 */
class CalendarManager(private val context: Context) {
    
    private val contentResolver: ContentResolver = context.contentResolver
    
    /**
     * Takvime randevu ekle
     */
    fun addEventToCalendar(scheduledCall: ScheduledCall): Long? {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = scheduledCall.scheduledTime
        
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, scheduledCall.scheduledTime)
            put(CalendarContract.Events.DTEND, scheduledCall.scheduledTime + (60 * 60 * 1000)) // 1 saat
            put(CalendarContract.Events.TITLE, "Video Call: ${scheduledCall.contactName}")
            put(CalendarContract.Events.DESCRIPTION, buildEventDescription(scheduledCall))
            put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId())
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.EVENT_END_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1) // Hatırlatıcı aktif
            put(CalendarContract.Events.HAS_ATTENDEE_DATA, 0)
        }
        
        return try {
            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLong()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Takvimden randevu sil
     */
    fun removeEventFromCalendar(eventId: Long): Boolean {
        return try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            contentResolver.delete(deleteUri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Takvimden randevu güncelle
     */
    fun updateEventInCalendar(eventId: Long, scheduledCall: ScheduledCall): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = scheduledCall.scheduledTime
        
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, scheduledCall.scheduledTime)
            put(CalendarContract.Events.DTEND, scheduledCall.scheduledTime + (60 * 60 * 1000))
            put(CalendarContract.Events.TITLE, "Video Call: ${scheduledCall.contactName}")
            put(CalendarContract.Events.DESCRIPTION, buildEventDescription(scheduledCall))
        }
        
        return try {
            val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            contentResolver.update(updateUri, values, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Takvimden Video Call randevularını oku
     */
    fun readVideoCallEventsFromCalendar(): List<ScheduledCall> {
        val events = mutableListOf<ScheduledCall>()
        
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        
        val selection = "${CalendarContract.Events.TITLE} LIKE ?"
        val selectionArgs = arrayOf("%Video Call:%")
        
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    val eventId = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID))
                    val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                    val description = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
                    val dtStart = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                    
                    // Başlıktan kişi adını çıkar: "Video Call: Ahmet" -> "Ahmet"
                    val contactName = title.replace("Video Call: ", "").trim()
                    
                    // Açıklamadan telefon numarası ve notları çıkar
                    val (phoneNumber, notes) = parseDescription(description)
                    
                    val scheduledCall = ScheduledCall(
                        id = eventId,
                        contactName = contactName,
                        contactPhoneNumber = phoneNumber,
                        scheduledTime = dtStart,
                        notes = notes,
                        isCompleted = false,
                        isCancelled = false,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    events.add(scheduledCall)
                }
            }
        } catch (e: Exception) {
            // Hata durumunda boş liste döndür
        } finally {
            cursor?.close()
        }
        
        return events
    }
    
    /**
     * Hatırlatıcı ekle (15 dakika önceden)
     */
    fun addReminder(eventId: Long, minutesBefore: Int = 15): Boolean {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            put(CalendarContract.Reminders.MINUTES, minutesBefore)
        }
        
        return try {
            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Varsayılan takvim ID'sini al
     */
    private fun getDefaultCalendarId(): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.SYNC_EVENTS} = ?"
        val selectionArgs = arrayOf("1", "1")
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            
            cursor?.let {
                if (it.moveToFirst()) {
                    return it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                }
            }
        } catch (e: Exception) {
            // Hata durumunda
        } finally {
            cursor?.close()
        }
        
        // Varsayılan olarak 1 döndür (genellikle varsayılan takvim)
        return 1
    }
    
    /**
     * Etkinlik açıklaması oluştur
     */
    private fun buildEventDescription(scheduledCall: ScheduledCall): String {
        val description = StringBuilder()
        description.append("Video Call randevusu\n\n")
        
        if (scheduledCall.contactPhoneNumber != null) {
            description.append("Telefon: ${scheduledCall.contactPhoneNumber}\n")
        }
        
        if (scheduledCall.roomCode != null) {
            description.append("Oda Kodu: ${scheduledCall.roomCode}\n")
        }
        
        if (scheduledCall.notes != null && scheduledCall.notes.isNotEmpty()) {
            description.append("\nNotlar: ${scheduledCall.notes}")
        }
        
        return description.toString()
    }
    
    /**
     * Açıklamadan telefon numarası ve notları parse et
     */
    private fun parseDescription(description: String?): Pair<String?, String?> {
        if (description == null) return Pair(null, null)
        
        var phoneNumber: String? = null
        var notes: String? = null
        
        val lines = description.split("\n")
        for (line in lines) {
            if (line.startsWith("Telefon:")) {
                phoneNumber = line.replace("Telefon:", "").trim()
            } else if (line.startsWith("Notlar:")) {
                notes = line.replace("Notlar:", "").trim()
            }
        }
        
        return Pair(phoneNumber, notes)
    }
    
    /**
     * Takvim izni kontrolü
     */
    fun hasCalendarPermission(): Boolean {
        return android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CALENDAR
                ) &&
                android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_CALENDAR
                )
    }
}


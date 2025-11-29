package com.videocall.app.data

import android.content.ContentResolver
import android.provider.ContactsContract
import com.videocall.app.model.Contact

class ContactsRepository(private val contentResolver: ContentResolver) {

    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        ) ?: return contacts

        cursor.use {
            val idColumn = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameColumn = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoUriColumn = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val hasPhoneColumn = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: continue
                val photoUri = it.getString(photoUriColumn)
                val hasPhone = it.getInt(hasPhoneColumn) > 0

                var phoneNumber: String? = null
                var email: String? = null

                if (hasPhone) {
                    phoneNumber = getPhoneNumber(id)
                }

                email = getEmail(id)

                if (phoneNumber != null || email != null) {
                    contacts.add(
                        Contact(
                            id = id,
                            name = name,
                            phoneNumber = phoneNumber,
                            email = email,
                            photoUri = photoUri
                        )
                    )
                }
            }
        }

        return contacts
    }

    private fun getPhoneNumber(contactId: Long): String? {
        val phoneProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val phoneSelectionArgs = arrayOf(contactId.toString())

        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            phoneProjection,
            phoneSelection,
            phoneSelectionArgs,
            null
        ) ?: return null

        phoneCursor.use {
            if (it.moveToFirst()) {
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return it.getString(numberColumn)
            }
        }
        return null
    }

    private fun getEmail(contactId: Long): String? {
        val emailProjection = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS)
        val emailSelection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
        val emailSelectionArgs = arrayOf(contactId.toString())

        val emailCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            emailProjection,
            emailSelection,
            emailSelectionArgs,
            null
        ) ?: return null

        emailCursor.use {
            if (it.moveToFirst()) {
                val emailColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                return it.getString(emailColumn)
            }
        }
        return null
    }
}


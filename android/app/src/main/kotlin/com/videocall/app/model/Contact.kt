package com.videocall.app.model

import com.videocall.app.model.UserStatus

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val email: String?,
    val photoUri: String?,
    val isFavorite: Boolean = false,
    val notes: String? = null,
    val groups: List<String> = emptyList(), // Örn: ["Akrabalarım", "Taraftar Grubu"]
    val status: UserStatus? = null // Kullanıcı durumu (backend'den gelecek)
)


package com.videocall.app.model

data class CallStatistics(
    val totalCalls: Int = 0,
    val totalDuration: Long = 0, // saniye cinsinden
    val totalIncomingCalls: Int = 0,
    val totalOutgoingCalls: Int = 0,
    val averageCallDuration: Long = 0, // saniye cinsinden
    val longestCallDuration: Long = 0, // saniye cinsinden
    val favoriteContact: String? = null
) {
    fun getFormattedTotalDuration(): String {
        val hours = totalDuration / 3600
        val minutes = (totalDuration % 3600) / 60
        val seconds = totalDuration % 60
        
        return when {
            hours > 0 -> "${hours}s ${minutes}dk ${seconds}sn"
            minutes > 0 -> "${minutes}dk ${seconds}sn"
            else -> "${seconds}sn"
        }
    }
    
    fun getFormattedAverageDuration(): String {
        val minutes = averageCallDuration / 60
        val seconds = averageCallDuration % 60
        
        return when {
            minutes > 0 -> "${minutes}dk ${seconds}sn"
            else -> "${seconds}sn"
        }
    }
}


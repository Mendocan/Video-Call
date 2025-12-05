package com.videocall.app.signaling

import org.json.JSONObject
import com.videocall.app.directcall.ice.DirectCallIceCandidate

sealed interface SignalingMessage {
    val type: String

    data class Join(val roomCode: String) : SignalingMessage {
        override val type: String = "join"
    }

    data class Leave(val roomCode: String?) : SignalingMessage {
        override val type: String = "leave"
    }

    data class Offer(val sdp: String) : SignalingMessage {
        override val type: String = "offer"
        // DirectCall kullanılıyor - SessionDescription gerekmez, SDP string yeterli
    }

    data class Answer(val sdp: String) : SignalingMessage {
        override val type: String = "answer"
        // DirectCall kullanılıyor - SessionDescription gerekmez, SDP string yeterli
    }

    data class IceCandidatePayload(
        val candidate: String,
        val sdpMid: String?,
        val sdpMLineIndex: Int
    ) {
        fun toDirectCallIceCandidate(): DirectCallIceCandidate? {
            // SDP candidate string'ini parse et
            // Format: "candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host"
            return try {
                val parts = candidate.split(" ")
                if (parts.size >= 8 && parts[0] == "candidate:") {
                    DirectCallIceCandidate(
                        foundation = parts[0].substring(10), // "candidate:" sonrası
                        componentId = parts[1].toIntOrNull() ?: 1,
                        transport = parts[2],
                        priority = parts[3].toLongOrNull() ?: 0L,
                        address = parts[4],
                        port = parts[5].toIntOrNull() ?: 0,
                        type = parts[7] // "typ" sonrası
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("SignalingMessage", "ICE candidate parse hatası", e)
                null
            }
        }
    }

    data class IceCandidateMessage(val payload: IceCandidatePayload) : SignalingMessage {
        override val type: String = "ice"
    }

    data class Presence(val participants: Int) : SignalingMessage {
        override val type: String = "presence"
    }

    data class Error(val reason: String) : SignalingMessage {
        override val type: String = "error"
    }

    data class Auth(val token: String, val phoneHash: String) : SignalingMessage {
        override val type: String = "auth"
    }
    
    data class Chat(
        val message: String, 
        val senderPhoneNumber: String, 
        val senderName: String?,
        val targetPhoneNumber: String? = null, // Kişiye özel chat için
        val messageId: String? = null, // Mesaj ID (düzenleme için)
        val timestamp: Long = System.currentTimeMillis() // Mesaj zamanı
    ) : SignalingMessage {
        override val type: String = "chat"
    }
    
    // Mesaj düzenleme mesajı
    data class ChatEdit(
        val messageId: String,
        val newMessage: String,
        val senderPhoneNumber: String,
        val targetPhoneNumber: String
    ) : SignalingMessage {
        override val type: String = "chat-edit"
    }
    
    // Mesaj durumu güncelleme mesajı
    data class MessageStatusUpdate(
        val messageId: String,
        val status: String, // "sent", "delivered", "read"
        val targetPhoneNumber: String
    ) : SignalingMessage {
        override val type: String = "message-status"
    }
    
    // Mesaj silme mesajı
    data class ChatDelete(
        val messageId: String,
        val senderPhoneNumber: String,
        val targetPhoneNumber: String
    ) : SignalingMessage {
        override val type: String = "chat-delete"
    }
    
    // Canlı yayın mesajları
    data class StartLive(
        val title: String? = null,
        val targetPhoneNumbers: List<String>? = null, // Kişiler için
        val groupIds: List<String>? = null, // Gruplar için
        val broadcasterPhoneNumber: String,
        val broadcasterName: String?
    ) : SignalingMessage {
        override val type: String = "start-live"
    }
    
    data class LiveStarted(
        val liveId: String,
        val roomCode: String,
        val broadcasterPhoneNumber: String,
        val broadcasterName: String?,
        val title: String?,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "live-started"
    }
    
    data class JoinLive(
        val liveId: String,
        val viewerPhoneNumber: String,
        val viewerName: String?
    ) : SignalingMessage {
        override val type: String = "join-live"
    }
    
    data class LiveJoined(
        val liveId: String,
        val roomCode: String,
        val broadcasterPhoneNumber: String,
        val broadcasterName: String?,
        val title: String?,
        val viewerCount: Int,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "live-joined"
    }
    
    data class EndLive(
        val liveId: String,
        val broadcasterPhoneNumber: String
    ) : SignalingMessage {
        override val type: String = "end-live"
    }
    
    data class LiveEnded(
        val liveId: String,
        val broadcasterPhoneNumber: String,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "live-ended"
    }
    
    data class LiveViewerJoined(
        val liveId: String,
        val viewerPhoneNumber: String,
        val viewerName: String?,
        val viewerCount: Int,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "live-viewer-joined"
    }
    
    data class LiveViewerLeft(
        val liveId: String,
        val viewerPhoneNumber: String,
        val viewerCount: Int,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "live-viewer-left"
    }
    
    // Kayıt durumu bildirimi
    data class RecordingStatus(
        val isRecording: Boolean,
        val senderPhoneNumber: String,
        val roomCode: String? = null // Görüşme için room code, chat için null
    ) : SignalingMessage {
        override val type: String = "recording-status"
    }
    
    data class IncomingLive(
        val liveId: String,
        val roomCode: String,
        val broadcasterPhoneNumber: String,
        val broadcasterName: String?,
        val title: String?,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "incoming-live"
    }
    
    data class FileShare(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String,
        val senderPhoneNumber: String,
        val senderName: String?,
        val chunkIndex: Int = 0,
        val totalChunks: Int = 1,
        val chunkData: String? = null, // Base64 encoded chunk data
        val isMetadata: Boolean = true // true = metadata, false = chunk data
    ) : SignalingMessage {
        override val type: String = "file"
    }

    // Kullanıcı kayıt mesajları
    data class Register(val phoneNumber: String, val name: String?, val otp: String? = null) : SignalingMessage {
        override val type: String = "register"
    }

    data class Registered(val phoneNumber: String, val name: String?, val timestamp: String) : SignalingMessage {
        override val type: String = "registered"
    }

    data class RegisterError(val message: String, val originalPhoneNumber: String? = null) : SignalingMessage {
        override val type: String = "register-error"
    }

    // Login/Logout mesajları
    data class LoggedIn(val phoneNumber: String, val name: String?, val timestamp: String) : SignalingMessage {
        override val type: String = "logged-in"
    }

    data class LoggedOut(val phoneNumber: String, val timestamp: String) : SignalingMessage {
        override val type: String = "logged-out"
    }

    data class Logout(val phoneNumber: String = "") : SignalingMessage {
        override val type: String = "logout"
    }

    // OTP mesajları
    data class RequestOTP(val phoneNumber: String) : SignalingMessage {
        override val type: String = "request-otp"
    }

    data class OTPSent(val phoneNumber: String, val testOTP: String? = null) : SignalingMessage {
        override val type: String = "otp-sent"
    }

    data class OTPError(val message: String) : SignalingMessage {
        override val type: String = "otp-error"
    }

    // Firebase kaldırıldı - WebSocket kullanıyoruz (bağımsız yapı)

    // Arama mesajları
    data class CallRequest(
        val targetPhoneNumber: String? = null,
        val groupId: String? = null,
        val callerPhoneNumber: String,
        val callerName: String?
    ) : SignalingMessage {
        override val type: String = "call-request"
    }

    data class CallRequestSent(
        val groupId: String,
        val roomCode: String? = null,
        val targetPhoneNumber: String? = null,
        val notifiedCount: Int? = null,
        val totalMembers: Int? = null,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "call-request-sent"
    }

    data class IncomingCall(
        val groupId: String,
        val roomCode: String,
        val callerPhoneNumber: String,
        val callerName: String?,
        val isGroupCall: Boolean,
        val groupName: String? = null,
        val memberCount: Int? = null,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "incoming-call"
    }

    data class CallAccept(val groupId: String) : SignalingMessage {
        override val type: String = "call-accept"
    }

    data class CallReject(val groupId: String) : SignalingMessage {
        override val type: String = "call-reject"
    }

    data class CallAccepted(
        val groupId: String,
        val roomCode: String,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "call-accepted"
    }

    data class CallRejected(
        val groupId: String,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "call-rejected"
    }

    data class CallAcceptedBy(
        val groupId: String,
        val roomCode: String,
        val phoneNumber: String,
        val name: String?,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "call-accepted-by"
    }

    data class CallRejectedBy(
        val groupId: String,
        val phoneNumber: String,
        val name: String?,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "call-rejected-by"
    }

    data class CallError(val reason: String, val targetPhoneNumber: String? = null) : SignalingMessage {
        override val type: String = "call-error"
    }

    data class Ringing(
        val targetPhoneNumber: String,
        val timestamp: String,
        override val type: String = "ringing"
    ) : SignalingMessage

    // Grup yönetim mesajları
    data class CreateGroup(val groupName: String, val memberPhoneNumbers: List<String>) : SignalingMessage {
        override val type: String = "create-group"
    }

    data class GroupCreated(
        val groupId: String,
        val groupName: String,
        val roomCode: String,
        val members: List<String>,
        val invalidMembers: List<String>? = null,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "group-created"
    }

    data class JoinGroup(val groupId: String) : SignalingMessage {
        override val type: String = "join-group"
    }

    data class GroupJoined(
        val groupId: String,
        val groupName: String,
        val roomCode: String,
        val addedBy: String? = null,
        val members: List<String>? = null,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "group-joined"
    }

    data class LeaveGroup(val groupId: String) : SignalingMessage {
        override val type: String = "leave-group"
    }

    data class GroupLeft(val groupId: String, val timestamp: String) : SignalingMessage {
        override val type: String = "group-left"
    }

    data class GroupMemberJoined(
        val groupId: String,
        val phoneNumber: String,
        val name: String?,
        val memberCount: Int,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "group-member-joined"
    }

    data class GroupMemberLeft(
        val groupId: String,
        val phoneNumber: String,
        val name: String?,
        val memberCount: Int,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "group-member-left"
    }

    data class GetGroups(val dummy: String = "") : SignalingMessage {
        override val type: String = "get-groups"
    }

    data class GroupsList(val groups: List<GroupListItem>, val count: Int) : SignalingMessage {
        override val type: String = "groups-list"
    }

    data class GetGroupInfo(val groupId: String) : SignalingMessage {
        override val type: String = "get-group-info"
    }

    data class GroupInfo(
        val groupId: String,
        val name: String,
        val roomCode: String,
        val members: List<GroupMemberInfo>,
        val memberCount: Int,
        val createdBy: String,
        val createdAt: String
    ) : SignalingMessage {
        override val type: String = "group-info"
    }

    data class GroupListItem(
        val groupId: String,
        val name: String,
        val roomCode: String,
        val memberCount: Int,
        val createdBy: String,
        val createdAt: String
    )

    data class GroupMemberInfo(
        val phoneNumber: String,
        val name: String?,
        val isOnline: Boolean
    )

    // Kullanıcı durumu mesajları
    data class UserStatusRequest(val phoneNumber: String) : SignalingMessage {
        override val type: String = "user-status"
    }

    // Kullanıcı lookup (keşif) mesajları
    data class UserLookup(val phoneNumber: String) : SignalingMessage {
        override val type: String = "user-lookup"
    }

    data class UserLookupResponse(
        val phoneNumber: String,
        val isRegistered: Boolean,
        val name: String? = null,
        val isOnline: Boolean = false,
        val lastSeen: String? = null
    ) : SignalingMessage {
        override val type: String = "user-lookup"
    }

    data class UserStatusResponse(
        val phoneNumber: String,
        val name: String?,
        val status: String, // "online" or "offline"
        val isOnline: Boolean,
        val lastSeen: String?,
        val connectedAt: String?
    ) : SignalingMessage {
        override val type: String = "user-status"
    }

    // Engellenenler listesi mesajları
    data class BlockUser(val targetPhoneNumber: String) : SignalingMessage {
        override val type: String = "block-user"
    }

    data class UnblockUser(val targetPhoneNumber: String) : SignalingMessage {
        override val type: String = "unblock-user"
    }

    data class GetBlockedUsers(val dummy: String = "") : SignalingMessage {
        override val type: String = "get-blocked-users"
    }

    data class UserBlocked(
        val targetPhoneNumber: String,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "user-blocked"
    }

    data class UserUnblocked(
        val targetPhoneNumber: String,
        val timestamp: String
    ) : SignalingMessage {
        override val type: String = "user-unblocked"
    }

    data class BlockedUsersList(
        val blockedUsers: List<BlockedUserInfo>,
        val count: Int
    ) : SignalingMessage {
        override val type: String = "blocked-users-list"
    }

    data class BlockedUserInfo(
        val phoneNumber: String,
        val name: String?,
        val isOnline: Boolean
    )

    companion object {
        fun fromJson(raw: String): SignalingMessage? {
            val json = JSONObject(raw)
            return when (json.optString("type")) {
                "offer" -> Offer(json.getJSONObject("payload").getString("sdp"))
                "answer" -> Answer(json.getJSONObject("payload").getString("sdp"))
                "ice" -> {
                    val payload = json.getJSONObject("payload")
                    IceCandidateMessage(
                        IceCandidatePayload(
                            candidate = payload.getString("candidate"),
                            sdpMid = payload.optString("sdpMid").takeIf { it.isNotEmpty() },
                            sdpMLineIndex = payload.optInt("sdpMLineIndex")
                        )
                    )
                }
                "presence" -> Presence(json.getJSONObject("payload").getInt("participants"))
                "join" -> Join(json.getJSONObject("payload").getString("room"))
                "leave" -> {
                    val payloadObj = json.optJSONObject("payload")
                    val room = payloadObj?.optString("room")?.takeIf { it.isNotEmpty() }
                    Leave(room)
                }
                "error" -> Error(json.optString("message", "Unknown error occurred"))
                "auth" -> {
                    val payload = json.getJSONObject("payload")
                    Auth(
                        token = payload.getString("token"),
                        phoneHash = payload.getString("phoneHash")
                    )
                }
                "chat" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    Chat(
                        message = payload.getString("message"),
                        senderPhoneNumber = payload.getString("senderPhoneNumber"),
                        senderName = payload.optString("senderName").takeIf { it.isNotEmpty() },
                        targetPhoneNumber = payload.optString("targetPhoneNumber").takeIf { it.isNotEmpty() },
                        messageId = payload.optString("messageId").takeIf { it.isNotEmpty() }
                    )
                }
                "chat-edit" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    ChatEdit(
                        messageId = payload.getString("messageId"),
                        newMessage = payload.getString("newMessage"),
                        senderPhoneNumber = payload.getString("senderPhoneNumber"),
                        targetPhoneNumber = payload.getString("targetPhoneNumber")
                    )
                }
                "message-status" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    MessageStatusUpdate(
                        messageId = payload.getString("messageId"),
                        status = payload.getString("status"),
                        targetPhoneNumber = payload.getString("targetPhoneNumber")
                    )
                }
                "chat-delete" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    ChatDelete(
                        messageId = payload.getString("messageId"),
                        senderPhoneNumber = payload.getString("senderPhoneNumber"),
                        targetPhoneNumber = payload.getString("targetPhoneNumber")
                    )
                }
                "start-live" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    StartLive(
                        title = payload.optString("title").takeIf { it.isNotEmpty() },
                        targetPhoneNumbers = payload.optJSONArray("targetPhoneNumbers")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        },
                        groupIds = payload.optJSONArray("groupIds")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        },
                        broadcasterPhoneNumber = payload.getString("broadcasterPhoneNumber"),
                        broadcasterName = payload.optString("broadcasterName").takeIf { it.isNotEmpty() }
                    )
                }
                "live-started" -> {
                    LiveStarted(
                        liveId = json.getString("liveId"),
                        roomCode = json.getString("roomCode"),
                        broadcasterPhoneNumber = json.getString("broadcasterPhoneNumber"),
                        broadcasterName = json.optString("broadcasterName").takeIf { it.isNotEmpty() },
                        title = json.optString("title").takeIf { it.isNotEmpty() },
                        timestamp = json.getString("timestamp")
                    )
                }
                "join-live" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    JoinLive(
                        liveId = payload.getString("liveId"),
                        viewerPhoneNumber = payload.getString("viewerPhoneNumber"),
                        viewerName = payload.optString("viewerName").takeIf { it.isNotEmpty() }
                    )
                }
                "live-joined" -> {
                    LiveJoined(
                        liveId = json.getString("liveId"),
                        roomCode = json.getString("roomCode"),
                        broadcasterPhoneNumber = json.getString("broadcasterPhoneNumber"),
                        broadcasterName = json.optString("broadcasterName").takeIf { it.isNotEmpty() },
                        title = json.optString("title").takeIf { it.isNotEmpty() },
                        viewerCount = json.getInt("viewerCount"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "end-live" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    EndLive(
                        liveId = payload.getString("liveId"),
                        broadcasterPhoneNumber = payload.getString("broadcasterPhoneNumber")
                    )
                }
                "live-ended" -> {
                    LiveEnded(
                        liveId = json.getString("liveId"),
                        broadcasterPhoneNumber = json.getString("broadcasterPhoneNumber"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "incoming-live" -> {
                    IncomingLive(
                        liveId = json.getString("liveId"),
                        roomCode = json.getString("roomCode"),
                        broadcasterPhoneNumber = json.getString("broadcasterPhoneNumber"),
                        broadcasterName = json.optString("broadcasterName").takeIf { it.isNotEmpty() },
                        title = json.optString("title").takeIf { it.isNotEmpty() },
                        timestamp = json.getString("timestamp")
                    )
                }
                "live-viewer-joined" -> {
                    LiveViewerJoined(
                        liveId = json.getString("liveId"),
                        viewerPhoneNumber = json.getString("viewerPhoneNumber"),
                        viewerName = json.optString("viewerName").takeIf { it.isNotEmpty() },
                        viewerCount = json.getInt("viewerCount"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "live-viewer-left" -> {
                    LiveViewerLeft(
                        liveId = json.getString("liveId"),
                        viewerPhoneNumber = json.getString("viewerPhoneNumber"),
                        viewerCount = json.getInt("viewerCount"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "recording-status" -> {
                    RecordingStatus(
                        isRecording = json.getBoolean("isRecording"),
                        senderPhoneNumber = json.getString("senderPhoneNumber"),
                        roomCode = json.optString("roomCode").takeIf { it.isNotEmpty() }
                    )
                }
                "file" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    FileShare(
                        fileId = payload.getString("fileId"),
                        fileName = payload.getString("fileName"),
                        fileSize = payload.getLong("fileSize"),
                        mimeType = payload.getString("mimeType"),
                        senderPhoneNumber = payload.getString("senderPhoneNumber"),
                        senderName = payload.optString("senderName").takeIf { it.isNotEmpty() },
                        chunkIndex = payload.optInt("chunkIndex", 0),
                        totalChunks = payload.optInt("totalChunks", 1),
                        chunkData = payload.optString("chunkData").takeIf { it.isNotEmpty() },
                        isMetadata = payload.optBoolean("isMetadata", true)
                    )
                }
                // Yeni mesaj tipleri (backend'den gelenler payload wrapper olmadan)
                "registered" -> {
                    Registered(
                        phoneNumber = json.getString("phoneNumber"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        timestamp = json.getString("timestamp")
                    )
                }
                "logged-in" -> {
                    LoggedIn(
                        phoneNumber = json.getString("phoneNumber"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        timestamp = json.getString("timestamp")
                    )
                }
                "logged-out" -> {
                    LoggedOut(
                        phoneNumber = json.getString("phoneNumber"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "request-otp" -> {
                    RequestOTP(phoneNumber = json.getString("phoneNumber"))
                }
                "otp-sent" -> {
                    OTPSent(
                        phoneNumber = json.getString("phoneNumber"),
                        testOTP = json.optString("testOTP").takeIf { it.isNotEmpty() }
                    )
                }
                "otp-error" -> {
                    OTPError(message = json.getString("message"))
                }
                "register-error" -> {
                    RegisterError(
                        message = json.getString("message"),
                        originalPhoneNumber = json.optString("originalPhoneNumber").takeIf { it.isNotEmpty() }
                    )
                }
                // Firebase kaldırıldı - WebSocket kullanıyoruz (bağımsız yapı)
                "call-request-sent" -> {
                    CallRequestSent(
                        groupId = json.getString("groupId"),
                        roomCode = json.optString("roomCode").takeIf { it.isNotEmpty() },
                        targetPhoneNumber = json.optString("targetPhoneNumber").takeIf { it.isNotEmpty() },
                        notifiedCount = json.optInt("notifiedCount").takeIf { it > 0 },
                        totalMembers = json.optInt("totalMembers").takeIf { it > 0 },
                        timestamp = json.getString("timestamp")
                    )
                }
                "incoming-call" -> {
                    IncomingCall(
                        groupId = json.getString("groupId"),
                        roomCode = json.getString("roomCode"),
                        callerPhoneNumber = json.getString("callerPhoneNumber"),
                        callerName = json.optString("callerName").takeIf { it.isNotEmpty() },
                        isGroupCall = json.getBoolean("isGroupCall"),
                        groupName = json.optString("groupName").takeIf { it.isNotEmpty() },
                        memberCount = json.optInt("memberCount").takeIf { it > 0 },
                        timestamp = json.getString("timestamp")
                    )
                }
                "call-accepted" -> {
                    CallAccepted(
                        groupId = json.getString("groupId"),
                        roomCode = json.getString("roomCode"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "call-rejected" -> {
                    CallRejected(
                        groupId = json.getString("groupId"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "ringing" -> {
                    Ringing(
                        targetPhoneNumber = json.getString("targetPhoneNumber"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "call-accepted-by" -> {
                    CallAcceptedBy(
                        groupId = json.getString("groupId"),
                        roomCode = json.getString("roomCode"),
                        phoneNumber = json.getString("phoneNumber"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        timestamp = json.getString("timestamp")
                    )
                }
                "call-rejected-by" -> {
                    CallRejectedBy(
                        groupId = json.getString("groupId"),
                        phoneNumber = json.getString("phoneNumber"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        timestamp = json.getString("timestamp")
                    )
                }
                "call-error" -> {
                    CallError(
                        reason = json.getString("reason") ?: json.optString("message", "Unknown error"),
                        targetPhoneNumber = json.optString("targetPhoneNumber").takeIf { it.isNotEmpty() }
                    )
                }
                "group-created" -> {
                    val membersArray = json.getJSONArray("members")
                    val members = (0 until membersArray.length()).map { membersArray.getString(it) }
                    val invalidMembersArray = json.optJSONArray("invalidMembers")
                    val invalidMembers = invalidMembersArray?.let { arr ->
                        (0 until arr.length()).map { index -> arr.getString(index) }
                    }
                    GroupCreated(
                        groupId = json.getString("groupId"),
                        groupName = json.getString("groupName"),
                        roomCode = json.getString("roomCode"),
                        members = members,
                        invalidMembers = invalidMembers,
                        timestamp = json.getString("timestamp")
                    )
                }
                "group-joined" -> {
                    val membersArray = json.optJSONArray("members")
                    val members = membersArray?.let { arr ->
                        (0 until arr.length()).map { index -> arr.getString(index) }
                    }
                    GroupJoined(
                        groupId = json.getString("groupId"),
                        groupName = json.getString("groupName"),
                        roomCode = json.getString("roomCode"),
                        addedBy = json.optString("addedBy").takeIf { it.isNotEmpty() },
                        members = members,
                        timestamp = json.getString("timestamp")
                    )
                }
                "group-left" -> {
                    GroupLeft(
                        groupId = json.getString("groupId"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "group-member-joined" -> {
                    GroupMemberJoined(
                        groupId = json.getString("groupId"),
                        phoneNumber = json.getString("phoneNumber"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        memberCount = json.getInt("memberCount"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "group-member-left" -> {
                    GroupMemberLeft(
                        groupId = json.getString("groupId"),
                        phoneNumber = json.getString("phoneNumber"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        memberCount = json.getInt("memberCount"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "groups-list" -> {
                    val groupsArray = json.getJSONArray("groups")
                    val groups = (0 until groupsArray.length()).map {
                        val groupObj = groupsArray.getJSONObject(it)
                        GroupListItem(
                            groupId = groupObj.getString("groupId"),
                            name = groupObj.getString("name"),
                            roomCode = groupObj.getString("roomCode"),
                            memberCount = groupObj.getInt("memberCount"),
                            createdBy = groupObj.getString("createdBy"),
                            createdAt = groupObj.getString("createdAt")
                        )
                    }
                    GroupsList(
                        groups = groups,
                        count = json.getInt("count")
                    )
                }
                "group-info" -> {
                    val membersArray = json.getJSONArray("members")
                    val members = (0 until membersArray.length()).map { index ->
                        val memberObj = membersArray.getJSONObject(index)
                        GroupMemberInfo(
                            phoneNumber = memberObj.getString("phoneNumber"),
                            name = memberObj.optString("name").takeIf { name -> name.isNotEmpty() },
                            isOnline = memberObj.getBoolean("isOnline")
                        )
                    }
                    GroupInfo(
                        groupId = json.getString("groupId"),
                        name = json.getString("name"),
                        roomCode = json.getString("roomCode"),
                        members = members,
                        memberCount = json.getInt("memberCount"),
                        createdBy = json.getString("createdBy"),
                        createdAt = json.getString("createdAt")
                    )
                }
                "user-status" -> {
                    UserStatusResponse(
                        phoneNumber = json.getString("phoneNumber"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        status = json.getString("status"),
                        isOnline = json.getBoolean("isOnline"),
                        lastSeen = json.optString("lastSeen").takeIf { it.isNotEmpty() },
                        connectedAt = json.optString("connectedAt").takeIf { it.isNotEmpty() }
                    )
                }
                "user-lookup" -> {
                    UserLookupResponse(
                        phoneNumber = json.getString("phoneNumber"),
                        isRegistered = json.getBoolean("isRegistered"),
                        name = json.optString("name").takeIf { it.isNotEmpty() },
                        isOnline = json.optBoolean("isOnline", false),
                        lastSeen = json.optString("lastSeen").takeIf { it.isNotEmpty() }
                    )
                }
                "user-blocked" -> {
                    UserBlocked(
                        targetPhoneNumber = json.getString("targetPhoneNumber"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "user-unblocked" -> {
                    UserUnblocked(
                        targetPhoneNumber = json.getString("targetPhoneNumber"),
                        timestamp = json.getString("timestamp")
                    )
                }
                "blocked-users-list" -> {
                    val blockedUsersArray = json.getJSONArray("blockedUsers")
                    val blockedUsers = (0 until blockedUsersArray.length()).map { index ->
                        val userObj = blockedUsersArray.getJSONObject(index)
                        BlockedUserInfo(
                            phoneNumber = userObj.getString("phoneNumber"),
                            name = userObj.optString("name").takeIf { name -> name.isNotEmpty() },
                            isOnline = userObj.getBoolean("isOnline")
                        )
                    }
                    BlockedUsersList(
                        blockedUsers = blockedUsers,
                        count = json.getInt("count")
                    )
                }
                else -> null
            }
        }

        fun toJson(message: SignalingMessage): String {
            val json = JSONObject()
            json.put("type", message.type)
            
            // Backend payload wrapper beklememiş, doğrudan mesaj alanlarını gönder
            when (message) {
                is Offer -> {
                    json.put("sdp", message.sdp)
                }
                is Answer -> {
                    json.put("sdp", message.sdp)
                }
                is IceCandidateMessage -> {
                    val payload = JSONObject()
                    payload.put("candidate", message.payload.candidate)
                    payload.put("sdpMid", message.payload.sdpMid)
                    payload.put("sdpMLineIndex", message.payload.sdpMLineIndex)
                    json.put("payload", payload)
                }
                is Presence -> {
                    val payload = JSONObject()
                    payload.put("participants", message.participants)
                    json.put("payload", payload)
                }
                is Error -> {
                    json.put("message", message.reason)
                }
                is Join -> {
                    val payload = JSONObject()
                    payload.put("room", message.roomCode)
                    json.put("payload", payload)
                }
                is Leave -> {
                    val payload = JSONObject()
                    message.roomCode?.let { payload.put("room", it) }
                    json.put("payload", payload)
                }
                is Auth -> {
                    val payload = JSONObject()
                    payload.put("token", message.token)
                    payload.put("phoneHash", message.phoneHash)
                    json.put("payload", payload)
                }
                is Chat -> {
                    json.put("message", message.message)
                    json.put("senderPhoneNumber", message.senderPhoneNumber)
                    message.senderName?.let { json.put("senderName", it) }
                    message.targetPhoneNumber?.let { json.put("targetPhoneNumber", it) }
                    message.messageId?.let { json.put("messageId", it) }
                    json.put("timestamp", message.timestamp)
                }
                is ChatEdit -> {
                    json.put("messageId", message.messageId)
                    json.put("newMessage", message.newMessage)
                    json.put("senderPhoneNumber", message.senderPhoneNumber)
                    json.put("targetPhoneNumber", message.targetPhoneNumber)
                }
                is MessageStatusUpdate -> {
                    json.put("messageId", message.messageId)
                    json.put("status", message.status)
                    json.put("targetPhoneNumber", message.targetPhoneNumber)
                }
                is ChatDelete -> {
                    json.put("messageId", message.messageId)
                    json.put("senderPhoneNumber", message.senderPhoneNumber)
                    json.put("targetPhoneNumber", message.targetPhoneNumber)
                }
                is StartLive -> {
                    message.title?.let { json.put("title", it) }
                    message.targetPhoneNumbers?.let { numbers ->
                        val arr = org.json.JSONArray()
                        numbers.forEach { arr.put(it) }
                        json.put("targetPhoneNumbers", arr)
                    }
                    message.groupIds?.let { ids ->
                        val arr = org.json.JSONArray()
                        ids.forEach { arr.put(it) }
                        json.put("groupIds", arr)
                    }
                    json.put("broadcasterPhoneNumber", message.broadcasterPhoneNumber)
                    message.broadcasterName?.let { json.put("broadcasterName", it) }
                }
                is LiveStarted -> {
                    json.put("liveId", message.liveId)
                    json.put("roomCode", message.roomCode)
                    json.put("broadcasterPhoneNumber", message.broadcasterPhoneNumber)
                    message.broadcasterName?.let { json.put("broadcasterName", it) }
                    message.title?.let { json.put("title", it) }
                    json.put("timestamp", message.timestamp)
                }
                is JoinLive -> {
                    json.put("liveId", message.liveId)
                    json.put("viewerPhoneNumber", message.viewerPhoneNumber)
                    message.viewerName?.let { json.put("viewerName", it) }
                }
                is LiveJoined -> {
                    json.put("liveId", message.liveId)
                    json.put("roomCode", message.roomCode)
                    json.put("broadcasterPhoneNumber", message.broadcasterPhoneNumber)
                    message.broadcasterName?.let { json.put("broadcasterName", it) }
                    message.title?.let { json.put("title", it) }
                    json.put("viewerCount", message.viewerCount)
                    json.put("timestamp", message.timestamp)
                }
                is EndLive -> {
                    json.put("liveId", message.liveId)
                    json.put("broadcasterPhoneNumber", message.broadcasterPhoneNumber)
                }
                is LiveEnded -> {
                    json.put("liveId", message.liveId)
                    json.put("broadcasterPhoneNumber", message.broadcasterPhoneNumber)
                    json.put("timestamp", message.timestamp)
                }
                is IncomingLive -> {
                    json.put("liveId", message.liveId)
                    json.put("roomCode", message.roomCode)
                    json.put("broadcasterPhoneNumber", message.broadcasterPhoneNumber)
                    message.broadcasterName?.let { json.put("broadcasterName", it) }
                    message.title?.let { json.put("title", it) }
                    json.put("timestamp", message.timestamp)
                }
                is LiveViewerJoined -> {
                    json.put("liveId", message.liveId)
                    json.put("viewerPhoneNumber", message.viewerPhoneNumber)
                    message.viewerName?.let { json.put("viewerName", it) }
                    json.put("viewerCount", message.viewerCount)
                    json.put("timestamp", message.timestamp)
                }
                is LiveViewerLeft -> {
                    json.put("liveId", message.liveId)
                    json.put("viewerPhoneNumber", message.viewerPhoneNumber)
                    json.put("viewerCount", message.viewerCount)
                    json.put("timestamp", message.timestamp)
                }
                is RecordingStatus -> {
                    json.put("isRecording", message.isRecording)
                    json.put("senderPhoneNumber", message.senderPhoneNumber)
                    message.roomCode?.let { json.put("roomCode", it) }
                }
                is FileShare -> {
                    json.put("fileId", message.fileId)
                    json.put("fileName", message.fileName)
                    json.put("fileSize", message.fileSize)
                    json.put("mimeType", message.mimeType)
                    json.put("senderPhoneNumber", message.senderPhoneNumber)
                    message.senderName?.let { json.put("senderName", it) }
                    json.put("chunkIndex", message.chunkIndex)
                    json.put("totalChunks", message.totalChunks)
                    message.chunkData?.let { json.put("chunkData", it) }
                    json.put("isMetadata", message.isMetadata)
                }
                // Yeni mesaj tipleri (backend formatına uygun)
                is Register -> {
                    json.put("phoneNumber", message.phoneNumber)
                    message.name?.let { json.put("name", it) }
                    message.otp?.let { json.put("otp", it) }
                }
                // RegisterError sadece backend'den gelir, client'tan gönderilmez
                is RequestOTP -> {
                    json.put("phoneNumber", message.phoneNumber)
                }
                // Firebase kaldırıldı - WebSocket kullanıyoruz (bağımsız yapı)
                is CallRequest -> {
                    message.targetPhoneNumber?.let { json.put("targetPhoneNumber", it) }
                    message.groupId?.let { json.put("groupId", it) }
                    json.put("callerPhoneNumber", message.callerPhoneNumber)
                    message.callerName?.let { json.put("callerName", it) }
                }
                is CallAccept -> {
                    json.put("groupId", message.groupId)
                }
                is CallReject -> {
                    json.put("groupId", message.groupId)
                }
                is CreateGroup -> {
                    json.put("groupName", message.groupName)
                    val membersArray = org.json.JSONArray()
                    message.memberPhoneNumbers.forEach { membersArray.put(it) }
                    json.put("memberPhoneNumbers", membersArray)
                }
                is JoinGroup -> {
                    json.put("groupId", message.groupId)
                }
                is LeaveGroup -> {
                    json.put("groupId", message.groupId)
                }
                is GetGroups -> {
                    // Boş mesaj, sadece type yeterli
                }
                is GetGroupInfo -> {
                    json.put("groupId", message.groupId)
                }
                is UserStatusRequest -> {
                    json.put("phoneNumber", message.phoneNumber)
                }
                is UserLookup -> {
                    json.put("phoneNumber", message.phoneNumber)
                }
                is BlockUser -> {
                    json.put("targetPhoneNumber", message.targetPhoneNumber)
                }
                is UnblockUser -> {
                    json.put("targetPhoneNumber", message.targetPhoneNumber)
                }
                is GetBlockedUsers -> {
                    // Boş mesaj, sadece type yeterli
                }
                is Logout -> {
                    // Logout mesajı - phoneNumber opsiyonel
                    if (message.phoneNumber.isNotEmpty()) {
                        json.put("phoneNumber", message.phoneNumber)
                    }
                }
                // Backend'den gelen mesajlar (toJson'da kullanılmaz, sadece fromJson'da)
                is Registered,
                is RegisterError,
                is LoggedIn,
                is LoggedOut,
                is CallRequestSent,
                is IncomingCall,
                is CallAccepted,
                is CallRejected,
                is CallAcceptedBy,
                is CallRejectedBy,
                is Ringing,
                is CallError,
                is GroupCreated,
                is GroupJoined,
                is GroupLeft,
                is GroupMemberJoined,
                is GroupMemberLeft,
                is GroupsList,
                is GroupInfo,
                is UserStatusResponse,
                is UserBlocked,
                is UserUnblocked,
                is BlockedUsersList,
                is OTPError,
                is OTPSent,
                is UserLookupResponse -> {
                    // Bu mesajlar backend'den gelir, client'tan gönderilmez
                    // Sadece type yeterli
                }
            }
            return json.toString()
        }
    }
}


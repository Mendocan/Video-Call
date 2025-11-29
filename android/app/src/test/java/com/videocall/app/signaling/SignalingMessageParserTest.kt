package com.videocall.app.signaling

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalingMessageParserTest {

    @Test
    fun `offer json is parsed correctly`() {
        val raw = JSONObject()
            .put("type", "offer")
            .put("payload", JSONObject().put("sdp", "test-sdp"))
            .toString()

        val message = SignalingMessage.fromJson(raw)
        assertTrue(message is SignalingMessage.Offer)
        assertEquals("test-sdp", (message as SignalingMessage.Offer).sdp)
    }

    @Test
    fun `ice candidate serialization roundtrip`() {
        val message = SignalingMessage.IceCandidateMessage(
            SignalingMessage.IceCandidatePayload(
                candidate = "candidate:1 1 UDP 2122197247 192.168.0.2 64044 typ host",
                sdpMid = "0",
                sdpMLineIndex = 0
            )
        )

        val encoded = SignalingMessage.toJson(message)
        val decoded = SignalingMessage.fromJson(encoded)

        assertTrue(decoded is SignalingMessage.IceCandidateMessage)
        val payload = (decoded as SignalingMessage.IceCandidateMessage).payload
        assertEquals(message.payload.candidate, payload.candidate)
        assertEquals(message.payload.sdpMid, payload.sdpMid)
        assertEquals(message.payload.sdpMLineIndex, payload.sdpMLineIndex)
    }
}


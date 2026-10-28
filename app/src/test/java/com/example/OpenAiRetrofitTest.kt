package com.example

import com.example.data.remote.openai.OpenAiChatRequest
import com.example.data.remote.openai.OpenAiChatResponse
import com.example.data.remote.openai.OpenAiChoice
import com.example.data.remote.openai.OpenAiMessage
import com.example.data.remote.openai.OpenAiResponseFormat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenAiRetrofitTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testOpenAiChatRequestSerialization() {
        val request = OpenAiChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAiMessage(role = "system", content = "You are JARVIS"),
                OpenAiMessage(role = "user", content = "YouTube kholo")
            ),
            temperature = 0.3,
            responseFormat = OpenAiResponseFormat("json_object")
        )

        val adapter = moshi.adapter(OpenAiChatRequest::class.java)
        val json = adapter.toJson(request)

        assertNotNull(json)
        assert(json.contains("gpt-4o-mini"))
        assert(json.contains("json_object"))
    }

    @Test
    fun testOpenAiChatResponseDeserialization() {
        val sampleJson = """
            {
              "id": "chatcmpl-123",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "{\"actionType\":\"OPEN_SETTINGS\"}"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(OpenAiChatResponse::class.java)
        val response = adapter.fromJson(sampleJson)

        assertNotNull(response)
        assertEquals("chatcmpl-123", response?.id)
        assertEquals(1, response?.choices?.size)
        assertEquals("assistant", response?.choices?.firstOrNull()?.message?.role)
        assertEquals("{\"actionType\":\"OPEN_SETTINGS\"}", response?.choices?.firstOrNull()?.message?.content)
    }
}

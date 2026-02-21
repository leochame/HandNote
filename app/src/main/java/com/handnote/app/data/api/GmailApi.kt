package com.handnote.app.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Gmail API REST 接口
 * 文档: https://developers.google.com/gmail/api/reference/rest
 */
interface GmailApi {
    @GET("gmail/v1/users/me/messages")
    suspend fun listMessages(
        @Query("q") query: String = "is:unread",
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null
    ): Response<GmailMessageListResponse>

    @GET("gmail/v1/users/me/messages/{id}")
    suspend fun getMessage(
        @Path("id") messageId: String,
        @Query("format") format: String = "full"
    ): Response<GmailMessage>
}

data class GmailMessageListResponse(
    val messages: List<GmailMessageRef>? = null,
    val nextPageToken: String? = null,
    val resultSizeEstimate: Int? = null
)

data class GmailMessageRef(
    val id: String,
    val threadId: String? = null
)

data class GmailMessage(
    val id: String,
    val threadId: String? = null,
    val labelIds: List<String>? = null,
    val snippet: String? = null,
    val payload: GmailMessagePart? = null,
    val internalDate: String? = null
)

data class GmailMessagePart(
    val partId: String? = null,
    val mimeType: String? = null,
    val filename: String? = null,
    val headers: List<GmailHeader>? = null,
    val body: GmailMessageBody? = null,
    val parts: List<GmailMessagePart>? = null
)

data class GmailHeader(
    val name: String,
    val value: String
)

data class GmailMessageBody(
    val attachmentId: String? = null,
    val size: Int? = null,
    val data: String? = null  // Base64url encoded
)


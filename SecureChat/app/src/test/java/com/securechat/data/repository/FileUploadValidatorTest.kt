package com.securechat.data.repository

import com.securechat.domain.model.MessageType
import com.securechat.domain.model.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileUploadValidatorTest {

    @Test
    fun validateForUpload_happyPath_imageUnderLimit_returnsSuccessAndImageType() {
        val result = FileUploadValidator.validateForUpload(
            fileSizeBytes = 2L * 1024L * 1024L,
            mimeType = "image/png"
        )
        val messageType = FileUploadValidator.resolveMessageType("image/png")

        assertTrue(result is Resource.Success)
        assertEquals(MessageType.IMAGE, messageType)
    }

    @Test
    fun validateForUpload_failure_fileTooLarge_returnsError() {
        val result = FileUploadValidator.validateForUpload(
            fileSizeBytes = 26L * 1024L * 1024L,
            mimeType = "application/pdf"
        )

        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("25MB"))
    }
}


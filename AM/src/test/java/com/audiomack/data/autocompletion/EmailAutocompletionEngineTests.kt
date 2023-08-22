package com.audiomack.data.autocompletion

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailAutocompletionEngineTests {

    @Test
    fun testEmailAutocompletionNoSuffix() {
        assertEquals("", EmailAutocompletionEngine().getCompletionForPrefix("user", true))
    }

    @Test
    fun testEmailAutocompletionEmpty() {
        assertEquals("gmail.com", EmailAutocompletionEngine().getCompletionForPrefix("user@", true))
    }

    @Test
    fun testEmailAutocompletionGmail() {
        assertEquals("mail.com", EmailAutocompletionEngine().getCompletionForPrefix("user@g", true))
    }

    @Test
    fun testEmailAutocompletionGmailBis() {
        assertEquals(".com", EmailAutocompletionEngine().getCompletionForPrefix("user@gmail", true))
    }

    @Test
    fun testEmailAutocompletionDoubleAt() {
        assertEquals("", EmailAutocompletionEngine().getCompletionForPrefix("user@@", true))
    }

    @Test
    fun testEmailAutocompletionNoFirstPart() {
        assertEquals("", EmailAutocompletionEngine().getCompletionForPrefix("@", true))
    }
}

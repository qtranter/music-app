package com.audiomack.data.sociallink

import com.audiomack.network.APIInterface
import com.audiomack.network.LoginProviderData
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SocialLinkRepositoryTest {

    private lateinit var sut: SocialLinkRepository

    @Mock private lateinit var api: APIInterface.SocialLinkInterface

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = SocialLinkRepository(api)
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `link twitter`() {
        val token = "123"
        val secret = "asd"
        sut.linkTwitter(token, secret)
        verify(api).linkSocial(eq(LoginProviderData.Twitter(token, secret)))
    }

    @Test
    fun `link instagram`() {
        val token = "123"
        sut.linkInstagram(token)
        verify(api).linkSocial(eq(LoginProviderData.Instagram(token)))
    }
}

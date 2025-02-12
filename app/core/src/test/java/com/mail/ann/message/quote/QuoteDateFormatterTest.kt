package com.mail.ann.message.quote

import com.mail.ann.Ann
import com.google.common.truth.Truth.assertThat
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

class QuoteDateFormatterTest {
    private lateinit var originalLocale: Locale
    private var originalTimeZone: TimeZone? = null
    private val quoteDateFormatter = QuoteDateFormatter()

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun hideTimeZoneEnabled_UsLocale() {
        Ann.isHideTimeZone = true
        Locale.setDefault(Locale.US)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate).isEqualTo("September 19, 2020 at 8:00:00 PM UTC")
    }

    @Test
    fun hideTimeZoneEnabled_GermanyLocale() {
        Ann.isHideTimeZone = true
        Locale.setDefault(Locale.GERMANY)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate).isEqualTo("19. September 2020 um 20:00:00 UTC")
    }

    @Test
    fun hideTimeZoneDisabled_UsLocale() {
        Ann.isHideTimeZone = false
        Locale.setDefault(Locale.US)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate).isEqualTo("September 19, 2020 at 10:00:00 PM GMT+02:00")
    }

    @Test
    fun hideTimeZoneDisabled_GermanyLocale() {
        Ann.isHideTimeZone = false
        Locale.setDefault(Locale.GERMANY)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate).isEqualTo("19. September 2020 um 22:00:00 GMT+02:00")
    }

    private fun String.toDate() = Date(ZonedDateTime.parse(this).toEpochSecond() * 1000L)
}

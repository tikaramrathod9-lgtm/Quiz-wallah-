package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppConfig
import com.example.data.model.UserProfile
import com.example.data.payment.PaymentManager
import com.example.data.repository.MembershipPlan
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Edustudy pro", appName)
    }

    @Test
    fun `test owner mobile number 6261371961 has full admin and membership validity`() {
        val ownerByPhone = UserProfile(
            userId = "owner_uid",
            email = "any@gmail.com",
            phoneNumber = "6261371961",
            displayName = "Owner",
            role = "student",
            membershipActive = false
        )
        assertTrue(ownerByPhone.isAdmin)
        assertTrue(ownerByPhone.hasMobileRegistered)
        assertTrue(ownerByPhone.isMembershipValid)
        assertEquals(999, ownerByPhone.daysRemaining)
    }

    @Test
    fun `test student mobile registration and bookmarks`() {
        val student = UserProfile(
            userId = "s2",
            email = "student2@gmail.com",
            phoneNumber = "9876543210",
            pinCode = "1234",
            role = "student",
            membershipActive = true,
            membershipExpiresAt = Timestamp(Date(System.currentTimeMillis() + 10L * 86400000L)),
            bookmarkedLinkIds = "link_math_1,link_phys_2"
        )
        assertFalse(student.isAdmin)
        assertTrue(student.hasMobileRegistered)
        assertTrue(student.isMembershipValid)
        assertTrue(student.daysRemaining >= 9)
        assertEquals(setOf("link_math_1", "link_phys_2"), student.bookmarkedSet)
    }

    @Test
    fun `test payment manager masked vpa and custom app config pricing`() {
        assertEquals("******71961@ybl", PaymentManager.MASKED_VPA_DISPLAY)
        val customConfig = AppConfig(
            monthlyPriceInr = 149L,
            quarterlyPriceInr = 399L,
            annualPriceInr = 999L,
            upiVpa = "6261371961@ybl",
            merchantDisplayName = "Edustudy pro Study Hub"
        )
        val uri = PaymentManager.buildUpiIntentUri(MembershipPlan.MONTHLY, "TEST_TXN", customConfig)
        assertEquals("upi", uri.scheme)
        assertEquals("pay", uri.authority)
        assertEquals("6261371961@ybl", uri.getQueryParameter("pa"))
        assertEquals("149", uri.getQueryParameter("am"))
    }
}

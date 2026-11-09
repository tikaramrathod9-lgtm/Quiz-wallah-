package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val pinCode: String = "",
    val displayName: String = "",
    val role: String = "student", // "admin" or "student"
    val membershipActive: Boolean = false,
    val membershipExpiresAt: Timestamp? = null,
    val planType: String = "free",
    val bookmarkedLinkIds: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    val isAdmin: Boolean
        get() = role == "admin" ||
            email.equals(OWNER_EMAIL, ignoreCase = true) ||
            phoneNumber.trim() == OWNER_MOBILE

    val hasMobileRegistered: Boolean
        get() = phoneNumber.length == 10

    val isMembershipValid: Boolean
        get() {
            if (isAdmin) return true
            if (!membershipActive) return false
            val expiry = membershipExpiresAt ?: return false
            return expiry.toDate().time > System.currentTimeMillis()
        }

    val daysRemaining: Int
        get() {
            if (isAdmin) return 999
            val expiry = membershipExpiresAt ?: return 0
            val diffMs = expiry.toDate().time - System.currentTimeMillis()
            return if (diffMs > 0) (diffMs / (1000 * 60 * 60 * 24)).toInt() else 0
        }

    val bookmarkedSet: Set<String>
        get() = bookmarkedLinkIds.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    companion object {
        const val OWNER_MOBILE = "6261371961"
        const val OWNER_EMAIL = "tikaramrathod9@gmail.com"
    }
}

@IgnoreExtraProperties
data class MobileRegistration(
    @DocumentId
    val id: String = "",
    val phoneNumber: String = "",
    val pinCode: String = "",
    val studentName: String = "",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val membershipActive: Boolean = false,
    val membershipExpiresAt: Timestamp? = null,
    val planType: String = "free",
    val bookmarkedLinkIds: String = "",
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    val isMembershipValid: Boolean
        get() {
            if (phoneNumber == UserProfile.OWNER_MOBILE) return true
            if (!membershipActive) return false
            val expiry = membershipExpiresAt ?: return false
            return expiry.toDate().time > System.currentTimeMillis()
        }

    val daysRemaining: Int
        get() {
            if (phoneNumber == UserProfile.OWNER_MOBILE) return 999
            val expiry = membershipExpiresAt ?: return 0
            val diffMs = expiry.toDate().time - System.currentTimeMillis()
            return if (diffMs > 0) (diffMs / (1000 * 60 * 60 * 24)).toInt() else 0
        }
}

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId
    val id: String = "global_settings",
    val announcementBanner: String = "",
    val monthlyPriceInr: Long = 99L,
    val quarterlyPriceInr: Long = 249L,
    val annualPriceInr: Long = 699L,
    val upiVpa: String = "6261371961@ybl",
    val merchantDisplayName: String = "Edustudy pro Study Hub",
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

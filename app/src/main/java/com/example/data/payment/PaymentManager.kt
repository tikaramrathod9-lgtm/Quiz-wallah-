package com.example.data.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.AppConfig
import com.example.data.repository.MembershipPlan

object PaymentManager {
    // The default target UPI ID where payments are routed.
    // The number is strictly masked in the UI to protect merchant privacy.
    private const val DEFAULT_RECEIVER_VPA = "6261371961@ybl"
    const val MERCHANT_NAME = "Edustudy pro Study Hub"
    const val MASKED_VPA_DISPLAY = "******71961@ybl"

    fun maskVpaForDisplay(rawVpa: String): String {
        val clean = rawVpa.trim().ifBlank { DEFAULT_RECEIVER_VPA }
        val parts = clean.split("@")
        if (parts.size != 2) return MASKED_VPA_DISPLAY
        val userPart = parts[0]
        val handle = parts[1]
        return if (userPart.length > 5) {
            "******" + userPart.takeLast(5) + "@" + handle
        } else {
            "****@" + handle
        }
    }

    fun priceForPlan(plan: MembershipPlan, appConfig: AppConfig? = null): Int {
        if (appConfig == null) return plan.priceInr
        return when (plan) {
            MembershipPlan.MONTHLY -> appConfig.monthlyPriceInr.toInt()
            MembershipPlan.QUARTERLY -> appConfig.quarterlyPriceInr.toInt()
            MembershipPlan.ANNUAL -> appConfig.annualPriceInr.toInt()
        }
    }

    /**
     * Builds an Android UPI intent URI to initiate payment with installed UPI apps
     * (Google Pay, PhonePe, Paytm, BHIM, etc.) without revealing raw contact details in the app UI.
     */
    fun buildUpiIntentUri(
        plan: MembershipPlan,
        transactionNote: String,
        appConfig: AppConfig? = null
    ): Uri {
        val targetVpa = appConfig?.upiVpa?.trim()?.ifBlank { DEFAULT_RECEIVER_VPA } ?: DEFAULT_RECEIVER_VPA
        val merchant = appConfig?.merchantDisplayName?.trim()?.ifBlank { MERCHANT_NAME } ?: MERCHANT_NAME
        val amount = priceForPlan(plan, appConfig)

        return Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", targetVpa)
            .appendQueryParameter("pn", merchant)
            .appendQueryParameter("am", amount.toString())
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", "Edustudy pro ${plan.title} - $transactionNote")
            .build()
    }

    /**
     * Generates a UPI intent to launch installed UPI apps.
     */
    fun createUpiIntent(
        plan: MembershipPlan,
        userRef: String,
        appConfig: AppConfig? = null
    ): Intent {
        val uri = buildUpiIntentUri(plan, userRef, appConfig)
        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Checks if at least one UPI app is installed.
     */
    fun isUpiAppAvailable(context: Context): Boolean {
        val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        val packageManager = context.packageManager
        val resolved = packageManager.queryIntentActivities(testIntent, 0)
        return resolved.isNotEmpty()
    }

    /**
     * Generates a unique transaction reference ID for verification.
     */
    fun generateTransactionRef(userId: String): String {
        val randomSuffix = (100000..999999).random()
        val userPrefix = userId.take(6).uppercase()
        return "TXN_${userPrefix}_$randomSuffix"
    }
}

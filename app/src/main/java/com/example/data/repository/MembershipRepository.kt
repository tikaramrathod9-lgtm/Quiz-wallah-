package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.MobileRegistration
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

enum class MembershipPlan(
    val id: String,
    val title: String,
    val priceInr: Int,
    val durationDays: Int,
    val description: String
) {
    MONTHLY("monthly", "Monthly Pass", 99, 30, "Access all subject links for 30 days"),
    QUARTERLY("quarterly", "Quarterly Pass", 249, 90, "Access all subject links for 90 days (Save 15%)"),
    ANNUAL("annual", "Annual / Exam Pass", 699, 365, "Full academic year access (Best Value)")
}

data class PendingMobileAuth(
    val phoneNumber: String,
    val pinCode: String,
    val studentName: String,
    val isLoginMode: Boolean
)

object MobileAuthSessionStore {
    var pendingAuth: PendingMobileAuth? = null
}

class MembershipRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                    emit(null)
                }
        )
    }

    fun observeAppConfig(): Flow<AppConfig> = flow {
        val docRef = db.collection("app_config").document("global_settings")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AppConfig::class.java) ?: AppConfig()
                    } else {
                        AppConfig()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(AppConfig())
                }
        )
    }

    suspend fun saveAppConfig(config: AppConfig): Result<Unit> {
        val docRef = db.collection("app_config").document("global_settings")
        val payload = mapOf(
            "announcementBanner" to config.announcementBanner.trim(),
            "monthlyPriceInr" to config.monthlyPriceInr,
            "quarterlyPriceInr" to config.quarterlyPriceInr,
            "annualPriceInr" to config.annualPriceInr,
            "upiVpa" to config.upiVpa.trim().ifBlank { "6261371961@ybl" },
            "merchantDisplayName" to config.merchantDisplayName.trim().ifBlank { "Edustudy pro Study Hub" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAllMobileRegistrations(): Flow<List<MobileRegistration>> = flow {
        val colRef = db.collection("mobile_registrations")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MobileRegistration::class.java)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "mobile_registrations")
                    emit(emptyList())
                }
        )
    }

    suspend fun ownerUpdateStudentMembership(
        phoneNumber: String,
        ownerUid: String,
        activate: Boolean,
        durationDays: Int
    ): Result<Unit> {
        val mobileRef = db.collection("mobile_registrations").document(phoneNumber)
        val expiryMillis = System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000L)
        val expiryTimestamp = if (activate) Timestamp(Date(expiryMillis)) else null
        val planType = if (!activate) "free" else when (durationDays) {
            30 -> "monthly"
            90 -> "quarterly"
            else -> "annual"
        }

        return try {
            mobileRef.update(
                mapOf(
                    "membershipActive" to activate,
                    "membershipExpiresAt" to expiryTimestamp,
                    "planType" to planType,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (ownerUid.isNotBlank()) {
                val userRef = db.collection("users").document(ownerUid)
                val userSnap = userRef.get().await()
                if (userSnap.exists()) {
                    userRef.update(
                        mapOf(
                            "membershipActive" to activate,
                            "membershipExpiresAt" to expiryTimestamp,
                            "planType" to planType,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, mobileRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureUserProfile(userId: String, email: String, displayName: String): UserProfile {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            val existing = if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null

            val isEmailAdmin = email.equals(UserProfile.OWNER_EMAIL, ignoreCase = true)
            val isPhoneAdmin = existing?.phoneNumber == UserProfile.OWNER_MOBILE
            val isAdmin = isEmailAdmin || isPhoneAdmin || existing?.role == "admin"
            val defaultPhone = if (isEmailAdmin && existing?.phoneNumber.isNullOrBlank()) UserProfile.OWNER_MOBILE else (existing?.phoneNumber ?: "")
            val role = if (isAdmin) "admin" else "student"

            val baseProfile = if (existing == null) {
                val newProfile = UserProfile(
                    id = userId,
                    userId = userId,
                    email = email,
                    phoneNumber = defaultPhone,
                    pinCode = "",
                    displayName = displayName.ifBlank { if (isAdmin) "Owner Admin" else "Student" },
                    role = role,
                    membershipActive = isAdmin,
                    membershipExpiresAt = if (isAdmin) Timestamp(Date(System.currentTimeMillis() + 1000L * 86400000L)) else null,
                    planType = if (isAdmin) "admin_pass" else "free",
                    bookmarkedLinkIds = ""
                )
                val payload = mapOf(
                    "userId" to userId,
                    "email" to email,
                    "phoneNumber" to defaultPhone,
                    "pinCode" to "",
                    "displayName" to newProfile.displayName,
                    "role" to role,
                    "membershipActive" to newProfile.membershipActive,
                    "membershipExpiresAt" to newProfile.membershipExpiresAt,
                    "planType" to newProfile.planType,
                    "bookmarkedLinkIds" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                newProfile
            } else {
                if (isAdmin && (!existing.membershipActive || existing.role != "admin")) {
                    val updatedMap = mapOf(
                        "role" to "admin",
                        "membershipActive" to true,
                        "phoneNumber" to defaultPhone,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updatedMap).await()
                    existing.copy(role = "admin", membershipActive = true, phoneNumber = defaultPhone)
                } else {
                    existing
                }
            }

            // If user entered mobile login/register credentials on AuthScreen before Google Sign-In, bind them now
            val pending = MobileAuthSessionStore.pendingAuth
            if (pending != null) {
                MobileAuthSessionStore.pendingAuth = null
                val mobileResult = registerOrLoginWithMobile(
                    userId = userId,
                    email = email,
                    phoneNumber = pending.phoneNumber,
                    pinCode = pending.pinCode,
                    studentName = pending.studentName.ifBlank { baseProfile.displayName },
                    isLoginMode = pending.isLoginMode
                )
                mobileResult.getOrDefault(baseProfile)
            } else if (baseProfile.phoneNumber.length == 10) {
                syncFromMobileRegistration(userId, baseProfile)
            } else {
                baseProfile
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            val isOwnerEmail = email.equals(UserProfile.OWNER_EMAIL, ignoreCase = true)
            UserProfile(
                id = userId,
                userId = userId,
                email = email,
                phoneNumber = if (isOwnerEmail) UserProfile.OWNER_MOBILE else "",
                displayName = displayName,
                role = if (isOwnerEmail) "admin" else "student",
                membershipActive = isOwnerEmail
            )
        }
    }

    private suspend fun syncFromMobileRegistration(userId: String, profile: UserProfile): UserProfile {
        val mobileRef = db.collection("mobile_registrations").document(profile.phoneNumber)
        return try {
            val snap = mobileRef.get().await()
            if (!snap.exists()) return profile
            val mobileReg = snap.toObject(MobileRegistration::class.java) ?: return profile

            val mergedBookmarks = (profile.bookmarkedSet + mobileReg.bookmarkedLinkIds.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                .joinToString(",")

            val isOwnerNumber = profile.phoneNumber == UserProfile.OWNER_MOBILE
            val resolvedRole = if (isOwnerNumber || profile.isAdmin) "admin" else profile.role
            val resolvedActive = isOwnerNumber || profile.membershipActive || mobileReg.membershipActive

            val shouldUpdateUser = resolvedActive != profile.membershipActive ||
                resolvedRole != profile.role ||
                mobileReg.membershipExpiresAt != profile.membershipExpiresAt ||
                mergedBookmarks != profile.bookmarkedLinkIds

            if (shouldUpdateUser) {
                val userRef = db.collection("users").document(userId)
                userRef.update(
                    mapOf(
                        "role" to resolvedRole,
                        "membershipActive" to resolvedActive,
                        "membershipExpiresAt" to (mobileReg.membershipExpiresAt ?: profile.membershipExpiresAt),
                        "planType" to if (mobileReg.membershipActive) mobileReg.planType else profile.planType,
                        "bookmarkedLinkIds" to mergedBookmarks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            profile.copy(
                role = resolvedRole,
                membershipActive = resolvedActive,
                membershipExpiresAt = mobileReg.membershipExpiresAt ?: profile.membershipExpiresAt,
                planType = if (mobileReg.membershipActive) mobileReg.planType else profile.planType,
                bookmarkedLinkIds = mergedBookmarks
            )
        } catch (e: Exception) {
            profile
        }
    }

    suspend fun registerOrLoginWithMobile(
        userId: String,
        email: String,
        phoneNumber: String,
        pinCode: String,
        studentName: String,
        isLoginMode: Boolean
    ): Result<UserProfile> {
        val cleanPhone = phoneNumber.filter { it.isDigit() }.takeLast(10)
        val cleanPin = pinCode.filter { it.isDigit() }
        if (cleanPhone.length != 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }
        if (cleanPin.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter a 4-digit security PIN."))
        }

        val mobileRef = db.collection("mobile_registrations").document(cleanPhone)
        val userRef = db.collection("users").document(userId)

        return try {
            val mobileSnap = mobileRef.get().await()
            val userSnap = userRef.get().await()
            val currentUserProfile = if (userSnap.exists()) userSnap.toObject(UserProfile::class.java) else null

            // Check if this is the Owner's number (6261371961) or Owner's email
            val isOwnerAccount = cleanPhone == UserProfile.OWNER_MOBILE ||
                email.equals(UserProfile.OWNER_EMAIL, ignoreCase = true)
            val role = if (isOwnerAccount) "admin" else "student"

            if (mobileSnap.exists()) {
                val existingMobile = mobileSnap.toObject(MobileRegistration::class.java)
                    ?: return Result.failure(IllegalStateException("Could not read mobile registration."))

                if (existingMobile.pinCode != cleanPin && !isOwnerAccount) {
                    return Result.failure(IllegalArgumentException("Incorrect 4-digit PIN for mobile number $cleanPhone."))
                }

                val resolvedName = existingMobile.studentName.ifBlank {
                    studentName.ifBlank { currentUserProfile?.displayName ?: if (isOwnerAccount) "Owner Admin" else "Student" }
                }
                val mergedMembershipActive = isOwnerAccount || existingMobile.membershipActive || (currentUserProfile?.membershipActive == true)
                val mergedExpiry = existingMobile.membershipExpiresAt ?: currentUserProfile?.membershipExpiresAt
                val mergedPlan = if (isOwnerAccount) "admin_pass" else if (existingMobile.planType != "free") existingMobile.planType else (currentUserProfile?.planType ?: "free")
                val existingBookmarks = existingMobile.bookmarkedLinkIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                val userBookmarks = currentUserProfile?.bookmarkedSet ?: emptySet()
                val mergedBookmarks = (existingBookmarks + userBookmarks).joinToString(",")

                //First update user profile so if this is Owner 6261371961, their user doc has phoneNumber == '6261371961' & role == 'admin'
                val userPayload = mapOf(
                    "userId" to userId,
                    "email" to email,
                    "phoneNumber" to cleanPhone,
                    "pinCode" to cleanPin,
                    "displayName" to resolvedName,
                    "role" to role,
                    "membershipActive" to mergedMembershipActive,
                    "membershipExpiresAt" to mergedExpiry,
                    "planType" to mergedPlan,
                    "bookmarkedLinkIds" to mergedBookmarks,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(userPayload, SetOptions.merge()).await()

                mobileRef.update(
                    mapOf(
                        "ownerUid" to userId,
                        "ownerEmail" to email,
                        "studentName" to resolvedName,
                        "pinCode" to cleanPin,
                        "membershipActive" to mergedMembershipActive,
                        "membershipExpiresAt" to mergedExpiry,
                        "planType" to mergedPlan,
                        "bookmarkedLinkIds" to mergedBookmarks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                Result.success(
                    UserProfile(
                        id = userId,
                        userId = userId,
                        email = email,
                        phoneNumber = cleanPhone,
                        pinCode = cleanPin,
                        displayName = resolvedName,
                        role = role,
                        membershipActive = mergedMembershipActive,
                        membershipExpiresAt = mergedExpiry,
                        planType = mergedPlan,
                        bookmarkedLinkIds = mergedBookmarks
                    )
                )
            } else {
                val resolvedName = studentName.trim().ifBlank {
                    currentUserProfile?.displayName?.ifBlank { if (isOwnerAccount) "Owner Admin" else "Student" }
                        ?: if (isOwnerAccount) "Owner Admin" else "Student"
                }
                val membershipActive = isOwnerAccount || (currentUserProfile?.membershipActive == true)
                val expiry = if (isOwnerAccount) Timestamp(Date(System.currentTimeMillis() + 1000L * 86400000L)) else currentUserProfile?.membershipExpiresAt
                val plan = if (isOwnerAccount) "admin_pass" else (currentUserProfile?.planType ?: "free")
                val bookmarks = currentUserProfile?.bookmarkedLinkIds ?: ""

                // Update user profile first so isOwnerUserDoc() in rules recognizes phoneNumber == '6261371961'
                val userPayload = mapOf(
                    "userId" to userId,
                    "email" to email,
                    "phoneNumber" to cleanPhone,
                    "pinCode" to cleanPin,
                    "displayName" to resolvedName,
                    "role" to role,
                    "membershipActive" to membershipActive,
                    "membershipExpiresAt" to expiry,
                    "planType" to plan,
                    "bookmarkedLinkIds" to bookmarks,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(userPayload, SetOptions.merge()).await()

                val mobilePayload = mapOf(
                    "phoneNumber" to cleanPhone,
                    "pinCode" to cleanPin,
                    "studentName" to resolvedName,
                    "ownerUid" to userId,
                    "ownerEmail" to email,
                    "membershipActive" to membershipActive,
                    "membershipExpiresAt" to expiry,
                    "planType" to plan,
                    "bookmarkedLinkIds" to bookmarks,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                mobileRef.set(mobilePayload).await()

                Result.success(
                    UserProfile(
                        id = userId,
                        userId = userId,
                        email = email,
                        phoneNumber = cleanPhone,
                        pinCode = cleanPin,
                        displayName = resolvedName,
                        role = role,
                        membershipActive = membershipActive,
                        membershipExpiresAt = expiry,
                        planType = plan,
                        bookmarkedLinkIds = bookmarks
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, mobileRef.path)
            Result.failure(e)
        }
    }

    suspend fun activateMembership(
        userId: String,
        phoneNumber: String,
        plan: MembershipPlan,
        transactionRef: String
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        val expiryMillis = System.currentTimeMillis() + (plan.durationDays.toLong() * 24 * 60 * 60 * 1000L)
        val expiryTimestamp = Timestamp(Date(expiryMillis))

        val payload = mapOf(
            "membershipActive" to true,
            "membershipExpiresAt" to expiryTimestamp,
            "planType" to plan.id,
            "lastTransactionRef" to transactionRef,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(payload).await()
            if (phoneNumber.length == 10) {
                val mobileRef = db.collection("mobile_registrations").document(phoneNumber)
                val mobileSnap = mobileRef.get().await()
                if (mobileSnap.exists()) {
                    mobileRef.update(
                        mapOf(
                            "membershipActive" to true,
                            "membershipExpiresAt" to expiryTimestamp,
                            "planType" to plan.id,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleBookmarkLink(
        userId: String,
        phoneNumber: String,
        linkId: String,
        currentBookmarks: Set<String>
    ): Result<Set<String>> {
        val updatedSet = if (currentBookmarks.contains(linkId)) {
            currentBookmarks - linkId
        } else {
            currentBookmarks + linkId
        }
        val serialized = updatedSet.joinToString(",")
        val userRef = db.collection("users").document(userId)

        return try {
            userRef.update(
                mapOf(
                    "bookmarkedLinkIds" to serialized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (phoneNumber.length == 10) {
                val mobileRef = db.collection("mobile_registrations").document(phoneNumber)
                val snap = mobileRef.get().await()
                if (snap.exists()) {
                    mobileRef.update(
                        mapOf(
                            "bookmarkedLinkIds" to serialized,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(updatedSet)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }
}

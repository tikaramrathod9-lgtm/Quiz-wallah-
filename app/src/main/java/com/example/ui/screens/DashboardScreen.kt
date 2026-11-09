package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.model.AppConfig
import com.example.data.model.StudyLink
import com.example.data.model.UserProfile
import com.example.data.repository.MembershipRepository
import com.example.data.repository.StudyRepository
import com.example.ui.components.AddEditLinkDialog
import com.example.ui.components.MembershipPaywallDialog
import com.example.ui.components.MobileAccountDialog
import com.example.ui.components.OfflineReaderDialog
import com.example.ui.components.OwnerControlPanelDialog
import com.example.ui.components.StudyLinkCard
import com.example.ui.components.SubjectTabBar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IndigoPrimary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUserId: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val studyRepo = remember { StudyRepository(context) }
    val membershipRepo = remember { MembershipRepository(context) }
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedSubject by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // User Profile state
    val userProfile by membershipRepo.observeUserProfile(currentUserId)
        .collectAsState(initial = null)

    // Dynamic App Config state (customizable by Owner 6261371961)
    val appConfig by membershipRepo.observeAppConfig()
        .collectAsState(initial = AppConfig())

    val isAdmin = userProfile?.isAdmin ?: false
    val hasAccess = isAdmin || (userProfile?.isMembershipValid ?: false)

    // Registered Students list for Owner Control Panel
    val registeredStudents by remember(isAdmin) {
        if (isAdmin) {
            membershipRepo.observeAllMobileRegistrations()
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Dialog states
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<StudyLink?>(null) }
    var viewingSnapshotLink by remember { mutableStateOf<StudyLink?>(null) }
    var showMembershipDialog by remember { mutableStateOf(false) }
    var showMobileAccountDialog by remember { mutableStateOf(false) }
    var showOwnerControlPanel by remember { mutableStateOf(false) }
    var hasPromptedMobileRegistration by remember { mutableStateOf(false) }

    // Ensure profile exists on load
    LaunchedEffect(currentUserId) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            val profile = membershipRepo.ensureUserProfile(
                userId = currentUserId,
                email = user.email ?: "",
                displayName = user.displayName ?: "Student"
            )
            if (!profile.isAdmin && !profile.hasMobileRegistered && !hasPromptedMobileRegistration) {
                hasPromptedMobileRegistration = true
                showMobileAccountDialog = true
            }
        }
    }

    // Study Links state (fetch all when "All" or "Saved", otherwise filter by subject)
    val studyLinks by studyRepo.observeStudyLinks(
        subjectFilter = if (selectedSubject == "All" || selectedSubject == "Saved") null else selectedSubject
    ).collectAsState(initial = emptyList())

    val bookmarkedSet = userProfile?.bookmarkedSet ?: emptySet()

    // Filter links by "Saved" tab and search query
    val filteredLinks = remember(studyLinks, searchQuery, selectedSubject, bookmarkedSet) {
        val subjectFiltered = if (selectedSubject == "Saved") {
            studyLinks.filter { bookmarkedSet.contains(it.id) }
        } else {
            studyLinks
        }
        if (searchQuery.isBlank()) {
            subjectFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            subjectFiltered.filter {
                it.title.lowercase().contains(q) ||
                    it.subject.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.url.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Edustudy pro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isAdmin) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IndigoPrimary)
                                        .clickable { showOwnerControlPanel = true }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OWNER • 6261371961",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (hasAccess) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AmberGold)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO • ${userProfile?.daysRemaining ?: 0}d left",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .clickable { showMembershipDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Get Pass",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Subtitle: Mobile Number & Name
                        val phoneText = if (userProfile?.hasMobileRegistered == true) {
                            "+91 ${userProfile?.phoneNumber} • ${userProfile?.displayName ?: ""}"
                        } else {
                            "Tap phone icon to link Mobile Number"
                        }
                        Text(
                            text = phoneText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { showMobileAccountDialog = true }
                        )
                    }
                },
                actions = {
                    // Mobile Account Login / Switch Button
                    IconButton(
                        onClick = { showMobileAccountDialog = true },
                        modifier = Modifier.testTag("mobile_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Mobile Number Login / Account",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = { showOwnerControlPanel = true },
                            modifier = Modifier.testTag("owner_control_panel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Owner Control Center",
                                tint = IndigoPrimary
                            )
                        }
                    } else if (!hasAccess) {
                        IconButton(
                            onClick = { showMembershipDialog = true },
                            modifier = Modifier.testTag("unlock_membership_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Upgrade Membership",
                                tint = AmberGold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                withContext(NonCancellable) {
                                    try {
                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                    } catch (ignored: Exception) {
                                    } finally {
                                        Firebase.auth.signOut()
                                        onSignOut()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingLink = null
                        showAddLinkDialog = true
                    },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Study Link") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_link_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Custom Global Announcement Banner from Owner 6261371961
            if (appConfig.announcementBanner.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appConfig.announcementBanner,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Owner / Admin Control Banner
            if (isAdmin) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndigoPrimary.copy(alpha = 0.09f))
                        .clickable { showOwnerControlPanel = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Owner Mode (6261371961): Customize prices, announcements, UPI & manage student memberships.",
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showOwnerControlPanel = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Customize", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (!hasAccess) {
                // Free / Non-member Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberGold.copy(alpha = 0.14f))
                        .clickable { showMembershipDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Study links locked. Get Pass (starting ₹${appConfig.monthlyPriceInr}) linked to your mobile number.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "UPGRADE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }
            }

            // Subject Filter Tabs (All, Math, Physics, Chemistry, Hindi, English, Saved)
            SubjectTabBar(
                selectedSubject = selectedSubject,
                onSubjectSelected = { selectedSubject = it }
            )

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title, subject or formula...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_links_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Links List
            if (filteredLinks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "No study links match \"$searchQuery\""
                                selectedSubject == "Saved" -> "No saved links on your mobile account yet"
                                else -> "No links in $selectedSubject yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                selectedSubject == "Saved" -> "Tap the bookmark icon on any study link to save it to your mobile number."
                                isAdmin -> "Tap the '+ Add Study Link' button below to add study links to this subject."
                                else -> "Owner will add verified study resources for this subject soon."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredLinks,
                        key = { it.id }
                    ) { link ->
                        StudyLinkCard(
                            link = link,
                            hasAccess = hasAccess,
                            isAdmin = isAdmin,
                            isBookmarked = bookmarkedSet.contains(link.id),
                            onToggleBookmark = {
                                scope.launch {
                                    val result = membershipRepo.toggleBookmarkLink(
                                        userId = currentUserId,
                                        phoneNumber = userProfile?.phoneNumber ?: "",
                                        linkId = link.id,
                                        currentBookmarks = bookmarkedSet
                                    )
                                    result.onSuccess { updated ->
                                        val msg = if (updated.contains(link.id)) {
                                            "Saved to your mobile account"
                                        } else {
                                            "Removed from saved links"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onOpenSnapshot = {
                                viewingSnapshotLink = link
                            },
                            onMembershipRequired = {
                                showMembershipDialog = true
                            },
                            onEditLink = {
                                editingLink = link
                                showAddLinkDialog = true
                            },
                            onDeleteLink = {
                                scope.launch {
                                    studyRepo.deleteStudyLink(link.id)
                                    Toast.makeText(context, "Link deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add or Edit Link Dialog
    if (showAddLinkDialog) {
        AddEditLinkDialog(
            initialLink = editingLink,
            onDismiss = {
                showAddLinkDialog = false
                editingLink = null
            },
            onSaveLink = { title, url, subject, description, snapshotContent ->
                scope.launch {
                    val currentEdit = editingLink
                    if (currentEdit != null) {
                        val updated = currentEdit.copy(
                            title = title,
                            url = url,
                            subject = subject,
                            description = description,
                            snapshotContent = snapshotContent
                        )
                        studyRepo.updateStudyLink(updated)
                        Toast.makeText(context, "Study link updated", Toast.LENGTH_SHORT).show()
                    } else {
                        studyRepo.addStudyLink(
                            title = title,
                            url = url,
                            subject = subject,
                            description = description,
                            snapshotContent = snapshotContent
                        )
                        Toast.makeText(context, "New study link added", Toast.LENGTH_SHORT).show()
                    }
                    showAddLinkDialog = false
                    editingLink = null
                }
            }
        )
    }

    // Offline Reader Dialog
    viewingSnapshotLink?.let { link ->
        OfflineReaderDialog(
            link = link,
            onDismiss = { viewingSnapshotLink = null }
        )
    }

    // Mobile Number Registration / Login Dialog
    if (showMobileAccountDialog) {
        MobileAccountDialog(
            userProfile = userProfile,
            isMandatoryRegistration = false,
            onDismiss = { showMobileAccountDialog = false },
            onSubmitMobileAuth = { phone, pin, name, isLogin, onResult ->
                scope.launch {
                    val email = Firebase.auth.currentUser?.email ?: userProfile?.email ?: ""
                    val res = membershipRepo.registerOrLoginWithMobile(
                        userId = currentUserId,
                        email = email,
                        phoneNumber = phone,
                        pinCode = pin,
                        studentName = name,
                        isLoginMode = isLogin
                    )
                    res.onSuccess { updatedProfile ->
                        val msg = if (updatedProfile.isAdmin) {
                            "Owner Mobile (6261371961) Verified! Full Admin Access Unlocked."
                        } else {
                            "Mobile +91 ${updatedProfile.phoneNumber} linked! Membership & saved data synced."
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    onResult(res)
                }
            }
        )
    }

    // Owner Control Panel Dialog (for Owner 6261371961)
    if (showOwnerControlPanel && isAdmin) {
        OwnerControlPanelDialog(
            appConfig = appConfig,
            registeredStudents = registeredStudents,
            onDismiss = { showOwnerControlPanel = false },
            onSaveConfig = { updatedConfig ->
                scope.launch {
                    val result = membershipRepo.saveAppConfig(updatedConfig)
                    if (result.isSuccess) {
                        Toast.makeText(context, "App Settings & Pricing Updated!", Toast.LENGTH_SHORT).show()
                        showOwnerControlPanel = false
                    } else {
                        Toast.makeText(context, "Failed to update settings", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUpdateStudentMembership = { phoneNumber, ownerUid, activate, durationDays ->
                scope.launch {
                    val result = membershipRepo.ownerUpdateStudentMembership(phoneNumber, ownerUid, activate, durationDays)
                    if (result.isSuccess) {
                        val statusMsg = if (activate) "Activated +$durationDays days for $phoneNumber" else "Locked membership for $phoneNumber"
                        Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Membership Paywall Dialog
    if (showMembershipDialog) {
        MembershipPaywallDialog(
            currentUserId = currentUserId,
            studentPhoneNumber = userProfile?.phoneNumber ?: "",
            appConfig = appConfig,
            onDismiss = { showMembershipDialog = false },
            onActivateMembership = { plan, txnRef ->
                scope.launch {
                    val result = membershipRepo.activateMembership(
                        userId = currentUserId,
                        phoneNumber = userProfile?.phoneNumber ?: "",
                        plan = plan,
                        transactionRef = txnRef
                    )
                    if (result.isSuccess) {
                        Toast.makeText(context, "Membership Activated & Saved to your Mobile Account!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Activation error: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

# VidyaVault: Study Links & Membership Portal

A premium Android study hub designed for seamless categorization of crucial educational links across Math, Physics, Chemistry, Hindi, and English. Features secure Google Sign-In, cross-device cloud synchronization, offline link snapshot caching, an admin content dashboard, and an integrated Razorpay & UPI membership paywall with phone-number privacy protection.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions have been aligned based on your responses:
> - **Payment Method**: Razorpay gateway integrated with UPI (PhonePe, Google Pay, Paytm) and card payments. Payments routed through Razorpay to UPI ID `6261371961@ybl` with the personal mobile number masked and hidden from students.
> - **Access Control**: Super Admin (`tikaramrathod9@gmail.com`) can upload, categorize, and manage links across all subjects. Students sign in with Google and require an active membership to unlock study links.
> - **Offline Link Access**: Instant local caching and saved page previews/snapshots so students can access study materials offline without internet.

---

## 1. Overview & Core Concept

**VidyaVault** solves the common problem of scattered study resources by offering a unified, high-speed repository for critical exam links and educational materials. 

- **Primary Persona**: 
  - **Admin / Educator**: Adds curated study links, assigns subject tags (Math, Physics, Chemistry, Hindi, English), and manages active subscribers.
  - **Student**: Logs in via Google, previews course resources, subscribes to membership via UPI/Razorpay, and accesses synced, categorized links with offline caching.
- **Key Value**: 
  - Multi-device real-time sync via Firebase Firestore.
  - Automated membership duration tracking with instant access lock when expired.
  - Complete phone number masking on UPI transactions to protect privacy.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication Flow**:
   - Modern Welcome screen with Google Sign-In button (`CredentialManager`).
   - Auto-detection: If logged in as admin (`tikaramrathod9@gmail.com`), admin capabilities (Add/Edit/Delete Links, Member Manager) unlock immediately.
   - If logged in as a student, system checks Firestore for active membership status.

2. **Home Study Dashboard**:
   - Dynamic top bar with search, subject filter chips, and membership status badge (Active / Expired / Get Pass).
   - **Subject Tabs**: `Math`, `Physics`, `Chemistry`, `Hindi`, `English` (plus `All` filter).
   - Study link cards displaying title, domain favicon/badge, description, subject badge, "Offline Cached" indicator, and quick actions (Open In-App, Copy Link, Share).

3. **Membership Paywall Flow**:
   - For non-members or expired members, clicking locked links or tapping "Upgrade" triggers the Membership Sheet.
   - Plan tiers: Monthly Pass (e.g. ₹99), Quarterly (₹249), Annual/Exam Pass (₹699).
   - "Pay via Razorpay / UPI" button initiates checkout.
   - UPI payments credit `6261371961@ybl` via Razorpay payment links / merchant gateway without exposing raw contact digits.
   - Upon successful payment, Firestore updates `membershipUntil`, and study links unlock immediately.

4. **Admin Link Management Flow**:
   - Floating Action Button `+ Add Study Link` (visible only to Admin).
   - Modal bottom sheet with Subject picker, Title, URL, Note/Description, and optional Offline Snapshot text/summary.
   - Real-time updates propagate across all student devices instantly.

5. **Offline Reader & Cache**:
   - Built-in In-App Web Viewer with local snapshot cache.
   - Students can mark links as "Saved Offline" for revision during internet outages.

### Visual Identity & Theme
- **Theme**: *Academic Precision* — High-trust, distraction-free, modern learning palette.
- **Colors**:
  - Primary: Deep Indigo (`#1A237E`) & Electric Royal Blue (`#2979FF`).
  - Secondary / Accent: Academic Amber (`#FFAB00`) for premium membership badges.
  - Background: Crisp off-white (`#F8F9FE`) in light mode; Deep Slate Navy (`#0F172A`) in dark mode.
- **Typography**: Clean modern sans typography with strict accessibility scaling (`sp`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Firebase Firestore & Google Sign-In as Cloud Backbone**
  - *Chosen Approach*: Official Firebase Firestore paired with Jetpack Credential Manager Google Sign-In.
  - *Why*: Provides zero-latency real-time sync across multiple devices, robust security rules for admin link authoring and subscriber-only read authorization.
  - *Alternative Considered*: Local-only SQLite (Room) would fail the multi-device sync and shared admin-to-student distribution requirements.

- **Decision 2: Dual Persistence (Cloud Firestore + Local Room/Cache)**
  - *Chosen Approach*: Firestore acts as the source of truth; local SQLite/Room caches link lists and snapshots for instant offline access.
  - *Why*: Delivers instant cold-start loading and 100% offline usability when students study without active internet.

- **Decision 3: Privacy-Preserving Razorpay & UPI Gateway**
  - *Chosen Approach*: Razorpay Checkout with UPI intent & VPA gateway routing.
  - *Why*: Masks personal phone number (`6261371961@ybl` is mapped to merchant business name "Vidya Study Hub" in Razorpay), automatically verifies payments via webhook/callback, and prevents revealing raw personal digits on the user interface.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────────────┐ │
│  │   Auth Screen    │  │  Study Dashboard │  │   Membership Modal    │ │
│  │ (Google Sign-In) │  │  (Subject Tabs)  │  │   (Razorpay / UPI)    │ │
│  └─────────┬────────┘  └────────┬─────────┘  └───────────┬───────────┘ │
└────────────┼────────────────────┼────────────────────────┼─────────────┘
             │                    │                        │
             ▼                    ▼                        ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        ViewModels & StateFlow                          │
│     AuthViewModel     │     StudyViewModel     │    MembershipViewModel│
└────────────┬────────────────────┬────────────────────────┬─────────────┘
             │                    │                        │
             ▼                    ▼                        ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           Repository Layer                             │
│       StudyRepository        │       MembershipRepository              │
└────────────┬─────────────────┴───────────────────────────┬─────────────┘
             │                                             │
             ├──────────────────────┐                      │
             ▼                      ▼                      ▼
┌─────────────────────────┐  ┌──────────────┐   ┌────────────────────────┐
│  Firebase Cloud Engine  │  │ Local Cache  │   │  Razorpay / UPI Engine │
│  • Firebase Auth        │  │ • Room DB    │   │  • Razorpay Checkout   │
│  • Cloud Firestore      │  │ • Snapshots  │   │  • Phone Masking       │
│  • Security Rules       │  │ • Offline UI │   │  • Verification Flow   │
└─────────────────────────┘  └──────────────┘   └────────────────────────┘
```

### Data Models

1. **`StudyLink`**:
   - `id: String` (Document ID)
   - `title: String`
   - `url: String`
   - `subject: String` (`"Math"`, `"Physics"`, `"Chemistry"`, `"Hindi"`, `"English"`)
   - `description: String`
   - `snapshotContent: String` (Offline cached summary or article text)
   - `authorEmail: String`
   - `createdAt: Timestamp`
   - `updatedAt: Timestamp`

2. **`UserProfile` & `Membership`**:
   - `uid: String`
   - `email: String`
   - `displayName: String`
   - `role: String` (`"admin"` for `tikaramrathod9@gmail.com`, `"student"` for others)
   - `membershipActive: Boolean`
   - `membershipExpiresAt: Timestamp?`
   - `paymentHistory: List<PaymentRecord>`

---

## 5. Phased Implementation Plan

1. **Setup & Backend Provisioning**:
   - Provision Firebase Firestore & Authentication using `set_up_firebase`.
   - Update `metadata.json` ("VidyaVault") and `strings.xml`.
   - Author `firebase-blueprint.json` and hardened `firestore.rules`.
   - Setup dependencies in `build.gradle.kts` (Firestore, Auth, Credential Manager, Room).

2. **Security Rules & Testing**:
   - Enforce read access for active subscribers or admin.
   - Enforce write/delete access strictly for admin (`tikaramrathod9@gmail.com`).
   - Run JavaScript rules unit tests & Robolectric repository tests.
   - Deploy rules via `deploy_firebase`.

3. **Data Layer & Repositories**:
   - Create `StudyLinkRepository` with real-time Firestore listeners and Room caching fallback.
   - Implement `MembershipRepository` for subscription status check, purchase handling, and expiration logic.

4. **UI & Navigation Implementation**:
   - Google Sign-In Gate with Credential Manager.
   - Subject Tabs: Math, Physics, Chemistry, Hindi, English, All.
   - Link Cards with copy, share, open, and offline snapshot dialog.
   - Admin "+ Add Link" bottom sheet with subject dropdown, link title, URL validation.
   - Membership Paywall Sheet with plan tiers, Razorpay integration, and UPI privacy masking.
   - In-app cached study reader.

5. **Verification & Polish**:
   - Compile and verify applet with `compile_applet`.
   - Validate edge-to-edge UI, dark/light theme, and empty states.

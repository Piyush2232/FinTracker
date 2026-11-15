# FinTracker — Minimal On-Device Indian Expense & Budget Tracker

FinTracker is a privacy-first Android personal finance application built with **Kotlin** and **Jetpack Compose** following the **Breeze Minimal Finance** design language.

All transaction capture, SMS/UPI notification regex parsing, Indian-diaspora machine learning classification, and database storage execute **100% locally on-device** with zero cloud servers or external telemetry.

---

## Key Features

### 1. Ultra-Minimal Concentric Spending UI & 3-Level Drill-Down
- **Level 1 — Concentric Spending Circle (`HomeScreen`)**:
  - Clean white canvas with only the **Streak** and **Profile** actions in the top-right header and zero footer clutter.
  - Spring-animated concentric rings displaying overall monthly spend vs. monthly goal (outer ring) and top category allocations (inner rings) formatted in Indian Rupees (`₹1,24,500`).
  - Subtle `+` action inside the center ring hub for quick paste, manual log, or CSV import.
  - **Scroll-Down "Recent Spending"**: Scrolling down smoothly scales and translates the concentric circle with a parallax transition to reveal the animated **Recent Spending** feed.
- **Level 2 — Categorical Spending & Visual Reports (`CategoryBreakdownScreen`)**:
  - Tapping the concentric circle transitions into per-category progress rings, remaining budget pace, a **14-Day Daily Spending Pace Chart**, and a **Category Distribution Report**.
- **Level 3 — Curated Category Feed (`CategoryDetailScreen`)**:
  - Tapping any category (e.g., *Food & Dining*) opens a full-screen curated transaction feed with top merchant chips, source filters (`SMS`, `SMS Live`, `Alert`, `CSV`, `Parsed`, `Manual`), and one-tap category reassignment.

### 2. Automatic Expense Capture (`ContentObserver` + Regex + UPI Notifications)
- **Background `ContentObserver` Service (`SmsContentObserverService`)**:
  - Monitors `Telephony.Sms.CONTENT_URI` (`content://sms`) in real time and scans `content://sms/inbox` locally on-device.
- **Indian Financial Regex Parser (`IndianSmsRegexParser`)**:
  - Extracts debit verbs (`Debited`, `Spent`, `Paid`, `Amt Sent`, `Withdrawn`, `Trf to`), `₹`/`Rs.`/`INR` amounts, account/card tails (`A/c *1234`, `Card XX9012`), UPI reference IDs, and payee/merchant names across 7 named bank/UPI regex rules (`UPI_TRF_TO`, `ICICI_SEMI_CREDITED`, `HDFC_SENT_TO`, `PAYEE_TO_CLAUSE`, `CARD_SPENT_AT`, `UPI_SLASH_HANDLE`, `NPCI_VPA_HANDLE`).
  - Automatically rejects OTPs, promotional/loan spam, failed/declined alerts, incoming credits, and UPI Lite wallet top-ups (to prevent double-counting).
- **Companion UPI Push Notification Listener (`PaymentNotificationListenerService`)**:
  - Captures small-value UPI and **UPI Lite** payments from Google Pay, PhonePe, Paytm, BHIM, CRED, Amazon Pay, WhatsApp Pay, Super.money, Navi, Jupiter, Fi, and major Indian banking apps when banks skip sending an SMS.
  - Shares a 3-minute `SHA-256` deduplication hash (`amount + merchant + 3minBucket`) with the SMS parser so payments that trigger both an SMS and a push notification are logged only once.

### 3. 100% Offline Indian & Diaspora ML Classifier (`DiasporaMlClassifier`)
- Pure-Kotlin **Multinomial Naive Bayes + character trigram similarity** classifier pre-trained across 8 categories:
  1. **Food & Dining** (*Swiggy, Zomato, Haldiram's, Saravana Bhavan, Chai Point, Chaayos, Blue Tokai, Dishoom...*)
  2. **Groceries & Kirana** (*Blinkit, Zepto, Instamart, BigBasket, DMart, Patel Brothers, Apna Bazaar, Amul, Nandini...*)
  3. **Transport & Travel** (*Uber, Ola, Rapido, Namma Yatri, IRCTC, IndiGo, DMRC/BMRC Metro, FASTag, IndianOil...*)
  4. **Bills & Utilities** (*Jio, Airtel, ACT Fibernet, BESCOM, Tata Power, MSEDCL, Indane, NoBroker, MyGate, CRED...*)
  5. **Shopping & Lifestyle** (*Amazon, Flipkart, Myntra, Ajio, Nykaa, Zudio, Westside, Decathlon, Fabindia, Lenskart...*)
  6. **Remittance & Transfers** (*Remitly, Wise, Xoom, Zerodha, Groww, Kuvera, NEFT/IMPS...*)
  7. **Health & Wellness** (*Apollo Pharmacy, PharmEasy, Tata 1mg, Cult.fit, Practo, Fortis, Manipal...*)
  8. **Entertainment & Media** (*BookMyShow, PVR INOX, Netflix, JioCinema, Hotstar, Spotify...*)
- **Online Learning**: Re-categorizing any transaction updates token weights in Room (`ml_token_weights`) so future transactions from that merchant classify accurately.

### 4. Interactive SMS Parser Test Lab & Emulator Batch Simulator
- Built-in **Indian Regex SMS Parser Lab** (`Profile` → `Indian Regex SMS Parser Lab`) to paste any custom SMS or select from 10 realistic Indian bank/UPI/OTP presets and inspect the live regex rule match, extracted `₹` amount, merchant, and ML confidence.
- **Emulator Live SMS Simulator**: One-tap **`Simulate +3 SMS`** on the Home screen (and **`+1 / +3 / +5 Bank SMS`** in Profile) to dispatch realistic Indian bank SMS alerts through `SmsContentObserverService` on an emulator without needing external ADB commands.

### 5. CSV & Multi-Page A4 PDF Export (`ExportManager`)
- Export monthly transaction ledgers to **CSV** (`text/csv`) or formatted **A4 PDF** (`application/pdf`) using Android's zero-permission Storage Access Framework (`ActivityResultContracts.CreateDocument`).

---

## Architecture & Project Structure

```text
app/src/main/java/com/example/
├── FinTrackerApp.kt                        # Application singleton wiring Room, DataStore & ML
├── MainActivity.kt                         # Edge-to-edge Activity + Navigation Compose host
├── capture/
│   ├── IndianSmsRegexParser.kt             # Regex engine for Indian bank/card/UPI patterns
│   ├── SmsTransactionParser.kt             # Parser facade + SHA-256 deduplication + presets
│   ├── SmsContentObserverService.kt        # Background Service with ContentObserver on content://sms
│   ├── SmsBroadcastReceiver.kt             # Live SMS_RECEIVED BroadcastReceiver
│   ├── SmsInboxScanner.kt                  # Historical current-month inbox ContentResolver scanner
│   └── PaymentNotificationListenerService.kt # UPI & banking app push notification listener
├── ml/
│   ├── DiasporaVocabulary.kt               # Pre-trained Indian & diaspora merchant/token priors
│   └── DiasporaMlClassifier.kt             # Offline Multinomial Naive Bayes + trigram classifier
├── data/
│   ├── Entity.kt                           # Room entities (transactions, budgets, ml_token_weights)
│   ├── Dao.kt                              # Reactive Flow DAOs
│   ├── AppDatabase.kt                      # Local SQLite Room database
│   ├── UserPreferencesRepository.kt        # DataStore preferences (monthly prompt & scan stats)
│   └── FinTrackerRepository.kt             # Unified repository mediating capture, ML, and Room
├── domain/
│   ├── ExpenseCategory.kt                  # 8 categories with Breeze ring colors & ratios
│   └── InrFormatter.kt                     # Indian numbering system formatter (₹1,24,500)
├── export/
│   └── ExportManager.kt                    # RFC-4180 CSV & native PdfDocument report writers
└── ui/
    ├── UiState.kt                          # Immutable UI state models
    ├── FinTrackerViewModel.kt              # Reactive StateFlow ViewModel
    ├── components/
    │   ├── ConcentricSpendingCircle.kt     # Spring-animated concentric Canvas ring visualizer
    │   └── VisualReportCharts.kt           # Daily pace bar chart & category distribution bar
    ├── screens/
    │   ├── HomeScreen.kt                   # Minimal concentric circle + scroll-down Recent Spending
    │   ├── CategoryBreakdownScreen.kt      # Level 2 categorical breakdown + visual reports
    │   ├── CategoryDetailScreen.kt         # Level 3 full-screen curated category feed + ML feedback
    │   └── SheetsAndHubs.kt                # Onboarding goal sheet, Streak sheet, Profile & Parser Lab
    └── theme/
        ├── Color.kt                        # Breeze Minimal Finance palette & semantic tokens
        ├── Type.kt                         # Plus Jakarta Sans typography with tabular numerals
        └── Theme.kt                        # Material 3 theme configuration
```

---

## Getting Started

### Prerequisites
- **Android Studio** Ladybug / Meerkat or newer (AGP `9.1.1`, Kotlin `2.2.10`)
- **JDK 11+**
- **Android SDK 36** (`minSdk = 24`, `targetSdk = 36`)

### Build & Run
1. Clone the repository:
   ```bash
   git clone <your-repo-url>
   cd FinTracker
   ```
2. Open the project in Android Studio or build from the terminal:
   ```bash
   ./gradlew :app:assembleDebug
   ```
3. Run unit and Robolectric UI tests:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

---

## Pushing / Exporting to GitHub from AI Studio

- **Direct Push to GitHub**: Click the **GitHub / Settings** icon in the top toolbar of Google AI Studio and select **Push to GitHub** to create or update your repository directly.
- **ZIP Export**: Alternatively, select **Download ZIP** from the settings menu and push to your remote:
  ```bash
  git init
  git add .
  git commit -m "Initial commit: FinTracker local-first Android expense tracker"
  git branch -M main
  git remote add origin https://github.com/<your-username>/FinTracker.git
  git push -u origin main
  ```

---

## Privacy & Permissions

- **`READ_SMS` & `RECEIVE_SMS`** *(Optional at runtime)*: Used exclusively on-device by `SmsContentObserverService`, `SmsInboxScanner`, and `SmsBroadcastReceiver` to detect Indian bank/UPI debit messages.
- **`BIND_NOTIFICATION_LISTENER_SERVICE`** *(Optional via system settings)*: Used on-device to capture UPI app payment notifications (including UPI Lite micro-spends).
- **Zero Network Permissions**: Notice that `AndroidManifest.xml` does **not** even request `<uses-permission android:name="android.permission.INTERNET" />`—making it technically impossible for your financial data or messages to leave your phone.

---

## License

Released under the [MIT License](LICENSE).

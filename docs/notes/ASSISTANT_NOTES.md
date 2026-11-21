# ASSISTANT_NOTES.md

## Milestone 0: Discovery & Architecture Alignment Report

### 1. Host Application Baseline
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 36 (Android 16 preview / 36)
- **Java / Kotlin Toolchain**: Kotlin 2.2.0, JVM Toolchain 17
- **UI Framework**: 100% Jetpack Compose with Material 3 (`androidx.compose.material3`). No XML Views used for screens.
- **Dependency Injection**: Manual constructor injection throughout the application. No Hilt, Koin, or Dagger.
- **Database Architecture**: Native Android SQLite via `AppDatabaseHelper : SQLiteOpenHelper` (`poultry_farm.db`, version 12). No Room or KSP configured in the project.
- **Theming**: `PoultryAppTheme` in `com.example.poultryapp.theme.Theme.kt`, supporting dynamic color, `DarkColorScheme`, and `LightColorScheme`. Governed by user preference `settings.themeMode` (`ThemeMode.DARK`, `ThemeMode.LIGHT`, `ThemeMode.SYSTEM`).
- **Localization**: Application is currently configured with Persian UI and string resources, with English support added for dual-language assistant operations.

### 2. Spec Deviations & Justifications
1. **Database Layer (Room -> Native SQLiteOpenHelper)**:
   - *Spec Requirement*: `AssistantDatabase (Room, its own tables)`.
   - *Deviation*: The host project uses `SQLiteOpenHelper` and does not use KSP or Room. Adding Room would introduce heavy compiler dependencies and dual connection pools.
   - *Resolution*: Implemented `AssistantDatabaseHelper` extending `SQLiteOpenHelper` with clean Kotlin DAO interfaces (`MessageDao`, `ActionLogDao`, `LearnedPhraseDao`, `AliasDao`). If Room is adopted globally later, the DAO interfaces remain identical.
2. **Jalali Calendar Calculation (Android ICU -> Pure Kotlin)**:
   - *Spec Requirement*: Implement `CalendarConverter` with `android.icu.util.PersianCalendar`.
   - *Deviation*: `android.icu.*` is an Android framework library that breaks JVM unit tests unless mocked.
   - *Resolution*: Used the pure Kotlin astronomical conversion algorithms from `JalaliDateHelper.kt` inside `assistant/core/entities/DateParser.kt`. This guarantees 100% JVM unit testability without Android runtime dependencies.
3. **Legacy Voice Code Cleanup**:
   - As instructed by the user, all previous procedural/regex assistant code (`VoiceIntentParser.kt`, `FarmConversationalAiEngine.kt`, `VoiceActionDispatcher.kt`, `FarmIntentSemanticRouter.kt`, etc.) is deleted.
   - The visual UI layout, animations, bottom sheet container, waveform visualizers, input dock, and history drawer from `VoiceAssistantSheet.kt` are preserved and refactored into the new architecture.

### 3. Golden Rule Compliance
All data-mutating actions (`AddEggsAction`, `AddTransactionAction`) write data exclusively through the existing repositories (`EggProductionRepository`, `FinancialRepository`, `FlockRepository`). No direct DAO or raw SQL writes bypass application business logic.

---

## Milestone 5: App Repair, Data Integrity & Security Hardening

### 1. Issues Resolved & Verified
1. **Feed/Water 1,000x Unit Mismatch**:
   - `AddFeedWaterAction`: Quantities from user input (in kg/liters) are normalized and saved as grams (feed) and milliliters (water) into `FeedWaterRepository`, matching `KTLiveConsumptionTracker` and database conventions.
   - `QueryFeedWaterAction`: Stored quantities (grams/ml) are formatted through `DailyNutritionEngine.formatFeed` and `DailyNutritionEngine.formatWater` and connected with `LiveConsumptionTracker.calculateLiveStatus` for live countdown and remaining depletion status.
   - `PackagingUnitExtractor` & `AssistantRouter`: Fractional amounts (e.g. `2.5 کیلو`) preserve decimal precision in both `COUNT` and `AMOUNT` without integer truncation.
2. **Warehouse Egg Stock Double Additions & Rollback**:
   - Removed duplicate `autoAddEggs` call in `MainViewModel.logEggProduction` and removed unconditional addition in `AppDatabaseHelper.insertEggProduction`.
   - Added `autoDeductProducedEggsFromWarehouse` in `AppDatabaseHelper` and wired it into `deleteEggProduction` to atomically roll back warehouse stock on egg deletion or assistant undo.
3. **Database Transaction Atomicity & Negative Inventory Guards**:
   - Wrapped `transferBirds`, `consumeWarehouseItem`, and `sellEggItem` in atomic database transactions (`db.beginTransaction()`).
   - Replaced silent clamping of insufficient inventory to `0.0` with strict inventory sufficiency checks (`if (currentStock < amount) return false`), preventing corrupted stock counts.
4. **Multi-Device Sync Cascading Deletion Prevention**:
   - In `AppDatabaseHelper.mergeDatabaseFromJson`, removed `CONFLICT_REPLACE` on the `flocks` table.
   - Flocks are merged by name lookup: updating non-conflicting fields on match or inserting without ID overwrite, avoiding SQLite row deletion that triggered cascading foreign-key deletions of linked history.
5. **Worker Mode Role Enforcement in Smart Assistant**:
   - `AssistantViewModel` receives `userRole` (`ADMIN` vs `WORKER`).
   - Mutations and queries regarding financial records (`ADD_TRANSACTION`, `QUERY_FINANCE`) and inventory consumption (`CONSUME_WAREHOUSE`) are strictly blocked in `WORKER` mode with an Arabic/Persian access-denied message prompting for manager PIN unlock.
6. **Dual-Flock Targeting & Generic Token Filtering**:
   - Added `resolveFlocksForTransfer` in `EntityResolver` to distinguish source and destination flocks from patterns like "از سالن ۱ به سالن ۲" / "from flock 1 to flock 2".
   - Filtered out generic words (`سالن`, `گله`, `فارم`) from single-token fuzzy flock matching.
   - Integrated warehouse item resolution in `AssistantRouter`.
7. **Jalali Date Standardization & Epoch Millis**:
   - Preserved requested dates (`LocalDate`) as standardized Jalali strings (`YYYY/MM/DD`) and epoch timestamps in `AddEggsAction`, `AddTransactionAction`, and `AddFeedWaterAction` instead of defaulting to today's date or using relative phrases as dates.
8. **Dialogue Management Hardening & Missing Actions**:
   - Validation warnings are handled interactively with preview confirmation cards rather than being bypassed.
   - Clear pending slots and confirmation state upon button confirmation and cancellation.
   - Added pending slots to `ClarifyOptions`.
   - Implemented `HelpAction` and `GreetingAction` for `HELP` and `GREETING` intents.
9. **Query Precision**:
   - Added period filtering (`SlotId.PERIOD` / `SlotId.DATE`) in `QueryFinanceAction`.
   - Filtered today's tasks by Shamsi weekday in `QueryScheduleAction`.
10. **Microphone Lifecycle & Speech Engine**:
    - Added runtime `RECORD_AUDIO` permission checks in `VoiceAssistantSheet`.
    - Added lifecycle `DisposableEffect` to stop listening when sheet is disposed or backgrounded.
    - Closed `callbackFlow` channels in `AndroidSpeechEngine` on `onError` and `onResults` to prevent leaks.
11. **Alarms, Schedules & Lockout Security**:
    - `AddScheduleAction` parses requested time/date and arms exact weekly alarms via `AlarmScheduler`.
    - `AlarmReceiver` re-arms recurring weekly schedules and morning reminder (9999).
    - Fixed schedule cancellation request code mismatch (`scheduleId * 10 + dayIndex`).
    - Upgraded PIN hashing to PBKDF2 with unique salt, lockout countdown backoff, and background auto-lock timeout.
    - Included assistant tables (`assistant_messages`, `action_logs`, `learned_phrases`, `entity_aliases`) in full app JSON backup and restore.
12. **Domain Calculation Edge Cases**:
    - `DailyNutritionEngine`: Flock nutrition returns `0.0` for flocks with zero live birds.
    - `InvestmentFeasibilityEngine`: Sets `breakEvenDay = null` when net profit <= 0 or revenue <= working capital, displaying `"حاصل نمی‌شود (زیان‌ده)"` in UI.
    - `DiagnoseDiseaseAction`: Prevented generic symptom fallback to Newcastle disease.

### 2. Test & Build Verification
- Unit Tests: 72/72 tests passed (100% pass rate).
- Full Gradle APK Build: `assembleDebug` completed successfully.


### 1. Capabilities Mapped & Implemented
All primary user operations across the application were systematically analyzed and brought to the Smart Assistant:
1. **Feed & Water Logistics**:
   - `AddFeedWaterAction`: Logs feed (kg/tons/sacks) and water refills (liters) per flock/barn via `FeedWaterRepository`.
   - `QueryFeedWaterAction`: Calculates daily consumption totals and predicts survival hours/days until depletion using flock nutrition models.
2. **Mortality & Bird Loss**:
   - `LogMortalityAction`: Records dead birds with clinical cause-of-death breakdown (`HEAT_COLD_STRESS`, `CANNIBALISM`, `PREDATOR_ACCIDENT`, `CULLING`, `LIVE_SALE`, `DISEASE`) via `MortalityRepository`.
   - `QueryMortalityAction`: Reports weekly/daily dead counts and percentage breakdown.
3. **Flock Management & Inter-Flock Bird Transfers**:
   - `QueryFlocksAction`: Total bird count, active barns, flock ages in weeks/days, and active medication withdrawal days via `FlockRepository`.
   - `TransferBirdsAction`: Safely transfers birds between flocks with quantity validation, rollback, and undo support.
4. **Warehouse & Inventory**:
   - `QueryWarehouseAction`: Current stock levels, low-stock warnings, and feed days-remaining projection via `WarehouseRepository`.
   - `ConsumeWarehouseAction`: Records stock consumption from warehouse bins directly to flocks.
5. **Clinical Health & Offline Veterinary Catalogs**:
   - `DiagnoseDiseaseAction`: Offline clinical symptom differential diagnosis matching symptoms to diseases (e.g., Newcastle, Coccidiosis, Gumboro, Fowl Cholera, Infectious Bronchitis) via `DiseaseCatalog`.
   - `QueryMedicationAction`: Offline dosage lookup, water/feed administration instructions, and meat/egg withdrawal period warnings via `MedicationCatalog`.
6. **Task Scheduling & Calendar Reminders**:
   - `AddScheduleAction`: Sets calendar tasks, vaccine reminders, and vitamin schedules via `ScheduleRepository`.
   - `QueryScheduleAction`: Lists today's agenda, upcoming vaccines, and overdue farm tasks.
7. **Farm Contacts & Directories**:
   - `QueryContactsAction`: Looks up suppliers, veterinarians, feed mills, and egg buyers with one-tap phone dialing via `FarmContactRepository`.
8. **Weather & Ventilation Guidance**:
   - `QueryWeatherAction`: Farm temperature monitoring, heat/cold stress alerts, and ventilation fan recommendations via `SettingsRepository`.
9. **In-App Direct Navigation**:
   - `OpenScreenAction`: Voice/text navigation across main tabs (`DASHBOARD`, `FLOCKS`, `MANAGEMENT`, `HEALTH`, `SETTINGS`) and sub-tabs (`WAREHOUSE`, `FINANCIAL`, `RATION_BUILDER`, `CONTACTS`, `SIMULATORS`, `DIAGNOSIS`, `DRUGS`, `SUPPLEMENTS`, `BIOSECURITY`, `VET_CONSULT`).

### 2. Architectural Compliance & Golden Rule Verification
- **Golden Rule**: 100% adherence — all write operations route strictly through existing application repositories (`FeedWaterRepository`, `MortalityRepository`, `FlockRepository`, `WarehouseRepository`, `ScheduleRepository`, etc.). Zero direct database bypass or raw DAO tampering.
- **Pure Kotlin JVM Core**: All NLU algorithms, slot extraction, packaging unit parsing (including `WATER_LITER`), fuzzy matching, and intent classification reside in `assistant/core/` with zero Android framework imports, ensuring 100% JVM unit testability.
- **Zero On-Device LLM**: Entire system operates deterministically with domain lexicons and phrase banks under 3 MB footprint.
- **Bilingual Support**: Persian and English inputs handled with normalization and language detection.

### 3. Verification & Regression Test Results
- `AllCapabilitiesRoutingTest`: 7/7 suites passed (100%).
- `GoldenEvaluationTest`: Persian and English golden corpora passed (100%).
- Project-wide unit tests: 72/72 tests passed (100% pass rate, 0 regressions).
- Android build: `assembleDebug` completed successfully with zero errors.


### 1. Pure Kotlin JVM Core (`assistant/core/`)
- `normalize/TextNormalizer.kt`: Complete NFKC normalization, Arabic-to-Persian letter harmonization, Persian and Arabic-Indic digit mapping, ZWNJ space/matchKey filtering, Damerau-Levenshtein fuzzy matching, and dual-language detection (`fa` vs `en`).
- `entities/NumberParser.kt`: Spelled-out numbers in Persian and English, compound numbers with digits and multipliers (`۱ میلیون و ۵۰۰ هزار`), suffixes (`2k`, `1.5m`), counters (`۱۲۰ تا`), and fractions (`نیم`).
- `entities/DateParser.kt`: Relative dates (`امروز`, `دیروز`, `yesterday`), periods (`این هفته`, `ماه گذشته`), Persian/English weekdays, Jalali month names (`۱۶ آبان`), and numeric Jalali (`۱۴۰۵/۰۸/۱۶`) with pure-Kotlin Gregorian conversion.
- `entities/PackagingUnitExtractor.kt`: Domain unit multipliers for trays (`شانه` = 30), cartons (`کارتن` = 180), and feed sacks (`کیسه` = 50 kg).
- `entities/MoneyParser.kt`: Tomans, Rials (divided by 10 to Tomans), USD, and EUR extraction with surrounding financial context detection.
- `entities/EntityResolver.kt`: Fuzzy flock matching against user database entities and financial category classification.
- `nlu/IntentScorer.kt`: Soft template matching, lexicon triggers, phrase-bank similarity, entity slot completion scoring, and safety guards (e.g. general question guard protecting against false command execution).
- `router/AssistantRouter.kt`: Unified routing returning `Execute`, `AskSlot`, `Clarify`, `AskOnline`, or `Unsupported`.
- `dialogue/DialogueManager.kt`: Multi-turn state management, slot-prompting workflow, cancellation, and safety confirmation checks for unusual quantities.

### 2. Actions & Data Layer (`assistant/actions/` & `assistant/data/`)
- Actions implemented with standard `AssistantAction` interface: `AddEggsAction`, `AddTransactionAction`, `QueryEggsAction`, `QueryFinanceAction`, `UndoAction`.
- SQLite assistant tables: `assistant_messages`, `action_logs`, `learned_phrases`, `entity_aliases` managed via `AssistantDatabaseHelper`.

### 3. UI Preservation & Integration
- Preserved `VoiceAssistantSheet.kt` UI layout, Material 3 animations, waveform visualizers, chat bubbles, suggestions dock, and history drawer.
- Cleanly decoupled from legacy monolithic engines and connected to `AssistantViewModel` powered by `LocalAssistantColors` mapping to the active Material 3 color scheme.

### 4. Golden Test Suites & Evaluation
- `assets/assistant/lexicon_en.json` & `lexicon_fa.json`
- `assets/assistant/phrases_en.json` & `phrases_fa.json`
- `assets/assistant/golden/fa.json` & `golden/en.json`
- Automated test suites:
  - `TextNormalizerTest`: 8/8 tests passing.
  - `NumberParserTest`: 3/3 tests passing.
  - `PackagingAndMoneyParserTest`: 2/2 tests passing.
  - `DateParserTest`: 4/4 tests passing.
  - `RouterAndDialogueManagerTest`: 6/6 tests passing.
  - `GoldenEvaluationTest`: 2/2 tests passing.
  - Full application regression suite: 66/66 tests passing (100%).
  - Full Gradle build: `assembleDebug` successful.



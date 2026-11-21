# Project: Persian Poultry Farm Voice & Conversational AI Assistant

## Architecture
- **Language & Runtime**: Kotlin 2.3.20, Android SDK 36 (target 35, min 26), Java 17 toolchain (JDK 25 compatibility runtime).
- **Architecture Model**: 100% Offline, on-device Zero-Network execution pipeline.
- **Subsystem Boundaries**:
  1. **Voice Normalization & ASR Sanitization**: `PersianVoiceNormalizer`, `PoultryPhoneticCorrector` (handles Persian/Arabic script unification, ZWNJ, numbers/fractions/Momayyez, ASR phonetic correction, dialect normalization).
  2. **Voice Intent Parsing & Slot-Filling**: `VoiceIntentParser` (handles operational action extraction, unit conversions, multi-turn slot merging, relative Jalali dates, compound actions, RBAC).
  3. **Semantic Intent Routing & Disambiguation**: `FarmIntentSemanticRouter`, `FarmSemanticWordComprehensionEngine` (routes across 34 semantic intents, resolves collisions, enforces priority hierarchy).
  4. **Conversational Engine & State Machine**: `FarmConversationalAiEngine` (manages multi-turn turns, session context `AiSessionState`, coreference resolution, polar vs open questions, early disambiguation).
  5. **Domain Intelligence Engines**:
     - Clinical & Pathology: `VeterinaryClinicalDiagnosticsEngine`, `DiseaseCatalog`, `AvianSpecialtyPathologyAndParasitologyEngine`.
     - Encyclopedic & Breeds: `PoultryScienceEncyclopedicKnowledgeEngine`, `PoultryExpertKnowledgeVault`, `AvianHousingAndHusbandryEngine`, `AvianPhotoperiodAndEquipmentEngine`.
     - Crisis Simulations: `PoultryScenarioSimulationEngine`, `PoultryVentilationClimateEngine`.
     - Farm Economics: `FarmEconomicProductionEngine`, `OptimalSlaughterEngine`, `InvestmentFeasibilityEngine`.
     - Analytics & Telemetry: `FarmOmniscientAnalyticsEngine`, `HistoricalRootCauseAnalysisEngine`.
     - Search & Information Retrieval: `OfflineSemanticRetriever` (BM25 search).
  6. **Automated Test Infrastructure**: `MasterAssistantOmniScenarioExhaustiveCoverageTest.kt` (JVM unit tests executing in-memory).

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Master Plan Documentation | Comprehensive strategy document detailing intent taxonomy, 19-stage disambiguation hierarchy, edge-case coverage, and testing roadmap | M1 | ORIGINAL_REQUEST §R1 |
| 2 | Ascites ("آسیت") vs Acidifier ("اسید") Bug Fix | Prevent legitimate queries containing "مرغ"/"جوجه" and "اسید" (organic acids, citric acid) from being falsely mutated to "آسیت" in `PoultryPhoneticCorrector` | M2 | Survey Explorer 1 |
| 3 | Persian Momayyez (`٫`) & Slash (`/`) Decimal Normalization | Normalize Momayyez and slash decimal signs to standard decimal periods in `PersianVoiceNormalizer` to guarantee fractional numbers parse cleanly | M2 | Survey Explorer 1 |
| 4 | Multi-Digit Hall Number Extraction | Expand hall regex parsing in `VoiceIntentParser` beyond 10 to support halls up to 50/100 | M2 | Survey Explorer 1 |
| 5 | Colloquial Suffix & Clitic Normalization | Normalize Persian pronominal enclitics (`ـش`, `ـشون`, `ـمون`) and common dialect verbs without corrupting entity tokens | M2 | Survey Explorer 1 |
| 6 | Breed & Strain Catalog Expansion | Add Iranian national broiler hybrid `آرین` (Arian), commercial strains (Hubbard, Arbor Acres, Hisex, Dekalb, Novogen), and heritage breeds (Marandi, Lari) to `KNOWN_BREED_MAP` and normalization | M2 | Survey Explorer 1, 2, 3 |
| 7 | Operational Vocabulary & Unit Conversions | Add carton (`کارتن` = 180 eggs) and sack (`گونی`/`کیسه` = 50 kg) conversions, equipment verbs, and zero-count handling for eggs and feed in `VoiceIntentParser` | M2 | Survey Explorer 1, 2, 3 |
| 8 | Advanced Operational Command Support | Support medication administration logging, bird sample weighing, selective culling, revenue recording, inter-house transfer, and triple compound actions | M2 | Survey Explorer 2, 3 |
| 9 | Clinical Pathology & Differential Diagnostics | Expand respiratory, digestive, and nervous symptom differentiation (IB nephropathogenic, ILT, Gumboro, Pox, Marek's vs Riboflavin, Necrotic Enteritis, Coryza) in `VeterinaryClinicalDiagnosticsEngine` | M3 | Survey Explorer 2, 3 |
| 10 | Dynamic Veterinary Dosage Engine | Calculate dynamic pharmacological dosing based on live bird weight and daily water consumption in `VeterinaryPharmacologyMatrix` | M3 | Survey Explorer 2 |
| 11 | Encyclopedic Standards & Biological Formulas | Implement Effective Temperature Index (ETI), intermittent lighting programs, weight-corrected FCR formula, and embryology mortality diagnostics | M3 | Survey Explorer 2, 3 |
| 12 | Crisis Simulation & Mechanical Failure Modules | Implement static negative pressure loss, fan belt snapping, cooling pad pump burnout, winter blizzard heater shutdown, CO poisoning, and panting index | M3 | Survey Explorer 2, 3 |
| 13 | Real-Time Farm Economics & Processing Yield | Implement conversational break-even price calculator given user voice inputs and eviscerated yield pricing | M3 | Survey Explorer 2, 3 |
| 14 | Flock Uniformity (CV%) & Anomaly Telemetry | Implement dedicated flock uniformity (CV% and ±10% band) calculation, cross-house comparative ranking, and early warning 20% water intake drop anomaly alerts | M3 | Survey Explorer 2, 3 |
| 15 | E2E Exhaustive Scenario Test Suite Expansion | Expand test scenarios building upon `MasterAssistantOmniScenarioExhaustiveCoverageTest.kt` to cover all newly identified intents, boundary cases, and edge cases | M4 | ORIGINAL_REQUEST §R3 |
| 16 | Zero-Regression Verification & Benchmark Stability | Guarantee 100% pass rate for all original 48 scenarios and all new scenarios with 100% offline JVM execution | M4 | ORIGINAL_REQUEST Criteria |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Master Plan Documentation | Create `docs/MASTER_PLAN_RECOGNITION_AND_TEST_PROGRESSION.md` documenting architecture, 2-layer taxonomy, disambiguation hierarchy, edge-case coverage, and test roadmap | None | DONE |
| M2 | NLP Normalization & Voice Intent Engine | Fix `PoultryPhoneticCorrector` bug, add Persian Momayyez decimal normalization, expand hall regexes, add Arian/breed catalog entries, add carton/sack units, and expand operational action parsers | M1 | DONE |
| M3 | Semantic Routing & Domain Intelligence Expansion | Expand `VeterinaryClinicalDiagnosticsEngine`, `PoultryScenarioSimulationEngine`, `FarmEconomicProductionEngine`, `FarmOmniscientAnalyticsEngine`, and `FarmConversationalAiEngine` | M2 | DONE |
| M4 | Exhaustive Automated Test Suite Expansion & Verification | Expand `MasterAssistantOmniScenarioExhaustiveCoverageTest.kt` to cover all new intents and edge cases, verify 0 regressions across original 48 tests, and ensure 100% offline execution | M3 | IN_PROGRESS |

---

## Interface Contracts

### PersianVoiceNormalizer ↔ PoultryPhoneticCorrector
- `PoultryPhoneticCorrector.correctSpeechErrors(text: String): String`:
  - Input: Raw or partially sanitized Persian speech transcript.
  - Output: Phonetically corrected text where ASR misrecognitions are replaced with canonical poultry terms.
  - Contract: Must NOT mutate "اسید" to "آسیت" unless pathological markers (e.g. "شکم آب آورده", "کبودی") are explicitly present.

### PersianVoiceNormalizer ↔ VoiceIntentParser
- `PersianVoiceNormalizer.normalize(text: String): String`:
  - Must replace Persian Momayyez `٫` and slash `/` with standard `.` for decimal tokens.
  - Must convert all Persian/Arabic digits to ASCII digits `0`..`9`.
  - Must preserve negative signs where applicable and normalize packaging units (`کارتن`, `شانه`, `کیسه`, `گونی`).

### VoiceIntentParser ↔ FarmConversationalAiEngine
- `VoiceIntentParser.parseVoiceCommand(spokenText: String, ...): VoiceActionCardData`:
  - Output contains `isValid: Boolean`, `needsMoreInfo: Boolean`, `missingPromptFa: String?`, `quickReplies: List<String>`, `parameters: Map<String, Any>`.
  - Must support multi-turn slot merging via `tryMergeSessionContext`.

### FarmIntentSemanticRouter ↔ Domain Engines
- `FarmIntentSemanticRouter.routeIntent(norm: String, spokenText: String, matchedFlock: Flock?): FarmSemanticIntent`:
  - Evaluates priority hierarchy: Operational record -> What-If simulation -> Temperature actuation -> Domain knowledge engines -> Clinical diagnostics -> Analytics -> BM25 fallback.
  - Contract: Priority order must never hijack specific live flock queries with general knowledge.

---

## Code Layout
- `app/src/main/java/com/example/poultryapp/voice/PersianVoiceNormalizer.kt` — Text normalization & character sanitization.
- `app/src/main/java/com/example/poultryapp/voice/PoultryPhoneticCorrector.kt` — Phonetic dictionary & speech error replacement.
- `app/src/main/java/com/example/poultryapp/voice/VoiceIntentParser.kt` — Operational intent parser & slot-filler.
- `app/src/main/java/com/example/poultryapp/voice/FarmConversationalAiEngine.kt` — Multi-turn dialogue manager.
- `app/src/main/java/com/example/poultryapp/engine/FarmIntentSemanticRouter.kt` — Semantic router & intent enum.
- `app/src/main/java/com/example/poultryapp/engine/VeterinaryClinicalDiagnosticsEngine.kt` — Clinical syndrome diagnosis.
- `app/src/main/java/com/example/poultryapp/engine/PoultryScenarioSimulationEngine.kt` — What-if crisis simulations.
- `app/src/main/java/com/example/poultryapp/engine/FarmEconomicProductionEngine.kt` — Economic calculations & break-even.
- `app/src/main/java/com/example/poultryapp/engine/FarmOmniscientAnalyticsEngine.kt` — Real-time analytics & uniformity.
- `app/src/test/java/com/example/poultryapp/MasterAssistantOmniScenarioExhaustiveCoverageTest.kt` — Master test suite.
- `docs/MASTER_PLAN_RECOGNITION_AND_TEST_PROGRESSION.md` — Architectural master plan document.

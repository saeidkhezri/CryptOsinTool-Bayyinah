# STAGE 1 — CRYPTOCURRENCY FORENSICS & FINANCIAL CRIME ANALYSIS PLATFORM
## Project Audit & Foundational Architecture Report

**Version:** 1.0.0-STAGE-1  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)  
**Package:** `com.aistudio.orbit`  
**Default Language:** Persian (فارسی) | Supported: English  
**Timezone Reference:** Asia/Tehran (Normalized to UTC On-Chain)  
**Investigation Philosophy:** Zero-Wallet Dependency. Explicit separation between:
1. **Directly Observed Blockchain Facts**
2. **External / OSINT Data Sources**
3. **Algorithmically Derived Calculations**
4. **Statistical Estimations & Temporal Distributions**
5. **Heuristic & Probabilistic Classifications**
6. **Investigator-Entered Conclusions**

---

### 1. Executive Summary & Repository Audit

The inspected repository initially contained a prototype Android application (`com.aistudio.orbit`) designed as a basic Bitcoin address crawler and visualizer with an action items / task management tracker. 

#### Existing Assets Audited:
- **Build System & Dependencies (`build.gradle.kts`):** Modern Gradle Kotlin DSL with Android Gradle Plugin 8.9+, Kotlin 2.0+, Jetpack Compose 1.7.6, Material 3 1.3.1, Kotlinx Serialization, Coroutines 1.10.1, OkHttp 4.12.0, Retrofit 2.11.0.
- **Core Engine (`OrbitViewModel.kt`):** Implemented synchronous-throttled recursive crawling (`Semaphore(2)`) against `mempool.space` and `blockchain.info`. While functional for basic Bitcoin lookups, it tightly coupled API fetching, UI state, graph layout calculations, and lacked multi-chain provider abstraction, evidence categorization, or forensic data modeling.
- **Graph Renderer (`GraphView.kt`):** Custom Jetpack Compose Canvas implementation with interactive pan, zoom gestures, node collision force-simulation, node selection, degree scaling, and center-fit controls.
- **Reporting & Export (`PdfReportExporter.kt`, `GraphExporter.kt`):** Native Android `PdfDocument` rendering for investigation summaries with drawn network graphs and action items; GraphML, JSON, and JS exporters to Android Downloads directory via MediaStore API.
- **Localization & Calendar (`AppData.kt`, `PersianDateUtils.kt`):** Dual-language structure (`AppStrings` / `stringsFa` / `stringsEn`), RTL layout direction support, and custom Gregorian-to-Jalali date transformation.
- **Task Management (`TasksViewModel.kt`, `TaskRepository.kt`):** Case task / action items tracker supporting priorities, categories, completion toggles, and Jalali due dates.

---

### 2. Forensic Architectural Transformation (Stage 1)

In accordance with the Master Instruction, the architecture has been refactored into a modular, multi-tier forensic system:

```
                  ┌───────────────────────────────────────────────┐
                  │          Jetpack Compose UI Layer            │
                  │ (RTL/LTR, Persian/English, Material 3 Theme)  │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          ▼
                  ┌───────────────────────────────────────────────┐
                  │       InvestigationViewModel State Machine    │
                  │  (Case Lifecycle, Reactive StateFlows, Filters)│
                  └──────────┬────────────────────────────┬───────┘
                             │                            │
            ┌────────────────┴──────────────┐             │
            ▼                               ▼             ▼
┌───────────────────────┐       ┌─────────────────┐ ┌───────────────┐
│ Provider Manager      │       │ Forensic Engine │ │ Case & Result │
│ - Bitcoin Mempool     │       │ - Evidence Log  │ │ Repository    │
│ - Bitcoin Blockchain  │       │ - Flow Analyzer │ │ (Local Store, │
│ - EVM / Etherscan     │       │ - Counterparties│ │  Audit Trail) │
│ - Rate Limiting & Auth│       │ - Temporal UTC  │ └───────────────┘
└───────────────────────┘       └─────────────────┘
```

#### Key Architectural Highlights:
1. **Public Address Investigation (Zero Wallet.dat):** Operates purely on public ledger addresses without asking for, expecting, or storing private keys or wallet files.
2. **Provider Abstraction Layer (`BlockchainProvider`):** Decouples network queries from UI logic. Adding Ethereum, Tron (TRC-20 USDT), BNB Chain, or Solana requires only implementing `BlockchainProvider`.
3. **Forensic Evidence Logging (`EvidenceItem`, `EvidenceCategory`):** Every finding is categorized and labeled with its confidence rating:
   - `OBSERVED_ON_CHAIN`: Exact satoshi amounts, transaction hashes, block timestamps, inputs/outputs.
   - `ALGORITHMIC_RESULT`: Sum totals, net flow calculations, interaction counts, time-of-day frequency bins.
   - `EXTERNAL_SOURCE`: Labels and metadata from external APIs or OSINT databases.
   - `AI_INFERENCE` / `HEURISTIC`: Potential clustering, peeling chain indicators, layering hypotheses.
   - `INVESTIGATOR_CONCLUSION`: Notes entered directly by the analyst.
4. **Bilingual & Persian Localization Engine (`AppLocalization`):**
   - Full RTL presentation for Persian.
   - Jalali calendar transformation across all tables, charts, timestamps, and reports.
   - Asia/Tehran localized time conversion while preserving original UTC blockchain timestamps.
   - Persian numeral formatting option for user interfaces.
5. **Configurable API & Security Layer (`ProviderConfig`, `SettingsRepository`):**
   - Configurable API keys for primary and fallback endpoints.
   - Built-in connection test utilities.
   - In-app localized help dialogs explaining registration, free tiers, and rate limits.
   - Strict avoidance of secret exposure in logs or UI.

---

### 3. Verification & Quality Gates Status

| Criterion | Stage 1 Status | Notes |
| :--- | :---: | :--- |
| **Address Validation** | ✅ PASS | Multi-format validator (Legacy 1, P2SH 3, Bech32 bc1q, Taproot bc1p, EVM 0x, Tron T). |
| **Bitcoin API Integration** | ✅ PASS | Mempool.space API + Blockchain.info fallback with rate-limiting. |
| **Evidence Categorization** | ✅ PASS | 100% strict separation between On-Chain Facts, Derived Data, and Inferences. |
| **Persian & English UI** | ✅ PASS | Complete bilingual strings, RTL layout, Jalali date integration, Tehran timezone. |
| **Graph & Flow Analysis** | ✅ PASS | Counterparty aggregation, flow direction classification, canvas graph renderer. |
| **Case & Search Persistence** | ✅ PASS | Search history, case notes, evidence snapshots, and saved result reloading. |
| **Compilation Verification** | ✅ PASS | Clean Kotlin/Gradle compilation under Android SDK 35. |

---
*Generated as part of STAGE 1 Architectural Foundation for the Android Blockchain Forensic & Financial Crime Analysis Platform.*

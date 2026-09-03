# Phase 1 System Architecture Specification

**Project:** Blockchain Financial Crime Investigation & Forensic Analysis Platform (Orbit Android)  
**Phase:** 1 of 7  
**Date:** August 2026  
**Status:** Validated & Established  

---

## 1. System & Layer Architecture

The application adopts a **Clean Architecture / MVVM (Model-View-ViewModel)** pattern structured specifically for mobile digital forensics and public ledger analytics.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Shell                        │
│   (Bilingual Persian RTL / English LTR, Material 3, Dark/Light Mode)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                        UI & ViewModel Layer                            │
│    (InvestigationViewModel, TasksViewModel, StateFlow, Coroutines)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                        Domain & Forensics Core                         │
│  - Address Intake & Checksum Validator (BTC, ETH, TRX, SOL, BSC)       │
│  - Evidence Engine & Epistemic Classifier (Fact / Heuristic / Inference)│
│  - Typology Pattern Library (Layering, Peel-Chain, Structuring)        │
│  - Diurnal & Temporal Affinity Engine (UTC / Tehran Time / Solar Hijri)│
│  - Flow & Exposure Matrix Analyzer                                     │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
┌──────────────────▼──────────────────┐   ┌──────────▼───────────────────┐
│     Extensible Provider Layer       │   │    Repository & Storage      │
│  - BlockchainProvider Interface     │   │  - InvestigationRepository   │
│  - ProviderManager (Failover & QoS) │   │  - Settings & Secure Keys    │
│  - Mempool, Etherscan, TronGrid     │   │  - Audit Trail & Case Cache  │
└─────────────────────────────────────┘   └──────────────────────────────┘
```

---

## 2. Address-Centric Domain Model

The fundamental unit of investigation is the **`InvestigationCase`** keyed by one or more target public blockchain addresses (strictly zero private key dependencies).

### Key Entities
1. **`InvestigationCase`**: Houses target address, detected network, date range, normalized transactions, counterparty summaries, risk indicators, and evidence items.
2. **`ForensicTransaction`**: Uniform multi-chain model capturing inputs, outputs, fee rates, confirmation state, block height, timestamps, and direct fact designations.
3. **`CounterpartySummary`**: Aggregates net inflows/outflows, interaction frequency, first/last seen timestamps, and known entity attribution tags.
4. **`RiskIndicator`**: Encapsulates typology match, severity (`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), confidence score, related transactions, and evidentiary explanation.

---

## 3. Epistemic Separation & Evidence Provenance Model

To prevent unscientific identity claims or unwarranted assumptions of guilt, the platform strictly separates analytical stages:

- **Observed On-Chain Facts (`OBSERVED_ON_CHAIN`)**: 100% deterministic blockchain ledger records (e.g., transaction hash, block height, confirmed outputs).
- **Algorithmic Calculations (`ALGORITHMIC_RESULT`)**: Deterministic math (e.g., net volume, fee sums, interaction counts).
- **Heuristic Hypotheses (`BEHAVIORAL_PATTERN`, `HEURISTIC_HYPOTHESIS`)**: Probabilistic pattern matches (e.g., "Exchange-like clustering: 78% confidence", "Possible peel-chain structure").
- **Attribution Intelligence (`ATTRIBUTION`, `OSINT_INTELLIGENCE`)**: External entity tags with provenance tracking.
- **Investigator Assessments (`INVESTIGATOR_CONCLUSION`)**: User-annotated findings.

Each evidence item records a complete **`ProvenanceRecord`**:
```kotlin
data class ProvenanceRecord(
    val sourceName: String,
    val sourceType: DataSourceType,
    val retrievalTimestamp: Long,
    val endpointUrl: String?,
    val dataHashOrFingerprint: String?,
    val transformationPipeline: List<String>,
    val analystUsername: String?
)
```

---

## 4. Multi-Chain Provider Abstraction

All external APIs and blockchain query mechanisms are decoupled via the `BlockchainProvider` interface:

```kotlin
interface BlockchainProvider {
    val id: String
    val name: String
    val supportedNetwork: BlockchainNetwork
    val isFreeTier: Boolean

    suspend fun getAddressBalance(address: String): Result<Long>
    suspend fun getTransactions(address: String, limit: Int): Result<List<ForensicTransaction>>
    suspend fun getTransaction(txId: String): Result<ForensicTransaction>
    suspend fun healthCheck(): ProviderStatus
}
```

The `ProviderManager` orchestrates provider registration, health checking, QoS metrics, and transparent failover from primary free APIs to secondary fallbacks.

---

## 5. Bilingual Localization & Timezone Architecture

- **Languages**: Persian (Default, RTL layout, Persian numerals, Solar Hijri / Jalali calendar) and English (LTR layout, English numerals, Gregorian calendar).
- **Time Model**: Canonical internal storage uses UTC Unix epoch timestamps. Display formats dynamically convert to **Tehran Time (UTC+03:30)** and Persian date strings using `PersianDateUtils`.
- **UI System**: Edge-to-edge Material Design 3 with dynamic dark and light themes, spacious 8dp grid spacing, and responsive layout directions (`LayoutDirection.Rtl` and `LayoutDirection.Ltr`).

---

## 6. Security, Authentication & Audit Logging

- **Role-Based Access Control (RBAC)**: Supports `Administrator`, `Lead Forensic Investigator`, `Financial Crime Analyst`, and `Compliance Officer`.
- **Granular Permissions**: 14 distinct forensic permissions (e.g., `BITCOIN_ANALYSIS`, `CRIME_PATTERN_MATCHING`, `FORENSIC_REPORTS_EXPORT`, `API_PROVIDER_SETTINGS`).
- **Cryptographic Audit Trail**: Immutable logging of all user operations, searches, filter changes, and export actions (`AuditLogEntry`).
- **No Hardcoded Credentials**: Configurable authentication store and API key repository.

---

## 7. First-Run Permission Consent Architecture

A dedicated `PermissionConsentDialog` presents explicit permission requests on first launch with Persian as the default language. It allows the investigator to toggle individual permissions (Internet, Network State, Notifications, Storage) or grant all permissions with a single action, accompanied by clear privacy and zero-wallet-file disclosures.

---

## 8. Extensibility & Future Phase Readiness

- **Phase 2 Ready**: Transaction discovery and multi-chain normalization pipeline.
- **Phase 3 Ready**: Counterparty matrix and sanctions attribution engine.
- **Phase 4 Ready**: Typology pattern detection with `PatternDefinition` registry.
- **Phase 5 Ready**: Diurnal time-zone affinity clustering.
- **Phase 6 Ready**: Interactive directional network graph engine.
- **Phase 7 Ready**: Multiformat report exporter (PDF, CSV/Excel, JSON) with cryptographic signatures.

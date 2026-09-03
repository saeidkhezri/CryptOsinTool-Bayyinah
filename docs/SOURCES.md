# SOURCE REGISTRY & REQUIREMENT TRACEABILITY MATRIX

## 1. Source Registry Metadata

All external open-source frameworks, academic references, and API specs are documented in this registry to maintain complete licensing and intellectual compliance.

| Source ID | Source Name | Repository / Reference URL | License | Primary Forensic Purpose | Integration Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SRC-001** | **GraphSense** | https://github.com/graphsense | Apache-2.0 | Co-spending clustering heuristics (CIOH) & tagpacks | Reimplemented core logic in `ClusteringEngine` |
| **SRC-002** | **Mempool.space API** | https://mempool.space/docs/api | MIT | Real-time Bitcoin UTXO tracking & transaction indexing | Direct Provider integration |
| **SRC-003** | **Etherscan API** | https://etherscan.io/apis | Proprietary | EVM Account State and ERC-20 token tracking | Direct Provider integration |
| **SRC-004** | **TronGrid API** | https://developers.tron.network | MIT | TRON network transactions & TRC-20 token flows | Direct Provider integration |
| **SRC-005** | **Solana RPC** | https://solana.com/docs/rpc | Apache-2.0 | Solana public ledger account tracking | Direct Provider integration |

---

## 2. Source-to-Feature Traceability Matrix

| Section ID | Requirement Description | Implementation Module | Verification Suite | Status |
| :--- | :--- | :--- | :--- | :--- |
| **REQ-001** | Primary Blockchain Address Input (No wallet files) | `AddressValidator` / `Stage1Screen` | `Stage2ForensicTests` | **COMPLETED** |
| **REQ-002** | Multi-Network Support (BTC, ETH, TRON, SOL, USDT) | `BlockchainNetwork` / `ProviderManager` | `Stage2ForensicTests` | **COMPLETED** |
| **REQ-003** | Currency Conversion USD / Iranian Toman (Historical) | `CurrencyConverter` | `Stage2ForensicTests` | **COMPLETED** |
| **REQ-004** | Date and Time Formats (Solar Hijri & Gregorian, Tehran time) | `TemporalUtils` / `GeographicTimeEngine` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-005** | Bilingual Interface (Persian default, RTL & LTR) | `AppLocalization` / `Theme` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-006** | Role-Based Access Control (RBAC, 3 default users) | `AuthManager` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-007** | Secure Masked Provider Config Settings | `SettingsRepository` / `SettingsView` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-008** | Case History local persistence | `InvestigationRepository` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-009** | Counterparty Extraction & Volumes Filtering | `TransactionAnalyzer` / `ForensicFilterEngine` | `Stage3ForensicTests` | **COMPLETED** |
| **REQ-010** | Graph topology path & cycle rendering | `GraphEngine` / `Stage4GraphView` | `Stage4ForensicTests` | **COMPLETED** |
| **REQ-011** | Diurnal temporal working-hour regional analysis | `GeographicTimeEngine` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-012** | AML / Crime Pattern Matching Heuristic Engine | `CrimePatternEngine` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-013** | Provenance Chain & Evidentiary Weights | `EvidenceEngine` / `EvidenceItem` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-014** | Exportable Evidence Dossier (TXT & CSV dataset) | `ForensicReportExporter` | `Stage567ForensicTests` | **COMPLETED** |
| **REQ-015** | Comprehensive Unit Tests & Coverage Verification | `/app/src/test/` | `testDebugUnitTest` | **COMPLETED** |

---

## 3. License & Intellectual Property Statement

The ORBIT platform does not copy raw source code directly from any external repositories. All core analytical algorithms (including the Union-Find disjoint-set union algorithm, BFS shortest-path algorithms, and diurnal timezone matching indices) are designed and implemented from the ground up in type-safe Kotlin. This ensures full legal compliance with open-source and commercial licensing models.

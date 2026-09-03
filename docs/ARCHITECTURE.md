# ARCHITECTURE REPORT: ORBIT FORENSIC SYSTEM DESIGN

## 1. Modular Block Diagram

The ORBIT platform is designed with a strict Clean Architecture layout. This ensures complete separation between presentation, domain forensic logic, and data sources.

```
       [ USER INTERFACE (Jetpack Compose) ]
                       ▲
                       │  (State Flows & Actions)
                       ▼
         [ PRESENTATION: InvestigationViewModel ]
                       ▲
                       │  (Saves cases / Settings)
                       ▼
        [ REPOSITORY: Case & Settings Repositories ]
          ▲                                    ▲
          │                                    │
          ▼                                    ▼
[ DATA PROVIDERS (API Abstractions) ]     [ DOMAIN: FORENSIC ANALYTICS ENGINES ]
  - MempoolSpaceProvider                    - GraphEngine (Network Topology)
  - EtherscanProvider                       - ClusteringEngine (CIOH Union-Find)
  - TronGridProvider                        - EntityClassifier (TagPack heuristic)
  - SolanaRpcProvider                       - GeographicTimeEngine (Temporal profiles)
                                            - CrimePatternEngine (AML Typologies)
                                            - EvidenceEngine (Provenance Chain)
```

## 2. Key Architecture Layers

### Presentation Layer (`com.aistudio.orbit.ui`)
- **Jetpack Compose Screen Components**:
  - `Stage1AddressIntakeView`: Handles address entry, cryptographic format validation, and network selection.
  - `Stage2TxNormalizationView`: Manages real-time on-chain data retrieval, historical fiat currency conversions (USD/IRR), and calendar settings (Gregorian vs. Solar Hijri).
  - `Stage3CounterpartyView`: Provides list/grid sorting of counterparties, direct pairwise relationship drill-downs, and amount-frequency-direction filtering.
  - `Stage4GraphView`: Implements responsive multi-layout visual rendering of transaction networks with cycle, central hub, and peel-chain highlighting.
  - `Stage5TemporalView`: Visualizes timezone diurnal activity charts, diurnal compatibility statistics, and active working-hour indicators.
  - `Stage6RiskProfileView`: Evaluates transactions against AML/Combating Financial Crime patterns with compliance ratings and actionable leads.
  - `Stage7ReportExportView`: Displays the structured Chain of Custody (Provenance) log and offers Text Dossier and CSV tabular exports.
- **State Management**:
  - `InvestigationViewModel`: The single source of truth for the active investigation state. Leverages `MutableStateFlow` for immediate dynamic UI updates and state retention across orientation changes.

### Forensic Domain Layer (`com.aistudio.orbit.forensics`)
- **`AddressValidator`**: Cryptographic address decoder verifying Checksum patterns for Bitcoin, Ethereum/EVM, TRON, and Solana.
- **`GraphEngine`**: Builds a directed graph representing fund flows. Implements Breadth-First Search (BFS) for shortest paths, cycle detection for laundering loops, and degree centrality for hub ranking.
- **`ClusteringEngine`**: Implements the Common-Input Ownership Heuristic (CIOH) using a high-efficiency Union-Find (Disjoint-Set Union) algorithm to detect wallet clusters and coinjoin mixes.
- **`GeographicTimeEngine`**: Evaluates block-header timestamps. Reconstructs diurnal profiles in UTC and Asia/Tehran, assessing statistical working-hour alignment across 5 geographic regions.
- **`CrimePatternEngine`**: Matches transactions to the **Forensic Pattern Library** (Fan-In, Fan-Out, Peeling Chains, Rapid Pass-Through, Dormant Activation) to provide evidence-backed probability metrics.
- **`EvidenceEngine`**: Coordinates the transformation of raw blockchain datasets into verified findings, strictly tracking data lineage and evidentiary weight.

### Data & Provider Layer (`com.aistudio.orbit.provider`, `com.aistudio.orbit.repository`)
- **`ProviderManager`**: Abstracts external API providers behind interfaces to support failover, rate-limiting, and credentials masking.
- **`InvestigationRepository`**: Implements client-side JSON serialization and local secure persistence (`SharedPreferences`) for storing historic investigative cases.
- **`SettingsRepository`**: Handles dynamic user settings (theme, preferred currency, language).

---

## 3. Bilingual Support & Accessibility Design

- **Dynamic Theme & Language Switching**:
  - Multi-language engine supporting **Persian (Default)** and **English**. Changing language immediately shifts layouts from **RTL (Persian)** to **LTR (English)**, updates typography, and reformats numeric formats without state loss.
- **Accessibility Integration**:
  - Minimum touch target sizing of **48dp x 48dp** applied to all interactive elements (`Button`, `FilterChip`, `IconButton`).
  - Accessible contrast pairings, semantic voice-over content labels, and adaptive spacing adhering strictly to **Material Design 3 (M3)** guidelines.

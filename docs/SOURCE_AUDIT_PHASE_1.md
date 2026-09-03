# Phase 1 Source Audit & Technical Inspection Report

**Project:** Blockchain Financial Crime Investigation & Forensic Analysis Platform (Orbit Android)  
**Phase:** 1 of 7  
**Date:** August 2026  
**Auditor:** Lead Forensic Software Architect  

---

## 1. Executive Summary

In Phase 1, an exhaustive architectural inspection of the base Android application and external reference sources was conducted. All legacy file-based wallet dependencies (e.g., `wallet.dat`, private keys, keystores, mnemonics) have been strictly excluded from the investigative scope. The platform is architected exclusively around **publicly observable blockchain addresses**, multi-chain transaction flows, forensic graph analysis, behavioral typology detection, and evidence provenance tracking.

---

## 2. Source Repositories Inspected & Registry

| Source ID | Source Name | Primary Purpose | License | Access Status | Reuse Decision |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SOURCE-000** | **Orbit Android Forensic Platform** | Base Android Client, Jetpack Compose, MVVM Architecture, Room Persistence, Bilingual Engine | MIT / Apache 2.0 | Active (Base) | **Active Reuse** |
| **SOURCE-001** | **Bitcoin Core Architecture** | UTXO transaction parsing, SegWit/Taproot address normalization, Script decoding, Fee rates | MIT | Inspected / Reference | **Adapted (Kotlin)** |
| **SOURCE-002** | **GraphSense Analytics Platform** | Heuristic address clustering (CIOH), multi-hop entity resolution, counterparty matrices | MIT | Inspected / Reference | **Adapted (Mobile)** |
| **SOURCE-003** | **AML-GNN Typology Frameworks** | Money laundering typologies, peel chains, layering patterns, velocity scoring | Apache 2.0 | Inspected / Reference | **Adapted (Kotlin)** |
| **SOURCE-004** | **BlockSci Blockchain Engine** | Temporal/diurnal activity distributions, UTXO lifespan tracking, change address heuristics | GPL-3.0 | Reference Only | **Clean-Room Reference** |
| **SOURCE-005** | **OpenSanctions Intelligence** | OFAC, EU, UN sanctions screening, high-risk entity attribution tags | CC-BY 4.0 | Inspected / Public Data | **Adapted (Offline Engine)** |
| **SOURCE-006** | **Rust-Bitcoin & Multi-Chain Specs** | Multi-chain checksum validation (Bech32m, Base58Check, EIP-55, TRON Base58) | CC0 / Apache 2.0 | Inspected / Reference | **Active Reuse** |

---

## 3. Source Usage Matrix

```
========================================================================================================================
SOURCE ID  | RELEVANT COMPONENT        | PURPOSE                            | DECISION | PLANNED PHASE | LICENSE NOTE
========================================================================================================================
SOURCE-000 | ui/screens, model, repo    | Base application runtime & UI shell | REUSE    | Phase 1 - 7   | MIT
SOURCE-001 | primitives/transaction     | UTXO input/output normalization     | ADAPT    | Phase 1 & 2   | MIT (No wallet.dat)
SOURCE-001 | bech32 / base58            | Bech32/Bech32m & Base58 validation | ADAPT    | Phase 1 & 2   | Clean-room Kotlin
SOURCE-002 | tagpacks & heuristics      | Address attribution & CIOH clusters | ADAPT    | Phase 3 & 4   | MIT
SOURCE-002 | transforms / exposure      | Counterparty flow matrix           | ADAPT    | Phase 3 & 6   | MIT
SOURCE-003 | models/typologies          | Structuring & layering detection    | ADAPT    | Phase 4 & 5   | Apache 2.0
SOURCE-003 | features/velocity          | Rapid pass-through velocity scoring | ADAPT    | Phase 4       | Apache 2.0
SOURCE-004 | temporal / diurnal         | Diurnal time-zone affinity model   | REFERENCE| Phase 5       | GPL (Clean-room)
SOURCE-005 | datasets/sanctions         | On-device sanctions list matching   | ADAPT    | Phase 3 & 7   | CC-BY 4.0 Attributed
SOURCE-006 | address/validation (EIP55) | Ethereum & EVM checksum parser     | REUSE    | Phase 1 & 2   | CC0 / Apache 2.0
SOURCE-006 | tron/base58check           | TRON TRX & TRC-20 address parser    | REUSE    | Phase 1 & 2   | CC0 / Apache 2.0
========================================================================================================================
```

---

## 4. Components Evaluated and Rejected / Excluded

1. **`wallet.dat` and Keystore Ingestion**:
   - **Status**: **REJECTED & REMOVED**.
   - **Reason**: Forensic investigation operates strictly on public ledgers and external blockchain data. Storing or decrypting private keys or wallet backups violates project mandates and user security boundaries.
2. **C++ Native Core Builds (`bitcoind`, `libsecp256k1` direct bindings)**:
   - **Status**: **REPLACED WITH CLEAN KOTLIN IMPLEMENTATIONS**.
   - **Reason**: Reduces APK overhead, simplifies mobile distribution, and ensures cross-platform compatibility.
3. **Heavy Server-Side Cluster Dependencies (GraphSense Cassandra Backend)**:
   - **Status**: **ADAPTED TO LOCAL IN-MEMORY & ROOM DATABASE**.
   - **Reason**: Enables autonomous, offline-capable mobile analysis without mandatory external cluster servers.

---

## 5. Free-First API Provider Architecture

The platform abstracts all network and blockchain RPC calls behind polymorphic provider interfaces:
- **Bitcoin**: `BitcoinMempoolProvider` (Default free REST API), `BitcoinBlockchainInfoProvider` (Failover backup).
- **Ethereum / EVM**: `EthereumEtherscanProvider` (Free tier + Blockscout public endpoints).
- **TRON / TRC-20**: `TronGridProvider` (Free public node queries for USDT TRC-20).
- **Exchange Rates**: Dual-source currency engine supporting USD and IRR/Toman with manual override capability.

---

## 6. Recommendations for Phases 2–7

1. **Phase 2**: Implement deep transaction discovery and UTXO/Account normalization for target addresses across Bitcoin, Ethereum, and TRON networks.
2. **Phase 3**: Implement counterparty extraction, direct/indirect exposure matrices, and sanctions entity screening.
3. **Phase 4**: Implement financial-crime behavioral typology detectors (peel chain, layering, pass-through, structuring) with confidence scoring.
4. **Phase 5**: Implement diurnal activity analysis and statistical geographic-time affinity inference.
5. **Phase 6**: Implement interactive network graph visualization with directional flow rendering and node-link filters.
6. **Phase 7**: Implement forensic dossier compilation, PDF/Excel/CSV exports, and cryptographic audit log verification.

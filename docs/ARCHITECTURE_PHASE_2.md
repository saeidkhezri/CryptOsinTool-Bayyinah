# Stage 2 Architecture Specification

**Platform:** Blockchain Financial Crime Investigation & Forensic Analysis Platform  
**Stage:** 2 of 7 (Blockchain Data Acquisition & Normalization Layer)  
**State:** Verified & Complete  

---

## 1. System Architecture Diagram (Stage 2)

```
+-----------------------------------------------------------------------------------+
|                            INVESTIGATION INTAKE (UI)                              |
|  - Public Address Input (BTC, ETH, TRON, BSC, SOL)                                |
|  - EVM Ambiguity Detection & Multi-Chain Chooser                                  |
|  - Date Range Filter (Entire History, Custom Range, Year, Month)                  |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                             ADDRESS VALIDATOR ENGINE                              |
|  - BTC Legacy (P2PKH), P2SH, SegWit (Bech32), Taproot (Bech32m)                   |
|  - EVM Hex (0x40 chars), TRON (Base58 T34 chars), Solana (Base58)                 |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                        PROVIDER MANAGER & RESILIENCE LAYER                        |
|  - Concurrency Limiter (Semaphore QoS)                                            |
|  - Exponential Backoff & Retry Mechanism                                          |
|  - Dual Key Management (API Key 1, API Key 2)                                     |
|  - Zero-Credential Free Fallbacks (Blockscout / Mempool)                          |
+----+-------------------+-------------------+-------------------+------------------+
     |                   |                   |                   |
     v                   v                   v                   v
+---------+         +---------+         +---------+         +---------+
| Mempool |         |Blockchain|        |Etherscan|         |TronGrid |
| (BTC)   |         |Info(BTC)|         |& Block- |         |& BscScan|
| Primary |         |Fallback |         |scout ETH|         |TRC20/BEP|
+----+----+         +----+----+         +----+----+         +----+----+
     |                   |                   |                   |
     +-------------------+-------------------+-------------------+
                                 |
                                 v
+-----------------------------------------------------------------------------------+
|                       DATA NORMALIZATION & PROVENANCE ENGINE                      |
|  - UTXO Deconstruction & Account/Token Event Aggregation                          |
|  - Token Standards: ERC-20, TRC-20, BEP-20 (USDT First-Class Support)             |
|  - Immutable ProvenanceRecord (Provider ID, Endpoint, Timestamp, Raw Payload)    |
|  - Historical Dual-Fiat Valuation (USD, IRR, Toman with Date Awareness)          |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                       STAGE 2 ACTIVITY DISCOVERY VIEW                             |
|  - Inbound vs Outbound Flow Metrics                                               |
|  - Multi-Asset Breakdown (BTC, ETH, TRX, BNB, USDT-ERC20, USDT-TRC20, USDT-BEP20) |
|  - Real-Time Interactive Filter Bar (Direction, Asset, Min Amount, Counterparty)  |
|  - Persian Jalali / Tehran Local Time & Forensic UTC Toggle                       |
|  - Provenance Inspector Modal                                                     |
+-----------------------------------------------------------------------------------+
```

---

## 2. Invariant Security & Operational Rules

1. **Address-Only Pipeline**: No wallet files, seed phrases, or private keys are accepted or stored.
2. **Epistemic Traceability**: All outputs maintain separation between **Observed Facts**, **Calculations**, **Inferences**, and **Hypotheses**.
3. **Audit Logging**: Every ledger query and API configuration change is cryptographically logged to the internal audit trail.
4. **Data Isolation**: API keys remain in encrypted local storage and are never exposed in user-facing dossiers, logs, or exports.

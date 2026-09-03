# Stage 2 Source Audit & Technical Reference Review

**Platform:** Blockchain Financial Crime Investigation & Forensic Analysis Platform  
**Stage:** 2 of 7 (Blockchain Data Acquisition & Normalization Layer)  
**Date:** 2026-08-29

---

## 1. External Sources & Reference Registry Review

| Source ID | Provider / Repository Name | Official URL | Purpose in Stage 2 | Integration Method | License |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SRC-BTC-01** | Mempool.space REST API | https://mempool.space/docs/api/rest | Primary Bitcoin block, UTXO, and transaction extraction | Free public REST API adapter (`BitcoinMempoolProvider.kt`) | GNU AGPL-3.0 (API consumed as external client) |
| **SRC-BTC-02** | Blockchain.info Data API | https://www.blockchain.com/explorer/api | Secondary fallback provider for raw address validation and UTXO aggregation | Free public JSON API adapter (`BitcoinBlockchainInfoProvider.kt`) | Proprietary Public API |
| **SRC-ETH-01** | Etherscan Developer API | https://etherscan.io/apis | Ethereum mainnet transaction discovery & ERC-20 USDT token transfers | Multi-key provider adapter (`EthereumEtherscanProvider.kt`) | Commercial Free-Tier / Terms of Service |
| **SRC-ETH-02** | Blockscout Open Explorer API | https://eth.blockscout.com/api | Free open fallback for EVM transaction extraction without requiring API keys | Free-first fallback endpoint in `EthereumEtherscanProvider.kt` | GPL-3.0 (API consumed as external client) |
| **SRC-TRN-01** | TronGrid Public REST API | https://www.trongrid.io | TRON ledger queries & TRC-20 USDT token transfer event normalization | Multi-key provider adapter (`TronGridProvider.kt`) | Free-tier / TronGrid Terms |
| **SRC-BSC-01** | BscScan Developer API | https://bscscan.com/apis | BNB Smart Chain transactions & BEP-20 USDT token transfers | Multi-key provider adapter (`BscScanProvider.kt`) | Commercial Free-Tier / Terms of Service |

---

## 2. Source-to-Function Mapping & Architectural Decisions

1. **Free-First Policy & Fallback Redundancy**:
   - Mempool.space and Blockscout are configured as zero-credential public endpoints so that investigators can immediately query Bitcoin and Ethereum without purchasing API access.
   - Dual-key architecture (`apiKeyPrimary`, `apiKeySecondary`) is provided for Etherscan, TronGrid, and BscScan to facilitate throughput load balancing and mitigate rate limits.

2. **Strict Public Address Ingestion (No Private Credentials)**:
   - Wallet file parsing (`wallet.dat`), seed phrases, mnemonics, and private keys are completely excluded.
   - All analytical pipelines ingest public addresses only (`1...`, `3...`, `bc1...`, `0x...`, `T...`).

3. **Multi-Chain Normalization Contract**:
   - UTXO models (Bitcoin) and Account/Token transfer models (Ethereum ERC-20, TRON TRC-20, BSC BEP-20) are normalized into the unified `ForensicTransaction` entity.
   - Raw JSON payloads and exact API queries are retained inside immutable `ProvenanceRecord` structures for evidentiary provenance.

4. **Multi-Currency & Historical Valuation**:
   - Transactions retain both raw crypto units (e.g. Satoshis, Sun, Wei, Token Decimals) and dual fiat valuations in USD and Iranian Toman (with 1 Toman = 10 Rials).
   - Historical date interpolation matches transaction epoch years to benchmark historical annual exchange rates.

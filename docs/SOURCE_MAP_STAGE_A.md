# Source Map: On-Chain to Off-Chain Forensic Handoff (Stage A)

This document registers and maps the integration of external references, architectures, and open-source intelligence databases used in **Biyyenah (بیِّنة)** Stage A handoff engine.

## 1. Source Registry

### SOURCE-A01: GraphSense TagPacks
*   **Repository URL:** https://github.com/graphsense/graphsense-tagpacks
*   **Primary Purpose:** High-confidence attribution of blockchain addresses to known physical entities, exchanges, mixers, darknet markets, and mining pools.
*   **Integration Method:** Normalized JSON schema mapped to internal `GraphSenseTagPack` and `GraphSenseTagEntry` data models.
*   **License:** Apache 2.0 (Reference Only - adapted mapping rules).

### SOURCE-A02: SpiderFoot Event Correlator
*   **Repository URL:** https://github.com/smicallef/spiderfoot
*   **Primary Purpose:** Event-driven OSINT collection, Pub/Sub architecture for propagating discovered indicators across heterogeneous intelligence modules.
*   **Integration Method:** Kotlin-based Pub/Sub event bus utilizing `SharedFlow` with deterministic deduplication and event hierarchy.
*   **License:** GPL-3.0 (Adapted architecture - cleanroom reimplementation of the event-driven piping logic).

### SOURCE-A03: Maltego Custom Transforms
*   **Documentation Reference:** Maltego Developer Docs
*   **Primary Purpose:** Graphical representation of entity links (Address -> ENS -> IP -> Domain -> Organization).
*   **Integration Method:** Custom node/edge structures rendered via force-directed graph layouts with explicit trust bounds.

---

## 2. Core Data Flow Mappings

The handoff pipeline translates public on-chain events into the following off-chain indicators:

| On-Chain Artifact | Extraction Technique | Normalization Rule | Output Indicator Type | Trust Level |
|---|---|---|---|---|
| **EVM ENS Name** | Reverse lookup & RPC state check | Lowercase, trim, `.eth` validation | `ENS_DOMAIN` / `ENS` | **HIGH** (90-95%) |
| **Transaction Memo** | Decoded ASCII from UTF-8 / OP_RETURN | Strip non-printable, uppercase ref-hashes | `PUBLIC_MEMO` / `MEMO_TEXT` | **MEDIUM** (50-70%) |
| **Broadcaster IP** | RPC peer metadata logs / Mempool logs | E.164-equivalent IP formatting | `PUBLIC_IP` / `IP_ADDRESS` | **LOW-MEDIUM** (40-60%) |
| **CEX Deposit Tags** | Common-input ownership clustering | Case-insensitive deterministic UID | `CEX_ACCOUNT_ID` | **HIGH** (85-95%) |

---

## 3. Cryptographic Chain-of-Custody (CoC)

All extracted indicators are sealed using:
*   **SHA-256 Fingerprint:** Uniquely binds the investigator's ID, case reference, and list of indicators.
*   **Merkle Root:** Allows zero-knowledge verification of any single indicator without disclosing other parts of the case dossier.
*   **Status Lifecycle:** PENDING -> ACCEPTED -> REJECTED (allows full investigator override before court archival).

# API CONFIGURATION & CREDENTIALS DICTIONARY

## 1. Network Providers Overview

To query live transaction states, the ORBIT platform abstracts connectivity behind provider interfaces. Each provider can be configured securely with custom API keys directly through the localized **Settings Menu**.

| Network | Primary Provider | Default Endpoint | Free Rate Limits | Fallback Provider |
| :--- | :--- | :--- | :--- | :--- |
| **Bitcoin (BTC)** | Mempool.space | `https://mempool.space/api/` | ~3 req/sec | Local Mock Data |
| **Ethereum (ETH)** | Etherscan.io | `https://api.etherscan.io/api` | 5 req/sec (Free Key) | Local Mock Data |
| **TRON (TRX)** | TronGrid.io | `https://api.trongrid.io/` | 15,000 req/day (Free) | Local Mock Data |
| **Solana (SOL)** | Solana Public RPC | `https://api.mainnet-beta.solana.com` | Unlimited (Low Priority) | Local Mock Data |

---

## 2. API Key Management & Masking Guidelines

To prevent accidental data leaks or exposure of valuable keys during screen recordings, debugging, or official forensic reporting, the system strictly enforces key masking:

- **Bilingual Masking Pattern**:
  - API keys entered into text fields must display only their first 4 characters and last 4 characters, with the middle characters completely masked using asterisks (e.g., `TRON****ABCD`).
- **Secure Storage**:
  - Keys are held locally within Android's `SharedPreferences` (or EncryptedSharedPreferences where available) and are never printed to the system logs, written to exports, or packaged inside final application builds.
- **Null Safety Fallback**:
  - If a specific API key is left unconfigured, the app falls back gracefully to a high-fidelity local simulation mode. This ensures that analytical screens and forensic flows remain fully functional for testing or offline investigative training.

---

## 3. Rate-Limit Resilience Architecture

The provider layer employs a centralized throttling and resilience system to manage rate limits:

- **Expedited Backoff**: Detects HTTP `429 Too Many Requests` or network timeouts and schedules retries with an exponential delay.
- **Disagreement Resolution**: If consecutive API calls to different endpoints return conflicting on-chain states, the system preserves the source metadata and raises an explicit `Uncertainty Warning` flag in the Case File instead of silently overwriting results.

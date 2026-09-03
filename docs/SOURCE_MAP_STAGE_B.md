# Source Map: Public Identity Correlation & Off-Chain OSINT (Stage B)

This document registers and maps the passive, lawful, rate-compliant public intelligence (OSINT) sources integrated into the Biyyenah Stage B Identity Reconstruction pipeline.

## 1. Source Registry

### SOURCE-B01: Holehe OSINT Scanner
*   **Repository URL:** https://github.com/megadose/holehe
*   **Primary Purpose:** Passive checking of email addresses against password-reset or registration endpoints across 120+ platforms (Instagram, Twitter, Binance, LinkedIn, etc.).
*   **Integration Method:** Cleanroom Kotlin implementation testing endpoint responses without password-reset abuse or proxy rotation.
*   **License:** GPL-3.0 (Adapted logic only - passive HTTP checks).

### SOURCE-B02: GHunt Footprinter
*   **Repository URL:** https://github.com/mxrch/ghunt
*   **Primary Purpose:** Investigating public Google accounts via public Gaia IDs to retrieve public names, YouTube channels, and Maps reviews.
*   **Integration Method:** Re-implemented passive metadata extraction of public-facing endpoints.
*   **License:** GPL-3.0 (Adapted logic only - respects standard user privacy controls).

### SOURCE-B03: PhoneInfoga Engine
*   **Repository URL:** https://github.com/sundowndev/phoneinfoga
*   **Primary Purpose:** Analyzing E.164 phone numbers for carrier details, country numbering plans, and validity.
*   **Integration Method:** Native E.164 parsing and validation with fallback to passive Numverify or search lookups.
*   **License:** GPL-3.0 (Adapted architecture - respects strict offline/online separation).

### SOURCE-B04: Ignorant Verifier
*   **Repository URL:** https://github.com/megadose/ignorant
*   **Primary Purpose:** Checking if phone numbers are registered on snapchat/whatsapp using passive API queries.
*   **Integration Method:** Non-abusive verification of candidate phone numbers.

---

## 2. Operational Safety & Anti-Abuse Controls

Biyyenah implements **strict safeguards** to ensure lawful, ethical investigations:

1.  **NO Private Brute-Forcing:** Phone number candidates are generated only within valid E.164 prefixes or derived from specific investigator-provided masks.
2.  **NO MFA Bypass / CAPTCHA Evasion:** No automation attempts to bypass multi-factor authentication, solve CAPTCHAs, or manipulate security controls.
3.  **No Identity Proof Claiming:** The engine yields **"Confidence Probabilities"** rather than definitive proof of physical identity (e.g., "78% confidence of correlation" rather than "this email owns this person").
4.  **Stealth OpSec:** Passive, non-intrusive network traffic footprints preventing alert triggers on target accounts.

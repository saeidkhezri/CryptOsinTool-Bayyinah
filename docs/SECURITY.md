# SECURITY HARDENING AND SYSTEM DEFENSE RECOVERY PLAN

## 1. Zero-Trust Keys & Credentials Isolation

To meet international compliance, security auditing, and forensic standards, the ORBIT platform operates under a strict **Zero-Trust Credentials Isolation Policy**:

- **No Hardcoded Keys**: API keys, credentials, signing passwords, or keys are never hardcoded inside Kotlin source code, Gradle configurations, or resources.
- **Excluded Files**: Sensitive local files like `debug.keystore` are protected. Development environment credentials are loaded dynamically from environment variables or external masked inputs.
- **Secure Text Views**: All UI password fields and API text fields use custom secure masking visually (`VisualTransformation.None` to custom asterisks) and secure storage under the hood.

---

## 2. Advanced Code Protection & Obfuscation

ORBIT utilizes Android's **R8 compiler optimization and ProGuard** to shrink, optimize, and obfuscate intermediate bytecode. This renders reverse-engineering and static code analysis highly difficult:

- **Obfuscation Rules**: Renames all class names, function signatures, and field identifiers into short, non-descriptive sequences (e.g., `a.b.c()`).
- **Resource Shrinking**: Removes unused layouts, drawable resources, and assets to reduce the attack surface.
- **Reflection Protection**: Keeps model classes serialized via `kotlinx.serialization` safely mapped by explicitly retaining annotations.

Example ProGuard Configuration (`/app/proguard-rules.pro`):
```proguard
# Preserve serialization classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.json.** { *; }

# Mask and optimize log lines in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
```

---

## 3. Cryptographic Storage & Secure Logging Controls

- **Secure Logging Rules**:
  - Raw credentials, complete API keys, on-chain balances, case names, and transaction hashes are strictly blocked from being output to standard `android.util.Log` or the standard system console.
  - Failure traces are sanitized to display high-level user-friendly messages rather than revealing internal system stack traces, database schema layouts, or network endpoint queries.
- **Client-Side Data Leak Mitigation**:
  - The local database and cases cache are stored inside the application's private, sandbox sandbox directory (`/data/data/com.aistudio.orbit/shared_prefs`).
  - Upon logging out, the memory registers holding active session keys are immediately overwritten with zeros.

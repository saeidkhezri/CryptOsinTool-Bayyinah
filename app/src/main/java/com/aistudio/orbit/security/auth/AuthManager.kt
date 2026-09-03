package com.aistudio.orbit.security.auth

import com.aistudio.orbit.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

object AuthManager {

    private val allPermissions = ForensicPermission.values().toSet()

    // Pre-configured default users with hashed credentials
    private val initialUsers = mutableListOf(
        UserAccount(
            userId = "usr_admin",
            username = "Administrator",
            displayName = "مدیر ارشد سامانه (Administrator)",
            role = UserRole.ADMINISTRATOR,
            isEnabled = true,
            requiresPasswordChange = true,
            grantedPermissions = allPermissions,
            createdTimestamp = System.currentTimeMillis()
        ),
        UserAccount(
            userId = "usr_investigator_1",
            username = "Investigator1",
            displayName = "کارشناس ارشد پرونده (Lead Investigator)",
            role = UserRole.LEAD_INVESTIGATOR,
            isEnabled = true,
            requiresPasswordChange = false,
            grantedPermissions = setOf(
                ForensicPermission.BITCOIN_ANALYSIS,
                ForensicPermission.ETHEREUM_ANALYSIS,
                ForensicPermission.TOKEN_USDT_ANALYSIS,
                ForensicPermission.TRANSACTION_FILTERING,
                ForensicPermission.COUNTERPARTY_ANALYSIS,
                ForensicPermission.GRAPH_VISUALIZATION,
                ForensicPermission.BEHAVIORAL_ANALYSIS,
                ForensicPermission.CRIME_PATTERN_MATCHING,
                ForensicPermission.GEOGRAPHIC_TIME_INFERENCE,
                ForensicPermission.FORENSIC_REPORTS_EXPORT,
                ForensicPermission.CASE_ARCHIVE_MANAGEMENT
            ),
            createdTimestamp = System.currentTimeMillis()
        ),
        UserAccount(
            userId = "usr_analyst_2",
            username = "Analyst2",
            displayName = "تحلیل‌گر جرائم مالی (Financial Crime Analyst)",
            role = UserRole.ANALYST,
            isEnabled = true,
            requiresPasswordChange = false,
            grantedPermissions = setOf(
                ForensicPermission.BITCOIN_ANALYSIS,
                ForensicPermission.TRANSACTION_FILTERING,
                ForensicPermission.COUNTERPARTY_ANALYSIS,
                ForensicPermission.GRAPH_VISUALIZATION,
                ForensicPermission.BEHAVIORAL_ANALYSIS,
                ForensicPermission.CRIME_PATTERN_MATCHING,
                ForensicPermission.GEOGRAPHIC_TIME_INFERENCE,
                ForensicPermission.CASE_ARCHIVE_MANAGEMENT
            ),
            createdTimestamp = System.currentTimeMillis()
        ),
        UserAccount(
            userId = "usr_auditor_3",
            username = "Auditor3",
            displayName = "ناظر انطباق قانونی (Audit Officer)",
            role = UserRole.AUDITOR,
            isEnabled = true,
            requiresPasswordChange = false,
            grantedPermissions = setOf(
                ForensicPermission.TRANSACTION_FILTERING,
                ForensicPermission.COUNTERPARTY_ANALYSIS,
                ForensicPermission.FORENSIC_REPORTS_EXPORT,
                ForensicPermission.CASE_ARCHIVE_MANAGEMENT
            ),
            createdTimestamp = System.currentTimeMillis()
        )
    )

    private val userPasswordHashes = mutableMapOf<String, String>(
        "Administrator" to hashPassword("Administrator"),
        "Investigator1" to hashPassword("Investigator1"),
        "Analyst2" to hashPassword("Analyst2"),
        "Auditor3" to hashPassword("Auditor3")
    )

    private val _usersList = MutableStateFlow<List<UserAccount>>(initialUsers)
    val usersList: StateFlow<List<UserAccount>> = _usersList.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(initialUsers.first())
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(
        listOf(
            AuditLogEntry(
                id = "log_init",
                userId = "usr_admin",
                username = "Administrator",
                operation = "SYSTEM_INITIALIZATION",
                target = "ORBIT_SECURITY_SUBSYSTEM",
                details = "Orbit Forensic Security Subsystem initialized with RBAC controls."
            )
        )
    )
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    fun login(username: String, passwordAttempt: String): Result<UserAccount> {
        val user = _usersList.value.find { it.username.equals(username, ignoreCase = true) }
            ?: return Result.failure(Exception("کاربر یافت نشد / User not found"))

        if (!user.isEnabled) {
            return Result.failure(Exception("حساب کاربری غیرفعال شده است / Account disabled"))
        }

        val expectedHash = userPasswordHashes[user.username]
        val attemptHash = hashPassword(passwordAttempt)

        if (expectedHash == attemptHash) {
            val updatedUser = user.copy(lastLoginTimestamp = System.currentTimeMillis())
            _currentUser.value = updatedUser
            logAudit("USER_LOGIN_SUCCESS", user.username, "User logged in successfully.")
            return Result.success(updatedUser)
        } else {
            logAudit("USER_LOGIN_FAILED", username, "Invalid password attempt.")
            return Result.failure(Exception("رمز عبور نادرست است / Invalid password"))
        }
    }

    fun changePassword(newPassword: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No active session"))
        if (newPassword.length < 8) {
            return Result.failure(Exception("رمز عبور باید حداقل ۸ کاراکتر باشد / Password must be at least 8 characters"))
        }

        userPasswordHashes[user.username] = hashPassword(newPassword)
        val updated = user.copy(requiresPasswordChange = false)
        _currentUser.value = updated
        updateUserInList(updated)
        logAudit("PASSWORD_CHANGE", user.username, "Password changed and security flags updated.")
        return Result.success(true)
    }

    fun hasPermission(permission: ForensicPermission): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role == UserRole.ADMINISTRATOR) return true
        return user.grantedPermissions.contains(permission)
    }

    fun togglePermission(userId: String, permission: ForensicPermission, grant: Boolean) {
        val updated = _usersList.value.map { u ->
            if (u.userId == userId) {
                val perms = u.grantedPermissions.toMutableSet()
                if (grant) perms.add(permission) else perms.remove(permission)
                u.copy(grantedPermissions = perms)
            } else u
        }
        _usersList.value = updated
        if (_currentUser.value?.userId == userId) {
            _currentUser.value = updated.find { it.userId == userId }
        }
        logAudit("PERMISSION_CHANGE", userId, "Permission ${permission.name} set to $grant")
    }

    fun toggleUserEnabled(userId: String, enabled: Boolean) {
        val updated = _usersList.value.map { u ->
            if (u.userId == userId) u.copy(isEnabled = enabled) else u
        }
        _usersList.value = updated
        logAudit("USER_STATUS_CHANGE", userId, "User enabled state set to $enabled")
    }

    private fun updateUserInList(updated: UserAccount) {
        _usersList.value = _usersList.value.map { if (it.userId == updated.userId) updated else it }
    }

    fun logAudit(operation: String, target: String, details: String) {
        val current = _currentUser.value
        val entry = AuditLogEntry(
            id = "log_${System.currentTimeMillis()}_${(100..999).random()}",
            userId = current?.userId ?: "system",
            username = current?.username ?: "SYSTEM",
            operation = operation,
            target = target,
            details = details
        )
        _auditLogs.value = listOf(entry) + _auditLogs.value.take(200)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

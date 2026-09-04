package com.aistudio.orbit.security.auth

import com.aistudio.orbit.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.UUID

/**
 * Session/RBAC boundary. Production account persistence should be backed by the application's
 * encrypted account store; this manager intentionally ships with no default credentials and no
 * implicit logged-in administrator.
 */
object AuthManager {
    private val allPermissions = ForensicPermission.values().toSet()
    private data class Credential(val salt: ByteArray, val hash: ByteArray, val iterations: Int)
    private val credentials = mutableMapOf<String, Credential>()
    private val _usersList = MutableStateFlow<List<UserAccount>>(emptyList())
    val usersList: StateFlow<List<UserAccount>> = _usersList.asStateFlow()
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()
    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    fun provisionInitialAdministrator(username: String, password: String): Result<UserAccount> {
        if (_usersList.value.isNotEmpty()) return Result.failure(IllegalStateException("Initial administrator already provisioned"))
        if (!username.matches(Regex("[A-Za-z0-9._-]{3,64}"))) return Result.failure(IllegalArgumentException("Invalid administrator username"))
        validatePassword(password).getOrElse { return Result.failure(it) }
        val user = UserAccount(
            userId = "usr_${UUID.randomUUID()}", username = username, displayName = username,
            role = UserRole.ADMINISTRATOR, isEnabled = true, requiresPasswordChange = false,
            grantedPermissions = allPermissions, createdTimestamp = System.currentTimeMillis()
        )
        credentials[username] = makeCredential(password)
        _usersList.value = listOf(user)
        logAudit("INITIAL_ADMIN_PROVISIONED", username, "Administrator account provisioned explicitly by the operator.")
        return Result.success(user)
    }

    fun login(username: String, passwordAttempt: String): Result<UserAccount> {
        val user = _usersList.value.find { it.username.equals(username, ignoreCase=true) }
            ?: return Result.failure(IllegalArgumentException("کاربر یافت نشد / User not found"))
        if (!user.isEnabled) return Result.failure(IllegalStateException("حساب کاربری غیرفعال است / Account disabled"))
        val credential = credentials[user.username] ?: return Result.failure(IllegalStateException("Credential record is unavailable"))
        if (!verify(passwordAttempt, credential)) {
            logAudit("USER_LOGIN_FAILED", username, "Invalid password attempt.")
            return Result.failure(IllegalArgumentException("رمز عبور نادرست است / Invalid password"))
        }
        val updated = user.copy(lastLoginTimestamp=System.currentTimeMillis())
        _currentUser.value = updated
        _usersList.value = _usersList.value.map { if (it.userId==updated.userId) updated else it }
        logAudit("USER_LOGIN_SUCCESS", username, "User logged in successfully.")
        return Result.success(updated)
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) logAudit("USER_LOGOUT", user.username, "User logged out.")
        _currentUser.value = null
    }

    fun changePassword(newPassword: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("No active session"))
        validatePassword(newPassword).getOrElse { return Result.failure(it) }
        credentials[user.username] = makeCredential(newPassword)
        val updated = user.copy(requiresPasswordChange=false)
        _currentUser.value = updated
        _usersList.value = _usersList.value.map { if (it.userId==updated.userId) updated else it }
        logAudit("PASSWORD_CHANGE", user.username, "Password changed.")
        return Result.success(true)
    }

    fun hasPermission(permission: ForensicPermission): Boolean = _currentUser.value?.let { it.role==UserRole.ADMINISTRATOR || it.grantedPermissions.contains(permission) } ?: false

    fun togglePermission(userId:String, permission:ForensicPermission, grant:Boolean) {
        val actor=_currentUser.value ?: return
        if (actor.role!=UserRole.ADMINISTRATOR) return
        val updated=_usersList.value.map { u -> if (u.userId==userId) u.copy(grantedPermissions=(u.grantedPermissions + permission).let { if(grant) it else it-permission }) else u }
        _usersList.value=updated
        if (_currentUser.value?.userId==userId) _currentUser.value=updated.firstOrNull{it.userId==userId}
        logAudit("PERMISSION_CHANGE", userId, "Permission ${permission.name} set to $grant")
    }

    fun toggleUserEnabled(userId:String, enabled:Boolean) {
        if (_currentUser.value?.role!=UserRole.ADMINISTRATOR) return
        _usersList.value=_usersList.value.map { if(it.userId==userId) it.copy(isEnabled=enabled) else it }
        if (_currentUser.value?.userId==userId && !enabled) logout()
        logAudit("USER_STATUS_CHANGE", userId, "User enabled state set to $enabled")
    }

    fun registerUser(user:UserAccount, initialPassword:String): Result<UserAccount> {
        if (_currentUser.value?.role!=UserRole.ADMINISTRATOR) return Result.failure(SecurityException("Administrator permission required"))
        if (_usersList.value.any { it.username.equals(user.username, true) }) return Result.failure(IllegalArgumentException("Username already exists"))
        validatePassword(initialPassword).getOrElse { return Result.failure(it) }
        credentials[user.username]=makeCredential(initialPassword)
        _usersList.value=_usersList.value + user.copy(requiresPasswordChange=true)
        logAudit("USER_CREATED", user.userId, "User account created.")
        return Result.success(user)
    }

    fun logAudit(operation:String, target:String, details:String) {
        val current=_currentUser.value
        val entry=AuditLogEntry(
            id = "log_${System.currentTimeMillis()}",
            userId = current?.userId ?: "system",
            username = current?.username ?: "SYSTEM",
            operation = operation,
            target = target,
            details = details
        )
        _auditLogs.value=listOf(entry)+_auditLogs.value.take(200)
    }

    fun seedTestUsersForTestingOnly() {
        _usersList.value = listOf(
            UserAccount(
                userId = "usr_admin", username = "admin", displayName = "Admin",
                role = UserRole.ADMINISTRATOR, isEnabled = true,
                grantedPermissions = allPermissions
            ),
            UserAccount(
                userId = "usr_investigator_1", username = "investigator", displayName = "Investigator",
                role = UserRole.LEAD_INVESTIGATOR, isEnabled = true,
                grantedPermissions = setOf(ForensicPermission.BEHAVIORAL_ANALYSIS, ForensicPermission.FORENSIC_REPORTS_EXPORT)
            ),
            UserAccount(
                userId = "usr_auditor_3", username = "auditor", displayName = "Auditor",
                role = UserRole.AUDITOR, isEnabled = true,
                grantedPermissions = emptySet()
            )
        )
    }

    private fun validatePassword(password:String):Result<Unit> = if(password.length>=12 && password.any(Char::isUpperCase) && password.any(Char::isLowerCase) && password.any(Char::isDigit)) Result.success(Unit) else Result.failure(IllegalArgumentException("Password must be at least 12 characters and contain upper, lower and numeric characters"))
    private fun makeCredential(password:String):Credential { val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}; val iter=120_000; return Credential(salt, derive(password,salt,iter),iter) }
    private fun verify(password:String,c:Credential):Boolean=MessageDigest.isEqual(c.hash,derive(password,c.salt,c.iterations))
    private fun derive(password:String,salt:ByteArray,iterations:Int):ByteArray { val spec=PBEKeySpec(password.toCharArray(),salt,iterations,256); return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() } }
}

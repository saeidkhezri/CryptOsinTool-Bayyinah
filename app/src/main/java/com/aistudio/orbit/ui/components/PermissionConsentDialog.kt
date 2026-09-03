package com.aistudio.orbit.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.aistudio.orbit.repository.AppLanguage

data class AppPermissionItem(
    val id: String,
    val permissionNames: List<String>,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val icon: ImageVector,
    val isMandatory: Boolean = false,
    var isGranted: Boolean = true
)

@Composable
fun PermissionConsentDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onPermissionsConfirmed: (grantedMap: Map<String, Boolean>) -> Unit
) {
    val context = LocalContext.current
    val isPersian = language == AppLanguage.PERSIAN
    val prefs = remember { context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) }

    val initialPermissions = remember {
        val storagePerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf("android.permission.READ_MEDIA_IMAGES")
        } else {
            listOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")
        }

        val notifPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf("android.permission.POST_NOTIFICATIONS")
        } else {
            emptyList()
        }

        listOf(
            AppPermissionItem(
                id = "perm_internet",
                permissionNames = listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"),
                titleFa = "دسترسی شبکه و نودهای بلاکچین",
                titleEn = "Network & Nodes Access",
                descriptionFa = "برقراری ارتباط امن با شبکه‌های توزیع‌شده بلاکچین.",
                descriptionEn = "Secure connection to decentralized blockchain networks.",
                icon = Icons.Default.Public,
                isMandatory = true
            ),
            AppPermissionItem(
                id = "perm_storage",
                permissionNames = storagePerms,
                titleFa = "ذخیره‌سازی پرونده‌های جرم‌یابی",
                titleEn = "Forensic Dossier Storage",
                descriptionFa = "ذخیره و مدیریت گزارشات تحلیلی و ادله دیجیتال.",
                descriptionEn = "Save and manage analytical reports and digital evidence.",
                icon = Icons.Default.FolderSpecial,
                isMandatory = false
            ),
            AppPermissionItem(
                id = "perm_notifications",
                permissionNames = notifPerms,
                titleFa = "هشدارها و اعلان‌های ردیابی",
                titleEn = "Tracking Alerts & Notifications",
                descriptionFa = "اعلان کشف تراکنش‌های پرریسک در پس‌زمینه.",
                descriptionEn = "Notification of high-risk transaction detection in background.",
                icon = Icons.Default.NotificationsActive,
                isMandatory = false
            ),
            AppPermissionItem(
                id = "perm_camera",
                permissionNames = listOf("android.permission.CAMERA"),
                titleFa = "اسکنر بصری آدرس‌ها (دوربین)",
                titleEn = "Visual Address Scanner (Camera)",
                descriptionFa = "استخراج آدرس‌های ولت از کدهای QR فیزیکی.",
                descriptionEn = "Extracting wallet addresses from physical QR codes.",
                icon = Icons.Default.QrCodeScanner,
                isMandatory = false
            )
        )
    }

    val permissionStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            initialPermissions.forEach { item ->
                val isGranted = item.permissionNames.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                put(item.id, isGranted || item.isMandatory)
            }
        }
    }

    val systemLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        result.forEach { (perm, granted) ->
            initialPermissions.forEach { item ->
                if (item.permissionNames.contains(perm)) {
                    permissionStates[item.id] = granted
                    prefs.edit().putBoolean(item.id, granted).apply()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 40.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Professional Forensic Header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isPersian) "احراز صلاحیت دسترسی‌ها" else "Access Authorization",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPersian) 
                        "برای حفظ یکپارچگی تحلیل‌های جرم‌یابی و اتصال به نودهای بلاکچین، تایید موارد زیر الزامی است."
                    else 
                        "Standard forensic protocols require the following authorizations for blockchain node connectivity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Streamlined Permission List
                initialPermissions.forEach { item ->
                    val isGranted = permissionStates[item.id] ?: false
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (!isGranted && !item.isMandatory) {
                                    systemLauncher.launch(item.permissionNames.toTypedArray())
                                }
                            },
                        color = if (isGranted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isGranted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isGranted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPersian) item.titleFa else item.titleEn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isPersian) item.descriptionFa else item.descriptionEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp,
                                    fontSize = 11.sp
                                )
                            }

                            if (isGranted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Granted",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (!item.isMandatory) {
                                TextButton(
                                    onClick = { systemLauncher.launch(item.permissionNames.toTypedArray()) },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isPersian) "تایید" else "Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Mandatory marker
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Mandatory",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Unified Action Area
                Button(
                    onClick = {
                        val grantedMap = initialPermissions.associate { it.id to (permissionStates[it.id] ?: it.isMandatory) }
                        onPermissionsConfirmed(grantedMap)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isPersian) "ورود به سامانه" else "Initialize Platform",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (isPersian) "تنظیمات پیشرفته دسترسی" else "Advanced System Permissions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

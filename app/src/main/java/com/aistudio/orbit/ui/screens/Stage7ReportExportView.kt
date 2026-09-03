@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.aistudio.orbit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.orbit.forensics.export.ForensicReportOptions
import com.aistudio.orbit.forensics.filter.ForensicFilterEngine
import com.aistudio.orbit.forensics.filter.InvestigationFilterState
import com.aistudio.orbit.localization.AppLocalization
import com.aistudio.orbit.model.EvidenceCategory
import com.aistudio.orbit.model.EvidenceItem
import com.aistudio.orbit.model.InvestigationCase
import com.aistudio.orbit.repository.AppLanguage
import com.aistudio.orbit.ui.InvestigationViewModel
import com.aistudio.orbit.ui.components.InvestigationFilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stage7ReportExportView(
    viewModel: InvestigationViewModel,
    investigationCase: InvestigationCase,
    language: AppLanguage,
    onNavigatePrev: () -> Unit
) {
    val strings = AppLocalization.getStrings(language)
    val isFa = language == AppLanguage.PERSIAN
    val context = LocalContext.current

    var filterState by remember { mutableStateOf(InvestigationFilterState()) }
    var selectedEvidenceFilter by remember { mutableStateOf<EvidenceCategory?>(null) }
    var previewModalContent by remember { mutableStateOf<String?>(null) }
    var previewModalTitle by remember { mutableStateOf("") }
    var showReportOptionsDialog by remember { mutableStateOf(false) }
    var reportOptions by remember { mutableStateOf(ForensicReportOptions()) }

    val currentUser by com.aistudio.orbit.security.auth.AuthManager.currentUser.collectAsState()
    val hasPermission = currentUser?.role == com.aistudio.orbit.model.UserRole.ADMINISTRATOR || 
                        currentUser?.grantedPermissions?.contains(com.aistudio.orbit.model.ForensicPermission.FORENSIC_REPORTS_EXPORT) == true

    val filteredEvidence = remember(investigationCase.evidenceLog, selectedEvidenceFilter, filterState) {
        var list = investigationCase.evidenceLog
        if (selectedEvidenceFilter != null) {
            list = list.filter { it.category == selectedEvidenceFilter }
        }
        ForensicFilterEngine.filterEvidenceLog(list, filterState)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (!hasPermission) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 480.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (isFa) "خطای عدم دسترسی مجاز (RBAC)" else "Access Control Restriction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (isFa) "مدیر سامانه دسترسی حساب کاربری شما را به ماژول تولید و خروجی گزارش مالی غیرفعال کرده است. لطفا جهت کسب اطلاعات بیشتر با مدیر کل سیستم تماس بگیرید."
                               else "The system administrator has disabled your access to the report generation and export module. Please contact administration for authorization.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                    Button(
                        onClick = onNavigatePrev,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.prevStage)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stage Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = strings.stage7Title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.evidenceLogSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Export Actions Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.exportDossierTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedButton(
                                    onClick = { showReportOptionsDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isFa) "تنظیم محتوای گزارش" else "Configure Content",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // Notification Banner if Legal options are checked
                            if (reportOptions.hasAnyLegalSection) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                                        Text(
                                            text = if (isFa) "ضمائم حقوقی و استعلام قضایی فعال شدند و در خروجی درج خواهند شد." else "Judicial subpoena annexes enabled and included in output.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }

                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val isNarrow = maxWidth < 480.dp
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Prominent Luxury PDF Button
                                    Button(
                                        onClick = {
                                            val filename = viewModel.exportCasePdf(context, language, reportOptions)
                                            if (filename != null) {
                                                Toast.makeText(
                                                    context,
                                                    if (isFa) "گزارش مصور PDF در دانلودها ذخیره شد:\n$filename" 
                                                    else "Official PDF Dossier saved to Downloads:\n$filename",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    if (isFa) "خطا در تولید گزارش PDF" else "Failed to generate PDF report",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD32F2F)
                                        )
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isFa) "دریافت سند رسمی واکاوی جرم‌یابی با گراف (PDF)" else "Download Official Forensic Dossier (PDF)",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // 2. Row: Professional Excel (.xlsx) Multi-sheet & ISO/IEC JSON Archive
                                    if (isNarrow) {
                                        Button(
                                            onClick = {
                                                val fn = viewModel.exportCaseXlsx(context, language)
                                                if (fn != null) {
                                                    Toast.makeText(context, if (isFa) "فایل اکسل چندشیت ذخیره شد:\n$fn" else "Excel (.xlsx) saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, if (isFa) "خطا در تولید اکسل" else "Error generating Excel", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)) // Microsoft Excel Green
                                        ) {
                                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isFa) "گزارش اکسل چند شیت (Excel .xlsx)" else "Export Multi-Sheet Excel (.xlsx)", color = Color.White)
                                        }

                                        Button(
                                            onClick = {
                                                val fn = viewModel.exportCourtArchiveJson(context, language)
                                                if (fn != null) {
                                                    Toast.makeText(context, if (isFa) "آرشیو ادله قضایی (JSON) ذخیره شد:\n$fn" else "Court JSON archive saved to Downloads:\n$fn", Toast.LENGTH_SHORT).show()
                                                }
                                                val json = viewModel.generateCourtArchiveJson(language)
                                                previewModalTitle = if (isFa) "آرشیو جامع ادله قضایی (ISO/IEC 27037 JSON)" else "Court Evidence Archive (JSON)"
                                                previewModalContent = json
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                        ) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isFa) "آرشیو مهرشده ادله (ISO 27037 JSON)" else "Court Sealed JSON Archive")
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    val fn = viewModel.exportCaseXlsx(context, language)
                                                    if (fn != null) {
                                                        Toast.makeText(context, if (isFa) "فایل اکسل چندشیت ذخیره شد:\n$fn" else "Excel (.xlsx) saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, if (isFa) "خطا در تولید اکسل" else "Error generating Excel", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41))
                                            ) {
                                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isFa) "اکسل چندشیت (.xlsx)" else "Excel Dataset (.xlsx)", color = Color.White)
                                            }

                                            Button(
                                                onClick = {
                                                    val fn = viewModel.exportCourtArchiveJson(context, language)
                                                    if (fn != null) {
                                                        Toast.makeText(context, if (isFa) "آرشیو ادله قضایی ذخیره شد:\n$fn" else "Court JSON archive saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                    }
                                                    val json = viewModel.generateCourtArchiveJson(language)
                                                    previewModalTitle = if (isFa) "آرشیو جامع ادله قضایی (ISO/IEC 27037 JSON)" else "Court Evidence Archive (JSON)"
                                                    previewModalContent = json
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                            ) {
                                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isFa) "آرشیو ادله (ISO JSON)" else "Court Archive (JSON)")
                                            }
                                        }
                                    }

                                    // 3. Row: Threat Intel (MISP / OpenCTI STIX2) & GraphML XML (Gephi / Cytoscape)
                                    if (isNarrow) {
                                        Button(
                                            onClick = {
                                                val fn = viewModel.exportMispStixJson(context)
                                                if (fn != null) {
                                                    Toast.makeText(context, if (isFa) "بسته MISP/OpenCTI ذخیره شد:\n$fn" else "MISP/OpenCTI STIX2 saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                }
                                                val misp = viewModel.generateMispStixJson()
                                                previewModalTitle = "MISP / OpenCTI STIX 2.1 Threat Intel"
                                                previewModalContent = misp
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("MISP / OpenCTI STIX 2.1")
                                        }

                                        Button(
                                            onClick = {
                                                val fn = viewModel.exportGraphMl(context)
                                                if (fn != null) {
                                                    Toast.makeText(context, if (isFa) "فایل GraphML ذخیره شد:\n$fn" else "GraphML file saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                }
                                                val gml = viewModel.generateGraphMlXml()
                                                previewModalTitle = "GraphML XML (Gephi / Neo4j / Cytoscape)"
                                                previewModalContent = gml
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E342E))
                                        ) {
                                            Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("گراف شبکه (GraphML XML)")
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    val fn = viewModel.exportMispStixJson(context)
                                                    if (fn != null) {
                                                        Toast.makeText(context, if (isFa) "بسته MISP/OpenCTI ذخیره شد:\n$fn" else "MISP/OpenCTI STIX2 saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                    }
                                                    val misp = viewModel.generateMispStixJson()
                                                    previewModalTitle = "MISP / OpenCTI STIX 2.1 Threat Intel"
                                                    previewModalContent = misp
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("MISP / OpenCTI STIX")
                                            }

                                            Button(
                                                onClick = {
                                                    val fn = viewModel.exportGraphMl(context)
                                                    if (fn != null) {
                                                        Toast.makeText(context, if (isFa) "فایل GraphML ذخیره شد:\n$fn" else "GraphML file saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                    }
                                                    val gml = viewModel.generateGraphMlXml()
                                                    previewModalTitle = "GraphML XML (Gephi / Neo4j / Cytoscape)"
                                                    previewModalContent = gml
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E342E))
                                            ) {
                                                Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("GraphML (گراف شبکه)")
                                            }
                                        }
                                    }

                                    // 4. Standard TXT Dossier and CSV Dataset
                                    if (isNarrow) {
                                        Button(
                                            onClick = {
                                                val fn = viewModel.exportCaseTxt(context, language, reportOptions)
                                                if (fn != null) {
                                                    Toast.makeText(context, if (isFa) "گزارش متنی با موفقیت ذخیره شد:\n$fn" else "TXT dossier saved to Downloads:\n$fn", Toast.LENGTH_SHORT).show()
                                                }
                                                val text = viewModel.generateDossierText(language)
                                                previewModalTitle = strings.exportTxtDossier
                                                previewModalContent = text
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isFa) "ذخیره سند متنی (TXT)" else "Save TXT Dossier",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val fn = viewModel.exportCaseCsv(context)
                                                if (fn != null) {
                                                    Toast.makeText(context, if (isFa) "دیتاست CSV ذخیره شد:\n$fn" else "CSV dataset saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                }
                                                val csv = viewModel.generateDossierCsv()
                                                previewModalTitle = strings.exportCsvDataset
                                                previewModalContent = csv
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isFa) "خروجی دیتاست (CSV)" else "Save CSV Dataset",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    val fn = viewModel.exportCaseTxt(context, language, reportOptions)
                                                    if (fn != null) {
                                                        Toast.makeText(context, if (isFa) "گزارش متنی با موفقیت ذخیره شد:\n$fn" else "TXT dossier saved to Downloads:\n$fn", Toast.LENGTH_SHORT).show()
                                                    }
                                                    val text = viewModel.generateDossierText(language)
                                                    previewModalTitle = strings.exportTxtDossier
                                                    previewModalContent = text
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isFa) "سند متنی (TXT)" else "TXT Dossier",
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val fn = viewModel.exportCaseCsv(context)
                                                    if (fn != null) {
                                                        Toast.makeText(context, if (isFa) "دیتاست CSV ذخیره شد:\n$fn" else "CSV dataset saved:\n$fn", Toast.LENGTH_SHORT).show()
                                                    }
                                                    val csv = viewModel.generateDossierCsv()
                                                    previewModalTitle = strings.exportCsvDataset
                                                    previewModalContent = csv
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isFa) "دیتاست (CSV)" else "CSV Dataset",
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Epistemic Demarcation Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isFa) "استاندارد تحدید مرز معرفتی (Epistemic Demarcation)" else "Epistemic Demarcation Standard",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (isFa) "• حقایق آن‌چین (تراکنش‌ها، موجودی‌ها، زمان‌ها): قطعی و مستند به دفترکل هستند."
                                       else "• Directly Observed On-Chain Facts: Definitive and mathematically verified on-ledger.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (isFa) "• برچسب‌های OSINT و فرضیات هویتی: استنتاجی بوده و جهت سرنخ‌یابی ارائه می‌شوند."
                                       else "• OSINT Attributions & Heuristics: Probabilistic intelligence to assist legal discovery.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Filter Bar for Evidence List
                item {
                    InvestigationFilterBar(
                        filterState = filterState,
                        onFilterChange = { filterState = it },
                        language = language
                    )
                }

                // Category Filter Chips
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedEvidenceFilter == null,
                            onClick = { selectedEvidenceFilter = null },
                            label = { Text(if (isFa) "همه دسته‌ها (${investigationCase.evidenceLog.size})" else "All Categories (${investigationCase.evidenceLog.size})") }
                        )
                        EvidenceCategory.values().forEach { cat ->
                            val count = investigationCase.evidenceLog.count { it.category == cat }
                            if (count > 0) {
                                FilterChip(
                                    selected = selectedEvidenceFilter == cat,
                                    onClick = { selectedEvidenceFilter = if (selectedEvidenceFilter == cat) null else cat },
                                    label = { Text("${cat.name} ($count)") }
                                )
                            }
                        }
                    }
                }

                // Evidence Items List
                items(filteredEvidence, key = { it.id }) { item ->
                    Stage7EvidenceCard(item = item, isFa = isFa)
                }

                // Stepper Navigation
                item {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        val isNarrow = maxWidth < 380.dp
                        OutlinedButton(
                            onClick = onNavigatePrev,
                            modifier = if (isNarrow) Modifier.fillMaxWidth() else Modifier
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.prevStage, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    // Scrollable Report Options Configuration Dialog
    if (showReportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showReportOptionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (isFa) "تنظیم ساختار و محتوای گزارش" else "Configure Report Structure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isFa) "بخش‌های مورد نظر خود را برای درج در فایل‌های PDF و TXT انتخاب فرمایید:"
                               else "Select the specific sections to include in your exported dossier:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Group 1: Technical & Forensic (Defaults ON)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isFa) "۱. داده‌های فنی و فارنزیک (پیش‌فرض)" else "1. Technical & Forensic Core",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            ReportCheckboxRow(
                                label = if (isFa) "مشخصات پرونده و آدرس هدف" else "Case Identification & Target Address",
                                checked = reportOptions.includeSummary,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeSummary = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "حقایق قطعی دفترکل (موجودی، دریافتی، ارسالی)" else "Directly Observed On-Chain Facts",
                                checked = reportOptions.includeLedgerFacts,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeLedgerFacts = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "جدول تراکنش‌های طرفین" else "Transaction Ledger Table",
                                checked = reportOptions.includeTransactions,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeTransactions = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "تصویر نمودار توپولوژی گراف (Image)" else "Topology Graph Diagram (High-Res Image)",
                                checked = reportOptions.includeGraphDiagram,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeGraphDiagram = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "جدول گره‌ها و خوشه‌بندی GraphSense" else "Discovered Graph Nodes & Entities",
                                checked = reportOptions.includeGraphTopology,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeGraphTopology = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "شناسه‌های هویتی OSINT و تبار شواهد" else "OSINT Identifiers & Evidence Logs",
                                checked = reportOptions.includeOsintCrossIdentities,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeOsintCrossIdentities = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "مهر اصالت و هش دیجیتال SHA-256" else "ISO/IEC 27037 SHA-256 Integrity Seal",
                                checked = reportOptions.includeEvidenceSealSha256,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeEvidenceSealSha256 = it) }
                            )
                        }
                    }

                    // Group 1.5: AI Copilot Executive Summary
                    val aiDraft = viewModel.aiDraftedSummary.collectAsState().value
                    val aiConfigs by viewModel.aiSettingsRepo.configs.collectAsState()
                    val hasAiConfig = aiConfigs.values.any { it.enabled } && aiConfigs.values.any { viewModel.aiSettingsRepo.getApiKey(it.providerType).isNotBlank() }
                    
                    if (hasAiConfig) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isFa) "تحلیل دستیار هوشمند (AI Copilot)" else "AI Copilot Analysis",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                if (aiDraft != null) {
                                    ReportCheckboxRow(
                                        label = if (isFa) "درج خلاصه تحلیل صادر شده توسط هوش مصنوعی" else "Include AI Generated Executive Summary",
                                        checked = reportOptions.aiCopilotSummary != null,
                                        onCheckedChange = { 
                                            reportOptions = reportOptions.copy(aiCopilotSummary = if (it) aiDraft else null) 
                                        }
                                    )
                                    // Make sure it's linked initially if it's not set but draft is available
                                    LaunchedEffect(aiDraft) {
                                        if (reportOptions.aiCopilotSummary == null) {
                                            reportOptions = reportOptions.copy(aiCopilotSummary = aiDraft)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.generateAiDraftedSummary(investigationCase) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFa) "تولید خلاصه گزارش با هوش مصنوعی" else "Generate AI Executive Summary")
                                    }
                                }
                            }
                        }
                    }

                    // Group 2: Legal & Judicial (Defaults OFF - Optional)
                    Surface(
                        color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isFa) "۲. ضمائم حقوقی و استعلامات قضایی (اختیاری)" else "2. Judicial & Subpoena Annexes (Optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )

                            ReportCheckboxRow(
                                label = if (isFa) "پیش‌نویس استعلام و دستور قضایی (Subpoena)" else "Judicial Subpoena & KYC Disclosure Order",
                                checked = reportOptions.includeExchangeSubpoena,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeExchangeSubpoena = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "فرم تقاضای مسدودی فوری حساب در صرافی" else "VASP Asset Freeze Request Form",
                                checked = reportOptions.includeJudiciaryRequest,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeJudiciaryRequest = it) }
                            )
                            ReportCheckboxRow(
                                label = if (isFa) "چارچوب استنادپذیری دادگاهی ادله دیجیتال" else "Court Admissibility Certification Annex",
                                checked = reportOptions.includeCourtAdmissibilityFramework,
                                onCheckedChange = { reportOptions = reportOptions.copy(includeCourtAdmissibilityFramework = it) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showReportOptionsDialog = false }) {
                    Text(if (isFa) "تایید و اعمال" else "Apply Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Reset to default
                    reportOptions = ForensicReportOptions()
                }) {
                    Text(if (isFa) "بازنشانی پیش‌فرض" else "Reset Defaults")
                }
            }
        )
    }

    // Scrollable Export Preview Modal
    if (previewModalContent != null) {
        val content = previewModalContent!!
        AlertDialog(
            onDismissRequest = { previewModalContent = null },
            title = {
                Text(text = previewModalTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Forensic Report", content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, strings.copiedToClipboard, Toast.LENGTH_SHORT).show()
                        previewModalContent = null
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFa) "کپی در کلیپ‌بورد" else "Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewModalContent = null }) {
                    Text(strings.close)
                }
            }
        )
    }
}

@Composable
private fun ReportCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun Stage7EvidenceCard(item: EvidenceItem, isFa: Boolean) {
    var expandedProvenance by remember { mutableStateOf(false) }

    val (categoryColor, categoryLabel) = when (item.category) {
        EvidenceCategory.OBSERVED_ON_CHAIN -> Pair(Color(0xFF2E7D32), if (isFa) "حقیقت قطعی دفترکل (Direct Fact)" else "Directly Observed Fact")
        EvidenceCategory.ALGORITHMIC_RESULT -> Pair(Color(0xFF1565C0), if (isFa) "محاسبه الگوریتمی (Calculation)" else "Algorithmic Calculation")
        EvidenceCategory.ATTRIBUTION, EvidenceCategory.OSINT_INTELLIGENCE -> Pair(Color(0xFF6A1B9A), if (isFa) "انتساب هویتی (Attribution)" else "Entity Attribution")
        EvidenceCategory.SANCTIONS_MATCH -> Pair(Color(0xFFB71C1C), if (isFa) "تطابق تحریم‌ها (Sanctions Screening)" else "Sanctions Screening")
        EvidenceCategory.BEHAVIORAL_PATTERN -> Pair(Color(0xFFC62828), if (isFa) "الگوی رفتاری و ریسک (Pattern Match)" else "Behavioral Pattern")
        EvidenceCategory.TEMPORAL_ANALYSIS -> Pair(Color(0xFFEF6C00), if (isFa) "تحلیل زمانی (Temporal Analysis)" else "Temporal Analysis")
        EvidenceCategory.AI_INFERENCE -> Pair(Color(0xFF00838F), if (isFa) "استنتاج هوش مصنوعی (AI Inference)" else "AI-Assisted Inference")
        EvidenceCategory.INVESTIGATOR_CONCLUSION -> Pair(Color(0xFF455A64), if (isFa) "یادداشت کارشناس (Investigator Note)" else "Investigator Assessment")
        EvidenceCategory.EXTERNAL_SOURCE -> Pair(Color(0xFF5E35B1), if (isFa) "منبع خارجی (External Source)" else "External Source")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        color = categoryColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = if (isFa) item.confidence.displayNameFa else item.confidence.displayNameEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = item.localizedTitle(isFa),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = item.localizedDescription(isFa),
                style = MaterialTheme.typography.bodyMedium
            )

            // Provenance & Source summary
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isFa) "منبع: ${item.provenance.sourceName} | وضعیت: ${item.verificationStatus.displayNameFa}" else "Source: ${item.provenance.sourceName} | Status: ${item.verificationStatus.displayNameEn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = { expandedProvenance = !expandedProvenance },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (expandedProvenance) (if (isFa) "بستن زنجیره اصل ادله" else "Hide Provenance") else (if (isFa) "زنجیره اصل ادله (Provenance)" else "View Provenance"),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            AnimatedVisibility(visible = expandedProvenance) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isFa) "ردپای زنجیره اصل ادله (Forensic Provenance Trail):" else "Forensic Provenance & Audit Trail:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFa) "• نوع منبع داده: ${item.provenance.sourceType.displayNameFa}" else "• Data Source Type: ${item.provenance.sourceType.displayNameEn}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (item.provenance.transformationPipeline.isNotEmpty()) {
                            Text(
                                text = if (isFa) "• خط لوله پردازش: ${item.provenance.transformationPipeline.joinToString(" ➔ ")}" else "• Pipeline: ${item.provenance.transformationPipeline.joinToString(" ➔ ")}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = if (isFa) "• ارزیابی قطعیت: ${item.confidence.displayNameFa} (وزن ادله: ${String.format("%.2f", item.confidence.weight)})" else "• Evidentiary Weight: ${String.format("%.2f", item.confidence.weight)} (${item.confidence.displayNameEn})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

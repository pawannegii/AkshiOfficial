package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import com.example.model.OnionResult
import com.example.ui.components.AppleButton
import com.example.ui.components.AppleCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.RottenError
import com.example.ui.theme.StoneBackground
import com.example.ui.theme.StoneOutline
import com.example.ui.theme.StoneSurface
import com.example.ui.theme.StoneSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    batchId: String,
    result: OnionResult,
    location: String,
    inspectorName: String,
    onSaveReport: () -> Unit,
    onScanAgain: () -> Unit,
    onNavigateBack: () -> Unit,
    isSaved: Boolean,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onDeleteReport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDate = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Inspection Report?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete report for batch $batchId?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteReport?.invoke()
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    fun shareReport() {
        coroutineScope.launch {
            try {
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "$batchId.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Report"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StoneBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("report_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Text(
                        text = "Digital Report",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("favorite_report_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) androidx.compose.material.icons.Icons.Filled.Favorite else androidx.compose.material.icons.Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite Report",
                                tint = if (isFavorite) androidx.compose.ui.graphics.Color.Red else TextPrimary
                            )
                        }
                        if (onDeleteReport != null && isSaved) {
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.testTag("delete_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Report",
                                    tint = TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { shareReport() },
                            modifier = Modifier.testTag("share_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.IosShare,
                                contentDescription = "Share Report",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            // Digital Report Card
            item {
                AppleCard(
                    modifier = Modifier.fillMaxWidth().drawWithCache { onDrawWithContent { graphicsLayer.record { this@onDrawWithContent.drawContent() }; drawLayer(graphicsLayer) } },
                    cornerRadius = 24.dp
                ) {
                    Column {
                        // Official Certificate Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.06f)
                                ) {
                                    Text(
                                        text = "MINISTRY OF CONSUMER AFFAIRS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Onion Quality Certificate",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Smart India Hackathon 2026 · PS 26031",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }

                            IconButton(
                                onClick = { showQrDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StoneSurfaceVariant)
                                    .testTag("open_qr_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode,
                                    contentDescription = "Verification QR",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = StoneOutline, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Metadata Grid
                        ReportRow(label = "Batch Identification", value = batchId, isBold = true)
                        ReportRow(label = "Inspection Timestamp", value = currentDate)
                        ReportRow(label = "Procurement Mandi", value = location)
                        ReportRow(label = "Certified Inspector", value = inspectorName)

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = StoneOutline, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Grade summary metrics
                        ReportRow(
                            label = "Grade A Ratio (Good)",
                            value = "${String.format("%.1f", result.gradeAPercentage)}%",
                            valueColor = TextPrimary,
                            isBold = true
                        )
                        ReportRow(
                            label = "URS Ratio (Bad)",
                            value = "${String.format("%.1f", result.ursPercentage)}%",
                            valueColor = TextPrimary,
                            isBold = true
                        )
                        ReportRow(label = "Total Bulb Count", value = "${result.totalOnionsDetected} bulbs")
                        ReportRow(label = "Good Bulbs Count", value = "${result.goodCount} bulbs")
                        ReportRow(label = "Bad Bulbs Count", value = "${result.badCount} bulbs")

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Inspector Remarks: ${result.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Actions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Save to Room DB button
                    Surface(
                        onClick = {
                            onSaveReport()
                            Toast.makeText(context, "Report saved to local records", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSaved) TextPrimary else Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .border(1.dp, if (isSaved) TextPrimary else StoneOutline, RoundedCornerShape(16.dp))
                            .testTag("save_report_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Outlined.CheckCircle else Icons.Outlined.Save,
                                contentDescription = "Save",
                                tint = if (isSaved) Color.White else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSaved) "Saved" else "Save Record",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isSaved) Color.White else TextPrimary
                            )
                        }
                    }

                    // Share Button
                    Surface(
                        onClick = { shareReport() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .border(1.dp, StoneOutline, RoundedCornerShape(16.dp))
                            .testTag("share_summary_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.IosShare,
                                contentDescription = "Share",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Primary Scan Again CTA
            item {
                AppleButton(
                    text = "Scan Again",
                    onClick = onScanAgain,
                    testTag = "scan_again_button"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showQrDialog) {
            QrInspectionDialog(
                result = result,
                batchId = batchId,
                dateFormatted = currentDate,
                location = location,
                inspectorName = inspectorName,
                onDismiss = { showQrDialog = false }
            )
        }
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(0.48f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.52f)
        )
    }
}

@Composable
private fun QrInspectionDialog(
    result: OnionResult,
    batchId: String,
    dateFormatted: String,
    location: String,
    inspectorName: String,
    onDismiss: () -> Unit
) {
    val qrPayload = remember(result, batchId, dateFormatted, location, inspectorName) {
        buildString {
            appendLine("=== AKSHI QUALITY CERTIFICATE ===")
            appendLine("Batch: $batchId")
            appendLine("Date: $dateFormatted")
            appendLine("Mandi: $location")
            appendLine("Inspector: $inspectorName")
            appendLine("---")
            appendLine("Status: ${result.qualityClassification.uppercase()}")
            appendLine("Grade A: ${"%.1f".format(result.gradeAPercentage)}% | URS: ${"%.1f".format(result.ursPercentage)}%")
            appendLine("Total Bulbs: ${result.totalOnionsDetected}")
            appendLine("---")
            appendLine("Defects: Damaged(${result.defectBreakdown.damaged}), Rotten(${result.defectBreakdown.rotten}), Sprouted(${result.defectBreakdown.sprouted}), Undersized(${result.defectBreakdown.undersized})")
            appendLine("VERIFIED OFFICIAL BUFFER RECORD")
        }
    }

    val qrBitmap = remember(qrPayload) {
        generateQrBitmap(qrPayload, 600)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss)
                .testTag("qr_dialog_scrim"),
            contentAlignment = Alignment.Center
        ) {
            AppleCard(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.90f)
                    .clickable(enabled = false) {}, // Prevent dismiss on card click
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Batch Verification QR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StoneSurfaceVariant)
                                .testTag("close_qr_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close QR dialog",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Generated QR Code for batch $batchId",
                                modifier = Modifier.size(230.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = batchId,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Scan with any QR scanner to view complete verified certificate data and defect breakdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    } catch (e: Exception) {
        null
    }
}

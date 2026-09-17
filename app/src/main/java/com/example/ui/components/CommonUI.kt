package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.InspectionReportEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AkshiLogo(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF111111),
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val radius = w * 0.36f
        val centerX = w * 0.5f
        val centerY = h * 0.54f

        // Onion bulb circle
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokePx)
        )

        // Equator horizontal divider line
        drawLine(
            color = color,
            start = Offset(centerX - radius, centerY),
            end = Offset(centerX + radius, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // Top stem / sprout loop
        val sproutWidth = radius * 0.30f
        val sproutTop = h * 0.12f
        val sproutBottom = centerY - radius + (strokePx * 0.5f)

        val sproutPath = Path().apply {
            moveTo(centerX - sproutWidth, sproutBottom)
            lineTo(centerX - sproutWidth, sproutTop + sproutWidth)
            arcTo(
                rect = Rect(
                    left = centerX - sproutWidth,
                    top = sproutTop,
                    right = centerX + sproutWidth,
                    bottom = sproutTop + (sproutWidth * 2)
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(centerX + sproutWidth, sproutBottom)
        }

        drawPath(
            path = sproutPath,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.85f)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(TextPrimary),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                BottomNavItem(
                    icon = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    isSelected = currentRoute == "home",
                    onClick = { onNavigate("home") }
                )
                
                // Dashboard
                BottomNavItem(
                    icon = Icons.Outlined.Dashboard,
                    isSelected = currentRoute == "dashboard",
                    onClick = { onNavigate("dashboard") }
                )
                
                // Favourites
                BottomNavItem(
                    icon = if (currentRoute == "favourites") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    isSelected = currentRoute == "favourites",
                    onClick = { onNavigate("favourites") }
                )
                
                // Profile
                BottomNavItem(
                    icon = Icons.Outlined.Person,
                    isSelected = currentRoute == "profile",
                    onClick = { onNavigate("profile") }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) TextPrimary else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(if (isSelected) 24.dp else 28.dp)
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StoneSurface,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search reports...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary
                        )
                    }
                    innerTextField()
                }
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TextPrimary)
                    .clickable { onFilterClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ReportCard(
    report: InspectionReportEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StoneSurface,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StoneSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!report.imageUri.isNullOrEmpty()) {
                    val context = LocalContext.current
                    val imageRequest = remember(report.imageUri, report.samplePresetType) {
                        ImageRequest.Builder(context)
                            .data(report.imageUri)
                            .size(216, 216)
                            .crossfade(true)
                            .error(if (report.samplePresetType == "MIXED") R.drawable.img_sample_mixed else R.drawable.img_sample_grade_a)
                            .fallback(if (report.samplePresetType == "MIXED") R.drawable.img_sample_mixed else R.drawable.img_sample_grade_a)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Batch ${report.batchId} thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (report.samplePresetType == "MIXED") {
                    Image(
                        painter = painterResource(id = R.drawable.img_sample_mixed),
                        contentDescription = "Batch ${report.batchId} thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_sample_grade_a),
                        contentDescription = "Batch ${report.batchId} thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.batchId,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onToggleFavorite != null) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (report.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (report.isFavorite) Color.Red else TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (report.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Report",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${report.totalOnions} onions • ${String.format("%.1f", report.gradeAPercentage)}% Grade A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(report.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
fun InspectionCard(
    report: InspectionReportEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Actual image thumbnail
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                if (!report.imageUri.isNullOrEmpty()) {
                    val context = LocalContext.current
                    val imageRequest = remember(report.imageUri, report.samplePresetType) {
                        ImageRequest.Builder(context)
                            .data(report.imageUri)
                            .size(228, 228)
                            .crossfade(true)
                            .error(if (report.samplePresetType == "MIXED") R.drawable.img_sample_mixed else R.drawable.img_sample_grade_a)
                            .fallback(if (report.samplePresetType == "MIXED") R.drawable.img_sample_mixed else R.drawable.img_sample_grade_a)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Batch ${report.batchId} thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (report.samplePresetType == "MIXED") {
                    Image(
                        painter = painterResource(id = R.drawable.img_sample_mixed),
                        contentDescription = "Batch ${report.batchId} thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_sample_grade_a),
                        contentDescription = "Batch ${report.batchId} thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top row: Batch ID & Star favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.batchId,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (report.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (report.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (report.isFavorite) Color(0xFFEAB308) else Color(0xFF111111),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Date row: Oct 14, 2:30 PM
                Text(
                    text = dateFormat.format(Date(report.timestamp)),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp
                    ),
                    color = Color(0xFF71717A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metrics row: Grade A, URS, Total bulbs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Grade A",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "${Math.round(report.gradeAPercentage)}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF16A34A)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Column {
                        Text(
                            text = "URS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "${Math.round(report.ursPercentage)}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total ${report.totalOnions}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF71717A)
                        )
                        Text(
                            text = "bulbs",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF71717A)
                        )
                    }
                }
            }
        }
    }
}

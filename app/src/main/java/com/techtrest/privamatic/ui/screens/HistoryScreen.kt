package com.techtrest.privamatic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.HistoryFilter
import com.techtrest.privamatic.data.model.CheckDeduction
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    snapshots: List<PrivacySnapshot>,
    selectedFilter: HistoryFilter,
    onFilterChanged: (HistoryFilter) -> Unit,
    onClearHistory: () -> Unit,
    onBackClick: () -> Unit,
    onLoadHistory: () -> Unit
) {
    var selectedSnapshot by remember(snapshots) { mutableStateOf<PrivacySnapshot?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadHistory()
    }

    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.label_history_screen_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.label_common_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.label_history_clear)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
        val timestampFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }

        val displaySnapshot = selectedSnapshot
        val deductions = remember(displaySnapshot) {
            displaySnapshot?.let { CheckDeduction.decode(it.deductionsJson) } ?: emptyList()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter strip
            item {
                HistoryFilterStrip(
                    selectedFilter = selectedFilter,
                    onFilterChanged = { filter ->
                        selectedSnapshot = null
                        onFilterChanged(filter)
                    }
                )
            }

            // Date range label
            if (snapshots.isNotEmpty()) {
                item {
                    val first = dateFormat.format(Date(snapshots.first().timestamp))
                    val last = dateFormat.format(Date(snapshots.last().timestamp))
                    Text(
                        text = if (snapshots.size == 1) first else "$first \u2013 $last",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chart
            item {
                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.label_history_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    ScoreLineChart(
                        snapshots = snapshots,
                        selectedSnapshot = selectedSnapshot,
                        onSnapshotSelected = { selectedSnapshot = it }
                    )
                }
            }

            // X-axis date labels (only when chart is visible)
            if (snapshots.size >= 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateFormat.format(Date(snapshots.first().timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(snapshots.last().timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Selected point info
            item {
                if (displaySnapshot != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.label_history_selected_score, displaySnapshot.score),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = timestampFormat.format(Date(displaySnapshot.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.label_history_tap_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { HorizontalDivider() }

            // Deductions list
            if (deductions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.copy_history_empty_deductions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(deductions, key = { it.checkName }) { deduction ->
                    HistoryDeductionRow(deduction = deduction)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.label_history_clear)) },
            text = { Text(stringResource(R.string.label_history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showClearConfirm = false
                }) {
                    Text(stringResource(R.string.label_history_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.label_common_cancel))
                }
            }
        )
    }
}

@Composable
private fun HistoryFilterStrip(
    selectedFilter: HistoryFilter,
    onFilterChanged: (HistoryFilter) -> Unit
) {
    val filters = listOf(
        HistoryFilter.WEEK to R.string.label_history_filter_week,
        HistoryFilter.MONTH to R.string.label_history_filter_month,
        HistoryFilter.THREE_MONTHS to R.string.label_history_filter_three_months,
        HistoryFilter.SIX_MONTHS to R.string.label_history_filter_six_months,
        HistoryFilter.YEAR to R.string.label_history_filter_year,
        HistoryFilter.ALL to R.string.label_history_filter_all,
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, labelRes) ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterChanged(filter) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }
}

@Composable
private fun ScoreLineChart(
    snapshots: List<PrivacySnapshot>,
    selectedSnapshot: PrivacySnapshot?,
    onSnapshotSelected: (PrivacySnapshot?) -> Unit
) {
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.secondary

    val scores = snapshots.map { it.score }
    val rawMax = scores.max() + 5
    val rawMin = scores.min() - 5
    var yMax = rawMax
    var yMin = rawMin
    if (yMax - yMin < 20) {
        val mid = (yMax + yMin) / 2
        yMax = mid + 10
        yMin = mid - 10
    }
    yMax = minOf(100, yMax)
    yMin = maxOf(0, yMin)
    val scoreRange = (yMax - yMin).toFloat().coerceAtLeast(1f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { 280.dp.toPx() }
        val paddingPx = with(density) { 24.dp.toPx() }
        val dotRadiusPx = with(density) { 4.dp.toPx() }
        val selectedRadiusPx = with(density) { 6.dp.toPx() }
        val touchRadiusPx = with(density) { 24.dp.toPx() }
        val strokeWidthPx = with(density) { 2.dp.toPx() }

        val yAxisLabelWidthPx = with(density) { 28.dp.toPx() }
        val chartLeft = yAxisLabelWidthPx
        val chartRight = canvasWidthPx - paddingPx
        val xAxisLabelHeightPx = with(density) { 20.dp.toPx() }
        val chartBottom = canvasHeightPx - paddingPx - xAxisLabelHeightPx
        val chartTop = with(density) { 32.dp.toPx() }
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        val n = snapshots.size
        val pointPositions: List<Offset> = remember(snapshots, canvasWidthPx, canvasHeightPx) {
            snapshots.mapIndexed { i, snapshot ->
                val x = if (n <= 1) chartLeft + chartW / 2f
                         else chartLeft + (i.toFloat() / (n - 1)) * chartW
                val y = chartBottom - ((snapshot.score - yMin) / scoreRange) * chartH
                Offset(x, y)
            }
        }

        val textPaint = remember {
            android.graphics.Paint().apply {
                textSize = with(density) { 10.sp.toPx() }
                color = android.graphics.Color.GRAY
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pointPositions) {
                    detectTapGestures { tapOffset ->
                        val nearest = pointPositions
                            .mapIndexed { idx, pos ->
                                val dx = tapOffset.x - pos.x
                                val dy = tapOffset.y - pos.y
                                idx to sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            }
                            .filter { (_, dist) -> dist <= touchRadiusPx }
                            .minByOrNull { (_, dist) -> dist }
                        onSnapshotSelected(nearest?.let { snapshots[it.first] })
                    }
                }
        ) {
            // Grid lines at every 10-point interval within the y range
            var gridScore = (yMin / 10) * 10
            while (gridScore <= yMax) {
                val y = chartBottom - ((gridScore - yMin) / scoreRange) * chartH
                if (y >= chartTop && y <= chartBottom) {
                    drawLine(
                        color = gridColor,
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = strokeWidthPx / 2f
                    )
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            gridScore.toString(),
                            chartLeft - with(density) { 6.dp.toPx() },
                            y + textPaint.textSize / 3f,
                            textPaint
                        )
                    }
                }
                gridScore += 10
            }

            // Connect data points with a line
            if (pointPositions.size >= 2) {
                val path = Path()
                path.moveTo(pointPositions[0].x, pointPositions[0].y)
                for (i in 1 until pointPositions.size) {
                    path.lineTo(pointPositions[i].x, pointPositions[i].y)
                }
                drawPath(path, primaryColor, style = Stroke(strokeWidthPx, cap = StrokeCap.Round))
            }

            // Draw data point circles
            for (i in snapshots.indices) {
                val pos = pointPositions[i]
                val isSelected = snapshots[i] == selectedSnapshot
                drawCircle(
                    color = if (isSelected) selectedColor else primaryColor,
                    radius = if (isSelected) selectedRadiusPx else dotRadiusPx,
                    center = pos
                )
            }
        }
    }
}

@Composable
private fun HistoryDeductionRow(deduction: CheckDeduction) {
    val context = LocalContext.current
    val displayName = remember(deduction.checkName) {
        try {
            context.getString(PrivacyCheck.valueOf(deduction.checkName).displayName)
        } catch (_: IllegalArgumentException) {
            deduction.checkName
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.error,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "-${deduction.points}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

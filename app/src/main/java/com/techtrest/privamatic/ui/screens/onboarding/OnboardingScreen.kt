package com.techtrest.privamatic.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.BritishRacingGreen
import com.techtrest.privamatic.Cream
import com.techtrest.privamatic.R
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3
private const val DOT_SELECTED_SIZE_DP = 10
private const val DOT_UNSELECTED_SIZE_DP = 6
private const val DOT_ANIMATION_DURATION_MS = 200

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BritishRacingGreen)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Skip row — fixed height so the pager doesn't shift when skip disappears
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = stringResource(R.string.label_onboarding_skip),
                            color = Cream.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Pager fills remaining vertical space
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> WelcomeSlide()
                    1 -> HowItWorksSlide()
                    else -> PragmaticSlide()
                }
            }

            // Bottom controls: dots + CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    pageCount = PAGE_COUNT,
                    currentPage = pagerState.currentPage
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isLastPage) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cream,
                            contentColor = BritishRacingGreen
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.label_onboarding_get_started),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.label_onboarding_next),
                            color = Cream,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Cream,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = currentPage == index
            val dotSize by animateDpAsState(
                targetValue = if (isSelected) DOT_SELECTED_SIZE_DP.dp else DOT_UNSELECTED_SIZE_DP.dp,
                animationSpec = tween(DOT_ANIMATION_DURATION_MS),
                label = "dot_size_$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Cream else Cream.copy(alpha = 0.35f)
                    )
            )
        }
    }
}

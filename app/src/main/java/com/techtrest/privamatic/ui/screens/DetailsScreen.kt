package com.techtrest.privamatic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.data.model.PrivacyCategory
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.ui.components.CategoryGroup

@Composable
fun DetailsScreen(
    privacyScore: PrivacyScore,
    modifier: Modifier = Modifier
) {
    val securityCategories = listOf(
        PrivacyCategory.SYSTEM_SECURITY
    )
    
    val surveillanceCategories = listOf(
        PrivacyCategory.NETWORK_PRIVACY,
        PrivacyCategory.GOOGLE_SERVICES,
        PrivacyCategory.DEFAULT_APPS,
        PrivacyCategory.GOOGLE_APPS,
        PrivacyCategory.META_FACEBOOK_APPS,
        PrivacyCategory.MICROSOFT_APPS,
        PrivacyCategory.AI_AND_OTHER_APPS
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "SECURITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        
        items(securityCategories, key = { it.name }) { category ->
            val statusColor = getCategoryStatusColor(category, privacyScore)
            CategoryGroup(
                category = category,
                privacyScore = privacyScore,
                statusColor = statusColor
            )
        }
        
        item {
            Text(
                text = "SURVEILLANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
        
        items(surveillanceCategories, key = { it.name }) { category ->
            val statusColor = getCategoryStatusColor(category, privacyScore)
            CategoryGroup(
                category = category,
                privacyScore = privacyScore,
                statusColor = statusColor
            )
        }
    }
}

@Composable
private fun getCategoryStatusColor(category: PrivacyCategory, privacyScore: PrivacyScore): Color {
    val (issuesCount, _) = PrivacyCategory.getIssuesCount(category, privacyScore)
    return if (issuesCount > 0) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
}

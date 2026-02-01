package com.techtrest.privacywidget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.model.PrivacyCategory
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.ui.components.CategoryGroup
import com.techtrest.privacywidget.ui.navigation.DetailsSubTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    privacyScore: PrivacyScore,
    selectedSubTab: DetailsSubTab,
    onSubTabSelected: (DetailsSubTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "Privacy Details",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sub-Tab Selector - SegmentedButton
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedSubTab == DetailsSubTab.SURVEILLANCE,
                onClick = { onSubTabSelected(DetailsSubTab.SURVEILLANCE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(
                        imageVector = DetailsSubTab.SURVEILLANCE.icon,
                        contentDescription = null
                    )
                }
            ) {
                Text("Surveillance")
            }
            SegmentedButton(
                selected = selectedSubTab == DetailsSubTab.SECURITY,
                onClick = { onSubTabSelected(DetailsSubTab.SECURITY) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        imageVector = DetailsSubTab.SECURITY.icon,
                        contentDescription = null
                    )
                }
            ) {
                Text("Security")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filtered Category Groups based on selected sub-tab
        when (selectedSubTab) {
            DetailsSubTab.SURVEILLANCE -> {
                // Show all categories EXCEPT SYSTEM_SECURITY
                val surveillanceCategories = listOf(
                    PrivacyCategory.NETWORK_PRIVACY,
                    PrivacyCategory.GOOGLE_SERVICES,
                    PrivacyCategory.DEFAULT_APPS,
                    PrivacyCategory.GOOGLE_APPS,
                    PrivacyCategory.META_FACEBOOK_APPS,
                    PrivacyCategory.MICROSOFT_APPS,
                    PrivacyCategory.AI_AND_OTHER_APPS
                )
                surveillanceCategories.forEach { category ->
                    val issues = PrivacyCategory.getIssuesForCategory(category, privacyScore)
                    if (issues.isNotEmpty()) {
                        CategoryGroup(category = category, privacyScore = privacyScore)
                    }
                }
            }
            DetailsSubTab.SECURITY -> {
                // Show only SYSTEM_SECURITY category
                val securityIssues = PrivacyCategory.getIssuesForCategory(
                    PrivacyCategory.SYSTEM_SECURITY,
                    privacyScore
                )
                if (securityIssues.isNotEmpty()) {
                    CategoryGroup(
                        category = PrivacyCategory.SYSTEM_SECURITY,
                        privacyScore = privacyScore
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

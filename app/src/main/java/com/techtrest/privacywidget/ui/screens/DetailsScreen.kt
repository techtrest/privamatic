package com.techtrest.privacywidget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val filteredCategories = remember(selectedSubTab, privacyScore) {
        val categories = when (selectedSubTab) {
            DetailsSubTab.SURVEILLANCE -> listOf(
                PrivacyCategory.NETWORK_PRIVACY,
                PrivacyCategory.GOOGLE_SERVICES,
                PrivacyCategory.DEFAULT_APPS,
                PrivacyCategory.GOOGLE_APPS,
                PrivacyCategory.META_FACEBOOK_APPS,
                PrivacyCategory.MICROSOFT_APPS,
                PrivacyCategory.AI_AND_OTHER_APPS
            )
            DetailsSubTab.SECURITY -> listOf(
                PrivacyCategory.SYSTEM_SECURITY
            )
        }
        categories.filter { category ->
            PrivacyCategory.getIssuesForCategory(category, privacyScore).isNotEmpty()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Segmented tab switcher
        item {
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
        }

        // Extra spacing between switcher and first card
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Category groups
        items(filteredCategories, key = { it.name }) { category ->
            CategoryGroup(category = category, privacyScore = privacyScore)
        }
    }
}

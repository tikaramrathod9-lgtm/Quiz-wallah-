package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.SubjectColors

data class SubjectTabItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

val STUDY_SUBJECT_TABS = listOf(
    SubjectTabItem("All", "All Subjects", Icons.Default.Book),
    SubjectTabItem("Math", "Math", Icons.Default.Calculate),
    SubjectTabItem("Physics", "Physics", Icons.Default.Speed),
    SubjectTabItem("Chemistry", "Chemistry", Icons.Default.Science),
    SubjectTabItem("Hindi", "Hindi", Icons.Default.Translate),
    SubjectTabItem("English", "English", Icons.Default.Language),
    SubjectTabItem("Saved", "Saved Links", Icons.Default.Bookmark)
)

@Composable
fun SubjectTabBar(
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        STUDY_SUBJECT_TABS.forEach { tab ->
            val isSelected = selectedSubject.equals(tab.id, ignoreCase = true)
            val subjectColor = when (tab.id) {
                "All" -> MaterialTheme.colorScheme.primary
                "Saved" -> AmberGold
                else -> SubjectColors.colorForSubject(tab.id)
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSubjectSelected(tab.id) },
                label = { Text(tab.title) },
                leadingIcon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else subjectColor
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = subjectColor,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("subject_tab_${tab.id.lowercase()}")
            )
        }
    }
}

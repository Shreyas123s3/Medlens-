package com.medlens.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medlens.app.ui.components.CategoryChip
import com.medlens.app.ui.components.ChatBubble
import com.medlens.app.ui.components.EmptyState
import com.medlens.app.ui.components.EmergencyCard
import com.medlens.app.ui.components.ErrorState
import com.medlens.app.ui.components.FeatureCard
import com.medlens.app.ui.components.InfoSection
import com.medlens.app.ui.components.MascotExpression
import com.medlens.app.ui.components.MedLensMascot
import com.medlens.app.ui.components.MedLensTopBar
import com.medlens.app.ui.components.MedicineCard
import com.medlens.app.ui.components.SearchBar
import com.medlens.app.ui.components.SectionHeader
import com.medlens.app.ui.components.ShimmerPlaceholder
import com.medlens.app.ui.components.TypingIndicator
import com.medlens.app.ui.components.WarningBanner

@Composable
fun DesignPreviewScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MedLensTopBar(onLogoClick = {}, onSettingsClick = {})
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Design preview · debug only", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onBack) { Text("Close") }
        }
        Text("Component preview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        SearchBar("", {}, {}, onVoiceClick = {})
        FeatureCard("Feature card component", "Preview content only.", "Preview action", {})
        MedicineCard("Medicine card component", "Preview only · not a product record", onClick = {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip("Selected", true, {})
            CategoryChip("Unselected", false, {})
        }
        SectionHeader("Section header component", supportingText = "Supporting text preview")
        InfoSection("Collapsible information section", "Preview-only explanatory copy.")
        WarningBanner("Warning banner component · preview only.")
        EmergencyCard("Emergency card component · preview only. The action opens the phone dialer.")
        ChatBubble("Chat bubble component · preview only.", fromUser = true)
        ChatBubble("Assistant bubble component · preview only.", fromUser = false)
        Text("Typing indicator", style = MaterialTheme.typography.titleMedium)
        TypingIndicator()
        Text("Shimmer placeholder", style = MaterialTheme.typography.titleMedium)
        ShimmerPlaceholder()
        EmptyState("Empty state component", "Preview-only empty state.")
        ErrorState("Error state component · preview only.", onRetry = {})
        Text("Mascot expressions", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MascotExpression.entries.forEach { expression ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MedLensMascot(expression, Modifier.size(70.dp))
                    Text(expression.name.lowercase(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

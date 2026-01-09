/*
 * Copyright © 2024 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.utils.Colors
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * Navigation drawer content for the main screen.
 */
@Composable
fun NavigationDrawerContent(
    profiles: List<ProfileListItem>,
    currentProfileId: Long?,
    onProfileSelected: (Long) -> Unit,
    onEditProfile: (Long) -> Unit,
    onCreateNewProfile: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToBackups: () -> Unit,
    onProfilesReordered: (List<ProfileListItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track dragging state to prevent parent updates from resetting the list during drag
    var isDragging by remember { mutableStateOf(false) }
    var reorderableProfiles by remember { mutableStateOf(profiles) }

    // Sync with parent profiles only when not dragging
    LaunchedEffect(profiles) {
        if (!isDragging) {
            reorderableProfiles = profiles
        }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            isDragging = true
            reorderableProfiles = reorderableProfiles.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { _, _ ->
            onProfilesReordered(reorderableProfiles)
            isDragging = false
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        DrawerHeader()

        // Profile list
        LazyColumn(
            state = reorderState.listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .reorderable(reorderState),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = reorderableProfiles,
                key = { it.id }
            ) { profile ->
                ReorderableItem(reorderState, key = profile.id) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 8.dp else 0.dp,
                        label = "elevation"
                    )
                    ProfileRow(
                        profile = profile,
                        isSelected = profile.id == currentProfileId,
                        onProfileClick = { onProfileSelected(profile.id) },
                        onEditClick = { onEditProfile(profile.id) },
                        modifier = Modifier
                            .shadow(elevation)
                            .background(MaterialTheme.colorScheme.surface)
                            .detectReorderAfterLongPress(reorderState)
                    )
                }
            }

            // Add new profile button (not reorderable)
            item {
                AddProfileRow(onClick = onCreateNewProfile)
            }
        }

        // Bottom menu items
        HorizontalDivider()
        DrawerMenuItem(
            icon = Icons.AutoMirrored.Filled.List,
            label = stringResource(R.string.nav_templates),
            onClick = onNavigateToTemplates
        )
        DrawerMenuItem(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.backups_activity_label),
            onClick = onNavigateToBackups
        )
    }
}

@Composable
private fun DrawerHeader() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon_transparent_bg),
                contentDescription = stringResource(R.string.nav_header_desc),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: ProfileListItem,
    isSelected: Boolean,
    onProfileClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    val themeColor = remember(profile.theme) {
        if (profile.theme == -1) {
            Color(Colors.getPrimaryColorForHue(Colors.DEFAULT_HUE_DEG))
        } else {
            Color(Colors.getPrimaryColorForHue(profile.theme))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onProfileClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color tag
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .background(themeColor)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Profile name
        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // Edit button (always visible)
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddProfileRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.new_profile_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DrawerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

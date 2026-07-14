package com.gentlemanstore.feature.support.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.gentlemanstore.ui.theme.Gold500

/**
 * Red status filter chipova za support tikete sa unread badge-om po kategoriji.
 * Deli ga customer SupportScreen i Employee Panel -> Support Tickets tab -
 * ista logika, isti dizajn (postojeci crveni kruzni badge sa "9+" cap-om).
 *
 * [unreadByStatus] dolazi sa backend unread-summary endpointa (racunat nad
 * svim tiketima, ne nad trenutnom stranicom); "ALL" badge je zbir svih.
 * Badge se ne prikazuje kada je brojac 0.
 */
@Composable
fun TicketStatusFilterRow(
    selectedStatus: String?,
    unreadByStatus: Map<String, Int>,
    onStatusSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    statuses: List<String> = listOf("ALL", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(count = statuses.size, key = { statuses[it] }) { index ->
            val status = statuses[index]
            val isSelected = when {
                status == "ALL" && selectedStatus == null -> true
                status == selectedStatus -> true
                else -> false
            }
            val unreadCount = if (status == "ALL") {
                unreadByStatus.values.sum()
            } else {
                unreadByStatus[status] ?: 0
            }
            FilterChip(
                selected = isSelected,
                onClick = { onStatusSelected(if (status == "ALL") null else status) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(status)
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = TextUnit(9f, TextUnitType.Sp)
                                )
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Gold500,
                    selectedLabelColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

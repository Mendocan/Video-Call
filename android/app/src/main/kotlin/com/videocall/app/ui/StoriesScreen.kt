package com.videocall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videocall.app.model.Contact
import com.videocall.app.model.Story
import com.videocall.app.viewmodel.VideoCallViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import com.videocall.app.utils.TimeFormatter

@Composable
fun StoriesScreen(
    stories: List<Story>,
    viewModel: VideoCallViewModel,
    addedContacts: List<Contact>,
    modifier: Modifier = Modifier
) {
    // Aktif hikayeleri filtrele (24 saat geçmemiş olanlar)
    val activeStories = stories.filter { it.isActive() }
    
    // Hikayeleri yayıncıya göre grupla
    val storiesByBroadcaster = activeStories.groupBy { it.broadcasterPhoneNumber }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Hikayeler",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (activeStories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Henüz hikaye yok",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Canlı yayınlarınız 24 saat süreyle burada görünecek",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                storiesByBroadcaster.forEach { (phoneNumber, broadcasterStories) ->
                    val contact = addedContacts.find { it.phoneNumber == phoneNumber }
                    val latestStory = broadcasterStories.maxByOrNull { it.createdAt }
                    
                    if (latestStory != null) {
                        item {
                            StoryItem(
                                story = latestStory,
                                contact = contact,
                                storyCount = broadcasterStories.size,
                                onStoryClick = {
                                    // Hikayeye tıklandığında canlı yayına katıl
                                    viewModel.joinLiveStream(latestStory.liveId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryItem(
    story: Story,
    contact: Contact?,
    storyCount: Int,
    onStoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingSeconds = story.getRemainingSeconds()
    val remainingHours = remainingSeconds / 3600
    val remainingMinutes = (remainingSeconds % 3600) / 60
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onStoryClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profil resmi veya baş harf
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (contact?.name?.firstOrNull() ?: story.broadcasterName?.firstOrNull() ?: '?').toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = contact?.name ?: story.broadcasterName ?: story.broadcasterPhoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (story.title != null) {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kalan süre
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TimeFormatter.formatRemainingTime(remainingSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Başlangıç zamanı
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TimeFormatter.formatLiveTime(story.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // İzleyici sayısı
                    if (story.viewerCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${story.viewerCount} izleyici",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Hikaye sayısı badge
            if (storyCount > 1) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = storyCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


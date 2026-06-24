package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RecorderViewModel

@Composable
fun VideoGalleryScreen(viewModel: RecorderViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val videos = viewModel.savedVideos.collectAsState().value
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (videos.isEmpty()) {
            item {
                Text(
                    text = "No saved videos found.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(videos) { file ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                             // Open the video with external player using FileProvider
                             val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                             val intent = Intent(Intent.ACTION_VIEW).apply {
                                 setDataAndType(uri, "video/*")
                                 addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                 addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                             }
                             context.startActivity(intent)
                        }
                ) {
                    Text(
                        text = file.name,
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

package com.pratyaksh.iykyk

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.pratyaksh.iykyk.ui.theme.IykykTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IykykTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
                    HomeScreen(contentPadding)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(contentPadding: PaddingValues) {
    var selectedVideoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val context = LocalContext.current
    val contentResolver = context.contentResolver

    var extractedFrames by remember {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    var selectedVideoName by remember {
        mutableStateOf<String?>(null)
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedVideoUri = uri

        uri?.let {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, it)

            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L

            retriever.release()

            contentResolver.query(
                it,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    selectedVideoName = cursor.getString(nameIndex)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ){
        Text(
            text = "Home Screen",
        )
        Button(onClick = {videoPickerLauncher.launch("video/*")}) {
            Text(text = "Select Video")
        }

        if (selectedVideoUri != null) {
            Text(text = "Video Name: $selectedVideoName")
        }

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IykykTheme {
        HomeScreen(PaddingValues())
    }
}
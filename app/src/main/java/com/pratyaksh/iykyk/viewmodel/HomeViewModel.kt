package com.pratyaksh.iykyk.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set

    var selectedVideoName by mutableStateOf<String?>(null)
        private set

    fun onVideoSelected(uri: Uri?) {
        selectedVideoUri = uri
    }
}
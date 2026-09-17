#!/bin/bash
sed -i '/val uiState by viewModel/i \    val context = androidx.compose.ui.platform.LocalContext.current' app/src/main/java/com/example/MainActivity.kt
sed -i '/import android.os.Bundle/a import android.widget.Toast' app/src/main/java/com/example/MainActivity.kt

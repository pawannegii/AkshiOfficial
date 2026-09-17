#!/bin/bash
sed -i '/import androidx.compose.runtime.setValue/a \
import androidx.compose.ui.graphics.layer.drawLayer\nimport androidx.compose.ui.graphics.rememberGraphicsLayer\nimport androidx.compose.ui.draw.drawWithCache\nimport androidx.compose.runtime.rememberCoroutineScope\nimport androidx.core.content.FileProvider\nimport java.io.File\nimport java.io.FileOutputStream\nimport kotlinx.coroutines.launch\nimport androidx.compose.ui.graphics.asAndroidBitmap' app/src/main/java/com/example/ui/screens/ReportScreen.kt

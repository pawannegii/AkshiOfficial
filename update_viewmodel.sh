#!/bin/bash
sed -i '/val uiState/a \    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)\n    val sortOrder = _sortOrder.asStateFlow()\n' app/src/main/java/com/example/viewmodel/AkshiViewModel.kt
sed -i '/import kotlinx.coroutines.flow.combine/a import com.example.viewmodel.SortOrder' app/src/main/java/com/example/viewmodel/AkshiViewModel.kt

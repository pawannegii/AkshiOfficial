#!/bin/bash
sed -i '/enum class SortOrder(val label: String) {/,/}/d' app/src/main/java/com/example/viewmodel/AkshiViewModel.kt
sed -i '/import com.example.viewmodel.SortOrder/d' app/src/main/java/com/example/viewmodel/AkshiViewModel.kt

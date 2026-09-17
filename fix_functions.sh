#!/bin/bash
sed -i '/fun updateSearchQuery/d' app/src/main/java/com/example/viewmodel/AkshiViewModel.kt
sed -i 's/        _searchQuery.value = query/    fun updateSearchQuery(query: String) {\n        _searchQuery.value = query/' app/src/main/java/com/example/viewmodel/AkshiViewModel.kt

#!/bin/bash
sed -i '11a \
enum class SortOrder(val label: String) {\n    DATE_DESC("Newest First"),\n    DATE_ASC("Oldest First"),\n    LOCATION("Location (A-Z)")\n}\n' app/src/main/java/com/example/viewmodel/AkshiViewModel.kt

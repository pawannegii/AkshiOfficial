#!/bin/bash
sed -i 's/onSearchQueryChange: (String) -> Unit,/onSearchQueryChange: (String) -> Unit,\n    onFilterClick: () -> Unit = {},/' app/src/main/java/com/example/ui/screens/HomeScreen.kt
sed -i 's/onSearchQueryChange: (String) -> Unit,/onSearchQueryChange: (String) -> Unit,\n    onFilterClick: () -> Unit = {},/' app/src/main/java/com/example/ui/screens/MainScreens.kt

#!/bin/bash
sed -i 's/onQueryChange = onSearchQueryChange/onQueryChange = onSearchQueryChange,\n                    onFilterClick = onFilterClick/' app/src/main/java/com/example/ui/screens/HomeScreen.kt
sed -i 's/onQueryChange = onSearchQueryChange/onQueryChange = onSearchQueryChange,\n                    onFilterClick = onFilterClick/' app/src/main/java/com/example/ui/screens/MainScreens.kt

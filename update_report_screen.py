import re

with open("app/src/main/java/com/example/ui/screens/ReportScreen.kt", "r") as f:
    content = f.read()

# Update signature
sig_replace = """    onNavigateBack: () -> Unit,
    isSaved: Boolean,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier"""
content = re.sub(r'    onNavigateBack: \(\) -> Unit,\s*isSaved: Boolean,\s*modifier: Modifier = Modifier', sig_replace, content)

# Add Favorite button next to Share
btn_replace = """                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("favorite_report_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) androidx.compose.material.icons.Icons.Filled.Favorite else androidx.compose.material.icons.Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite Report",
                            tint = if (isFavorite) androidx.compose.ui.graphics.Color.Red else TextPrimary
                        )
                    }
                    IconButton(
                        onClick = { shareReport() },
                        modifier = Modifier.testTag("share_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.IosShare,
                            contentDescription = "Share Report",
                            tint = SagePrimary
                        )
                    }
                }"""
content = re.sub(r'                    IconButton\(\s*onClick = \{ shareReport\(\) \},\s*modifier = Modifier\.testTag\("share_report_button"\)\s*\) \{\s*Icon\(\s*imageVector = Icons\.Outlined\.IosShare,\s*contentDescription = "Share Report",\s*tint = SagePrimary\s*\)\s*\}\s*\}', btn_replace, content)

# Import Filled Favorite if not imported
if "import androidx.compose.material.icons.filled.Favorite" not in content:
    content = content.replace("import androidx.compose.material.icons.Icons", "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.Favorite\nimport androidx.compose.material.icons.outlined.FavoriteBorder")

with open("app/src/main/java/com/example/ui/screens/ReportScreen.kt", "w") as f:
    f.write(content)

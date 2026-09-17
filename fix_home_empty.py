import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

replacement = """            if (reports.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found for '$searchQuery'",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(reports, key = { it.id }) { report ->
                    com.example.ui.components.ReportCard(
                        report = report,
                        onClick = { onViewSavedReport(report) }
                    )
                }
            }"""
            
content = re.sub(r'            items\(reports, key = \{ it\.id \}\) \{ report ->\s*com\.example\.ui\.components\.ReportCard\(\s*report = report,\s*onClick = \{ onViewSavedReport\(report\) \}\s*\)\s*\}', replacement, content)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)

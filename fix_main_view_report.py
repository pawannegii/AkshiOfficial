import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

replacement = """                onViewSavedReport = { report ->
                    viewModel.viewSavedReport(report)
                    navController.navigate("report")
                },"""

content = re.sub(r'                onViewSavedReport = \{ report ->\s+val onionResult = com\.example\.model\.OnionResult\([\s\S]*?notes = report\.notes\s+\)\s+navController\.navigate\("report"\)\s+\},', replacement, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

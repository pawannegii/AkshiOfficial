import re

with open('app/src/main/java/com/example/ui/screens/ReportScreen.kt', 'r') as f:
    content = f.read()

# Replace the first occurrence of 'modifier = Modifier.fillMaxWidth(),' inside AppleCard
content = re.sub(
    r'(AppleCard\(\s+)modifier = Modifier.fillMaxWidth(),',
    r'\1modifier = Modifier.fillMaxWidth().drawWithCache { onDrawWithContent { graphicsLayer.record { this@onDrawWithContent.drawContent() }; drawLayer(graphicsLayer) } },',
    content
)

with open('app/src/main/java/com/example/ui/screens/ReportScreen.kt', 'w') as f:
    f.write(content)

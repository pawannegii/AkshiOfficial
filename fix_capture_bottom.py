import re

with open("app/src/main/java/com/example/ui/screens/CaptureScreen.kt", "r") as f:
    content = f.read()

# Make background match Home
content = content.replace(".background(Color(0xFF141413))", ".background(com.example.ui.theme.StoneBackground)")

# Update bottom bar gradient and styles
bottom_gradient = """                .background(
                    com.example.ui.theme.StoneBackground
                )"""
content = re.sub(r'\.background\(\s*Brush\.verticalGradient\(\s*colors = listOf\(\s*Color\.Transparent,\s*Color\(0xFF141413\)\.copy\(alpha = 0\.95f\),\s*Color\(0xFF141413\)\s*\)\s*\)\s*\)', bottom_gradient, content)

# Update chip backgrounds and text colors
content = content.replace("Color.White.copy(alpha = 0.15f)", "com.example.ui.theme.StoneSurface")
content = content.replace("color = if (selectedPreset == \"GRADE_A\") Color.White else Color.White.copy(alpha = 0.7f)", "color = if (selectedPreset == \"GRADE_A\") Color.White else com.example.ui.theme.TextSecondary")
content = content.replace("color = if (selectedPreset == \"MIXED\") Color.White else Color.White.copy(alpha = 0.7f)", "color = if (selectedPreset == \"MIXED\") Color.White else com.example.ui.theme.TextSecondary")

# Icon tint colors
content = content.replace("tint = Color.White", "tint = com.example.ui.theme.TextPrimary")

# But we must restore the top controls to white text because they are on the dark camera preview
content = content.replace('tint = com.example.ui.theme.TextPrimary\n                )\n            }\n            Surface(', 'tint = Color.White\n                )\n            }\n            Surface(')

with open("app/src/main/java/com/example/ui/screens/CaptureScreen.kt", "w") as f:
    f.write(content)

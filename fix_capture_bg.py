import re

with open("app/src/main/java/com/example/ui/screens/CaptureScreen.kt", "r") as f:
    content = f.read()

content = content.replace(".background(Color(0xFF141413))", ".background(com.example.ui.theme.StoneBackground)")

with open("app/src/main/java/com/example/ui/screens/CaptureScreen.kt", "w") as f:
    f.write(content)

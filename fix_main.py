import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Remove import
content = re.sub(r'import com\.example\.ui\.screens\.AboutDeveloperScreen\n', '', content)

# Remove composable("about")
content = re.sub(r'        composable\(\n            route = "about",[\s\S]*?\}\n        \}\n    \}\n}', '    }\n}', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

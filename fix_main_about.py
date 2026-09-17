import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Remove the remainder of the about route if it's there
content = re.sub(r'        composable\(\n            route = "about"[\s\S]*?AboutDeveloperScreen\([\s\S]*?\}\n        \)', '', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

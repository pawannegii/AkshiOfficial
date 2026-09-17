# Akshi — Project Development Rules & Performance Guidelines

## Motion & Transitions
1. **Fluid Easing Curves**:
   - Never use raw linear or default un-eased tweens for screen transitions.
   - Use Apple / Material 3 Deceleration curves: `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)` for entrances and `CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)` for exits.
   - Combine directional container slides (`initialOffset = { it / 4 }`) with subtle `fadeIn` / `fadeOut` so screens crossfade smoothly without stutter.
   - For camera views (`CaptureScreen`), slide in from bottom (`SlideDirection.Up`) to prevent tearing.
   - For scanning/loading flows (`AnalyzingScreen`), use `fadeIn + scaleIn` dissolve rather than sliding.

2. **Tactile Spring Micro-Interactions**:
   - All interactive cards (`AppleCard`) and buttons (`AppleButton`) must use spring-damped press feedback (`spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)` or `Spring.StiffnessMedium`) via `graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }`.

3. **Backstack & Resource Hygiene**:
   - Always pop intermediate scanning/capture steps (`popUpTo("home") { inclusive = false }`) when transitioning to final results to unbind CameraX hardware pipelines and release memory.
   - Never leave active camera preview surfaces lingering in the backstack.

4. **Lazy Layout Performance**:
   - Always provide stable `key` lambdas for `LazyColumn` and `LazyRow` items (`key = { it.id }`).
   - Hoist and `remember` formatters (e.g., `SimpleDateFormat`), painters, and expensive computed states outside row item blocks to avoid GC allocation pauses during scrolling.

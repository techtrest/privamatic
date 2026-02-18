# Privamatic - Development Conventions

## Design Standards

### Typography
- Use MaterialTheme.typography ONLY
- Never use arbitrary fontSize
- Styles: displayLarge, headlineMedium, titleLarge, bodyMedium, labelSmall
- No custom Text styles unless absolutely necessary

### Icons
- Material Icons ONLY (Icons.Default.*)
- NO emoji in production UI
- Icons should be semantic (e.g., Icons.Default.Security for security)
- Use contentDescription for accessibility

### Colors
- MaterialTheme.colorScheme ONLY
- Primary: British Racing Green (#0B4619)
- Never hardcode colors (#RRGGBB)
- Use semantic color names (primary, surface, error, etc.)

### Spacing
- Material Design spacing scale: 4dp, 8dp, 12dp, 16dp, 24dp, 32dp
- Use Arrangement.spacedBy() for consistent gaps
- Padding in multiples of 4dp
- Never use arbitrary values (e.g., 13dp, 27dp)

### Components
- Material 3 components ONLY
- No custom clickable boxes - use Card, Button, etc.
- Proper elevation (2dp for cards, 4dp for interactive)
- Corner radius: 8dp (small), 12dp (standard)

## Code Style

### Naming Conventions
**Files:**
- Screens: `XxxScreen.kt` (e.g., DashboardScreen.kt)
- Components: `XxxComponent.kt` or in components/ folder
- Models: `Xxx.kt` (e.g., PrivacyScore.kt)
- Utilities: `XxxUtil.kt` or as object singletons

**Kotlin:**
- Classes: PascalCase (e.g., `PrivacyScoreCalculator`)
- Functions: camelCase (e.g., `calculateScore`)
- Composables: PascalCase (e.g., `DashboardScreen`)
- Properties: camelCase (e.g., `isRefreshing`)
- Constants: UPPER_SNAKE_CASE (e.g., `MAX_SCORE`)
- Private functions: prefix with `private`
- Extension functions: camelCase (e.g., `List<T>.countSecure()`)

### File Organization
```
app/src/main/java/com/techtrest/privacywidget/
├── data/
│   ├── model/          # Data classes
│   ├── scanner/        # Detection logic
│   └── QuickWinsDetector.kt
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # Full screen composables
│   ├── navigation/     # Navigation logic
│   └── theme/          # Theme definition
└── MainActivity.kt
```

### Compose Patterns

**Modifier Usage:**
```kotlin
// ✅ CORRECT: Modifier always last parameter, chained properly
@Composable
fun MyComponent(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier  // Last parameter, default value
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

// ❌ WRONG: Modifier not last, no default
@Composable
fun MyComponent(
    modifier: Modifier,
    title: String
)
```

**State Management:**
```kotlin
// ✅ CORRECT: Stateless, hoisted state
@Composable
fun Counter(
    count: Int,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
)

// ❌ WRONG: Stateful composable
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
}
```

**Remember with Dependencies:**
```kotlin
// ✅ CORRECT: Remember with key
val quickWins = remember(privacyScore) {
    QuickWinsDetector.detectQuickWins(privacyScore)
}

// ❌ WRONG: Remember without dependency
val quickWins = remember {
    QuickWinsDetector.detectQuickWins(privacyScore)
}
```

**No Logic in Composables:**
```kotlin
// ✅ CORRECT: Delegate to calculator
val score = remember(issues) {
    PrivacyScoreCalculator.calculateScore(issues)
}

// ❌ WRONG: Logic in composable
val score = issues.filter { !it.isSecure }.sumOf { it.points }
```

### Kotlin Best Practices

**Immutability:**
```kotlin
// ✅ CORRECT: Prefer val
val items = listOf(1, 2, 3)
val score = calculateScore()

// ❌ WRONG: Unnecessary var
var items = listOf(1, 2, 3)  // Never reassigned
```

**When Expressions:**
```kotlin
// ✅ CORRECT: When expression
val rating = when {
    score >= 85 -> ScoreRating.EXCELLENT
    score >= 70 -> ScoreRating.GOOD
    else -> ScoreRating.FAIR
}

// ❌ WRONG: If-else chain
val rating = if (score >= 85) {
    ScoreRating.EXCELLENT
} else if (score >= 70) {
    ScoreRating.GOOD
} else {
    ScoreRating.FAIR
}
```

**Extension Functions:**
```kotlin
// ✅ CORRECT: Type-specific logic as extension
fun PrivacyScore.getSecurityIssuesCount() =
    issues.count { it.check in PrivacyCategory.SYSTEM_SECURITY.checks && !it.isSecure }

// Usage
val count = privacyScore.getSecurityIssuesCount()

// ❌ WRONG: Utility function with parameter
fun getSecurityIssuesCount(score: PrivacyScore) = 
    score.issues.count { ... }
```

**Data Classes:**
```kotlin
// ✅ CORRECT: Immutable data class
data class PrivacyScore(
    val score: Int,
    val maxScore: Int,
    val issues: List<PrivacyIssue>,
    val scanTimestamp: Long
)

// ❌ WRONG: Mutable properties
data class PrivacyScore(
    var score: Int,
    var issues: List<PrivacyIssue>
)
```

## Architecture Patterns

### Separation of Concerns
- **UI Layer**: Composables (ui/screens, ui/components)
- **Business Logic**: Calculator/Detector objects (data/scanner)
- **Data Models**: Immutable data classes (data/model)
- **Navigation**: Centralized state (ui/navigation)

### State Management
- Composables are stateless
- State hoisted to parent or shared state object
- Use `remember` for derived/computed state
- Use `rememberSaveable` for state that survives config changes

### Data Flow
```
User Action → Event Handler → State Update → Recomposition
```

Example:
```kotlin
// State holder
val navigationState = remember { AppNavigationState() }

// Event handler
onClick = { navigationState.selectTab(NavigationTab.DETAILS) }

// UI reacts to state
when (navigationState.selectedTab) { ... }
```

## Anti-Patterns to Avoid

### ❌ Duplicated Logic
**DON'T:**
```kotlin
val count1 = issues.count { it.check in securityChecks && !it.isSecure }
// ... 50 lines later
val count2 = issues.count { it.check in trackingChecks && !it.isSecure }
```

**DO:**
```kotlin
fun List<PrivacyIssue>.countByCategory(category: PrivacyCategory) =
    count { it.check in category.checks && !it.isSecure }

val securityCount = issues.countByCategory(PrivacyCategory.SYSTEM_SECURITY)
val trackingCount = issues.countByCategory(PrivacyCategory.NETWORK_PRIVACY)
```

### ❌ Magic Strings and Numbers
**DON'T:**
```kotlin
if (score >= 85) return "Excellent Privacy"
val padding = 23.dp
```

**DO:**
```kotlin
private const val EXCELLENT_THRESHOLD = 85
private val CARD_PADDING = 24.dp  // Material Design scale

if (score >= EXCELLENT_THRESHOLD) return ScoreRating.EXCELLENT.displayName
```

### ❌ Hardcoded UI Text
**DON'T:**
```kotlin
Text("Excellent Privacy")
```

**DO:**
```kotlin
// Use enum/sealed class
enum class ScoreRating(val displayName: String) {
    EXCELLENT("Excellent Privacy")
}

Text(rating.displayName)
```

### ❌ Business Logic in Composables
**DON'T:**
```kotlin
@Composable
fun ScoreDisplay(issues: List<PrivacyIssue>) {
    val score = 100 - issues.filter { !it.isSecure }.sumOf { it.points }
    Text("$score")
}
```

**DO:**
```kotlin
@Composable
fun ScoreDisplay(privacyScore: PrivacyScore) {
    Text("${privacyScore.score}")
}

// Business logic in calculator
object PrivacyScoreCalculator {
    fun calculateScore(issues: List<PrivacyIssue>): PrivacyScore { ... }
}
```

### ❌ Nullable Types Without Reason
**DON'T:**
```kotlin
data class PrivacyScore(
    val score: Int?,  // Why nullable?
    val issues: List<PrivacyIssue>?
)
```

**DO:**
```kotlin
data class PrivacyScore(
    val score: Int,
    val issues: List<PrivacyIssue>  // Empty list if none
)
```

## Code Quality Checklist

Before committing code, verify:

**Style:**
- [ ] All naming follows conventions (PascalCase/camelCase/UPPER_SNAKE_CASE)
- [ ] Files organized in correct directories
- [ ] No arbitrary fontSize/spacing/colors
- [ ] Material 3 components used

**Kotlin:**
- [ ] Prefer `val` over `var`
- [ ] Use `when` over if-else chains
- [ ] Data classes are immutable
- [ ] Extension functions for type-specific logic

**Compose:**
- [ ] Modifier last parameter with default
- [ ] Composables are stateless
- [ ] `remember` has dependencies listed
- [ ] No business logic in Composables

**Quality:**
- [ ] No duplicated logic
- [ ] No magic strings/numbers
- [ ] No hardcoded colors
- [ ] Proper error handling

**Performance:**
- [ ] Heavy operations memoized with `remember`
- [ ] No unnecessary recompositions
- [ ] Expensive lists use LazyColumn

## Philosophy

**Clean:** Minimal, professional, no bloat
**Consistent:** Follow patterns everywhere
**Accessible:** Proper contentDescription, semantic colors
**Maintainable:** DRY principle, clear naming, separation of concerns
**Privacy-First:** Minimal permissions, transparent limitations

## Resources

- [Material 3 Design](https://m3.material.io/)
- [Compose Guidelines](https://developer.android.com/jetpack/compose/guidelines)
- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)

---

*Last Updated: 2026-01-31*
*When updating conventions, increment date and summarize changes*
```

---


# Privacy Guard - Quick Reference

## For AI Code Generation

### Every Prompt Must Include:
```
IMPORTANT: Read CONVENTIONS.md and .cursorrules before starting.
```

### Common Tasks

**Add New Screen:**
1. Create `ui/screens/XxxScreen.kt`
2. Follow pattern from existing screens
3. Use stateless composable with hoisted state
4. Material 3 components only

**Add New Check:**
1. Add to PrivacyCheck enum with point value
2. Implement detection in appropriate scanner
3. Add to relevant PrivacyCategory
4. Update Scoring System page

**Add UI Component:**
1. Create in `ui/components/`
2. Modifier last parameter with default
3. Use MaterialTheme values (no hardcoding)
4. Make stateless

### Quality Checklist (30 seconds)
```bash
# After any code generation:
- [ ] Read the diff - any duplication?
- [ ] Check for magic strings/numbers
- [ ] Verify Material theme usage
- [ ] Confirm proper naming
- [ ] Build and test
```

### Common Mistakes to Catch
- Hardcoded colors (`#0B4619` → `MaterialTheme.colorScheme.primary`)
- Magic numbers (`16` → `16.dp` or extract constant)
- Duplicated logic (extract to function)
- Stateful composables (hoist state up)
- Modifier not last parameter

### When to Seek Review
- Major architectural changes
- New design patterns
- Performance concerns
- After 3+ features (consistency check)

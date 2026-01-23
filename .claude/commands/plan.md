---
name: plan
description: Create detailed implementation plan for a feature
---

# Implementation Planning

You are about to create a detailed implementation plan using the planner agent.

## Instructions

1. **Analyze the Request**
   - Understand the feature/change requested
   - Identify affected components (ViewModel, Repository, Compose UI)
   - Check existing patterns in CLAUDE.md

2. **Launch Planner Agent**
   - Use the Task tool with `subagent_type: planner`
   - Provide context about the request
   - Include relevant file paths

3. **Review Plan Output**
   - Verify architecture alignment
   - Check dependency order
   - Confirm testing strategy

## Plan Structure

The plan should include:
- Overview (2-3 sentences)
- Requirements list
- Architecture changes
- Implementation steps (phased)
- Testing strategy
- Risks and mitigations

## Example Usage

User: "Add dark mode toggle to settings"

Plan Output:
```markdown
# Implementation Plan: Dark Mode Toggle

## Overview
Add a dark mode toggle to the application settings that persists user preference.

## Requirements
- Toggle switch in settings screen
- Persist preference in DataStore
- Apply theme change immediately

## Implementation Steps

### Phase 1: Data Layer
1. Add ThemePreference to PreferencesRepository
...

### Phase 2: ViewModel
1. Add theme state to SettingsViewModel
...

### Phase 3: UI Layer
1. Add toggle Composable
...

## Testing Strategy
- Unit test: SettingsViewModel with FakePreferencesRepository
- UI test: Verify toggle updates theme
```

## After Planning

Once plan is approved:
1. Start with the first phase
2. Run tests after each step: `nix run .#test`
3. Verify build: `nix run .#build`
4. For UI changes: `nix run .#verify`

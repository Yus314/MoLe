# Strategic Compact Skill

Strategic planning and scope management for complex features.

## Scope Definition

### Problem Statement
```markdown
**Problem**: [What issue are we solving?]
**Impact**: [Who is affected and how?]
**Success Criteria**: [How do we know we're done?]
```

### Scope Boundaries
```markdown
**In Scope**:
- [Feature A]
- [Feature B]

**Out of Scope** (for now):
- [Future consideration X]
- [Nice-to-have Y]

**Dependencies**:
- [External dependency]
- [Internal dependency]
```

## Prioritization Framework

### MoSCoW Method
| Priority | Definition | Example |
|----------|------------|---------|
| **Must** | Critical for release | Core functionality |
| **Should** | Important but not critical | UX improvements |
| **Could** | Nice to have | Polish features |
| **Won't** | Not this time | Future scope |

### Priority Matrix
```
         High Impact
              │
    Quick     │     Strategic
    Wins      │     Initiatives
              │
Low Effort────┼────High Effort
              │
    Fill-ins  │     Major
              │     Projects
              │
         Low Impact
```

## Phase Planning

### Phase 1: Foundation
- Core domain models
- Basic repository
- Minimal ViewModel
- Simple UI

### Phase 2: Enhancement
- Complete features
- Error handling
- Edge cases
- Validation

### Phase 3: Polish
- Performance optimization
- UX improvements
- Additional tests
- Documentation

## Risk Assessment

### Risk Template
```markdown
**Risk**: [Description]
**Probability**: Low / Medium / High
**Impact**: Low / Medium / High
**Mitigation**: [How to reduce risk]
**Contingency**: [What if it happens]
```

### Common Risks in MoLe
| Risk | Mitigation |
|------|------------|
| Database migration failure | Test with existing data |
| UI regression | Verify with `nix run .#verify` |
| Performance degradation | Profile before/after |
| Breaking existing tests | Run `nix run .#test` frequently |

## Decision Making

### Decision Template
```markdown
**Decision**: [What we decided]
**Context**: [Why we needed to decide]
**Options Considered**:
1. [Option A] - Pros / Cons
2. [Option B] - Pros / Cons
**Rationale**: [Why we chose this option]
**Consequences**: [What this means going forward]
```

## Progress Tracking

### Milestone Definition
```markdown
## Milestone: [Name]

**Target**: [Date or condition]
**Deliverables**:
- [ ] [Item 1]
- [ ] [Item 2]

**Success Criteria**:
- [ ] All tests pass
- [ ] Code reviewed
- [ ] Verified on device
```

### Status Updates
- **Green**: On track
- **Yellow**: Minor issues, recoverable
- **Red**: Blocked, needs intervention

## Incremental Delivery

### Principles
1. **Small commits**: Easy to review, easy to revert
2. **Working state**: Each commit should build/test
3. **Feature flags**: Hide incomplete features if needed
4. **Backward compatible**: Don't break existing functionality

### Verification at Each Step
```bash
# After each change
nix run .#test
nix run .#build

# After UI changes
nix run .#verify
```

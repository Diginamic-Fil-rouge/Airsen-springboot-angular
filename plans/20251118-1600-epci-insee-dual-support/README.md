# EPCI + INSEE Dual Code Support - Implementation Plan

**Plan Directory**: `/plans/20251118-1600-epci-insee-dual-support/`
**Created**: 2025-11-18
**Status**: Ready for Implementation
**Estimated Effort**: 7 days (2 senior developers)

---

## Quick Navigation

### 📋 Start Here
- **[Main Plan](./plan.md)** - Executive summary, problem statement, success criteria, risk assessment

### 📚 Research Foundation
- **[ATMO API Zone Types Research](./research/researcher-01-atmo-api-zone-types.md)** - EPCI vs INSEE codes, French administrative structure
- **[Implementation Strategies Analysis](./research/researcher-02-implementation-strategies.md)** - Three approaches evaluated, Option A recommended

### 🚀 Implementation Phases (Sequential)

| Phase | File | Effort | Priority | Dependencies |
|-------|------|--------|----------|--------------|
| **Phase 1** | [Database Schema & Entity Changes](./phase-01-database-schema.md) | 1 day | Critical | None |
| **Phase 2** | [Repository Layer Updates](./phase-02-repository-layer.md) | 1 day | High | Phase 1 |
| **Phase 3** | [Service Layer Integration](./phase-03-service-layer.md) | 2 days | High | Phase 2 |
| **Phase 4** | [API Client Enhancements](./phase-04-api-client.md) | 1 day | Medium | Phase 3 |
| **Phase 5** | [Controller & Response DTOs](./phase-05-controller-dtos.md) | 0.5 days | Medium | Phase 4 |
| **Phase 6** | [Frontend Integration](./phase-06-frontend.md) | 1 day | Low (DEFERRED) | Phase 5 |
| **Phase 7** | [Testing & Validation](./phase-07-testing.md) | 1 day | Critical | Phases 1-5 |
| **Phase 8** | [Documentation & Deployment](./phase-08-documentation.md) | 0.5 days | High | Phase 7 |

**Total Backend Effort**: 6 days (Phase 6 deferred to future sprint)

---

## What This Plan Implements

### Problem
AIRSEN currently only supports commune-level air quality data using INSEE codes (5 digits). However, ATMO France API returns data at TWO territorial levels:
- **Communes** - Individual towns (identified by 5-digit INSEE codes)
- **EPCIs** - Intercommunal groupings (identified by 9-digit SIREN codes)

Some regions (Occitanie, Bretagne) aggregate air quality at EPCI level only, causing data loss.

### Solution
Add `epciCode` field to `Commune` entity, implement EPCI-aware fallback chain:

**Old Flow**:
```
Direct commune query → 20km spatial search → NOT_AVAILABLE
```

**New Flow**:
```
Direct commune query → EPCI grouping check → 20km spatial search → NOT_AVAILABLE
                              ↑ NEW STEP
```

### Key Benefits
1. **Better data coverage** - Access EPCI-aggregated data when commune-level data unavailable
2. **Regional accuracy** - Handle Occitanie/Bretagne regions that only provide EPCI-level data
3. **Data source transparency** - Responses indicate DIRECT/EPCI_AGGREGATE/ESTIMATED
4. **Optimized fallback** - Check EPCI grouping BEFORE expensive spatial search
5. **Architecture alignment** - Mirrors existing `departmentCode`, `regionCode` pattern

---

## Key Architectural Decisions

### Why Option A (Schema Addition)?
- **Type-safe**: Compiler-checked, database constraints enforce data integrity
- **Query efficient**: Single indexed lookup, no runtime parsing
- **Cache simple**: Deterministic keys without logic overhead
- **TestContainers-compatible**: Schema-based approach works with integration tests
- **Team-friendly**: Junior developers understand explicit schema better than dynamic parsing

### Why NOT Option B (Dynamic Detection)?
- Violates "database-first approach" principle (from CLAUDE.md)
- Requires API call to determine zone type (circular dependency)
- Cache key complexity (must parse response to build key)

### Why NOT Option C (Try-Fallback)?
- N+1 API calls exhaust rate limits (60 req/min quota)
- Unpredictable latency (2x API calls in worst case)
- Cache key collision risk

---

## Technical Highlights

### Database Changes
- **New column**: `communes.epci_code VARCHAR(10)` (nullable, indexed)
- **Migration**: Flyway V006 with ALGORITHM=INPLACE (zero-downtime)
- **Data population**: INSEE open data (>95% coverage target)

### Repository Methods (Phase 2)
- `findByEpciCode(String epciCode)` - All communes in EPCI
- `findByInseeCodeOrEpciCode(String code)` - Dual lookup
- `findByEpciCodeWithCoordinates()` - Spatial queries
- `findByEpciCodeWithEagerLoading()` - Prevent LazyInitializationException

### Service Layer Logic (Phase 3)
- Enhanced `fetchAirQualityWithFallback()` with EPCI check
- New helper: `findAirQualityForEpci()` - Representative commune selection
- Cache key strategy: `air-quality:commune:INSEE` vs `air-quality:epci:SIREN`

### API Client Updates (Phase 4)
- Parse `type_zone` field from ATMO responses
- Zone code validation (INSEE 5-digit vs EPCI 9-digit)
- Updated DTO: `AtmoAirQualityResponse` with `zoneType` field

### Response Enhancements (Phase 5)
- DTOs include `epciCode` field (nullable)
- New `DataSource.EPCI_AGGREGATE` enum value
- MapStruct mappings updated

---

## File Structure

```
plans/20251118-1600-epci-insee-dual-support/
├── README.md                          ← You are here
├── plan.md                            ← Main plan document
├── research/
│   ├── researcher-01-atmo-api-zone-types.md
│   └── researcher-02-implementation-strategies.md
├── phase-01-database-schema.md        ← Start implementation here
├── phase-02-repository-layer.md
├── phase-03-service-layer.md
├── phase-04-api-client.md
├── phase-05-controller-dtos.md
├── phase-06-frontend.md               ← DEFERRED
├── phase-07-testing.md
└── phase-08-documentation.md
```

---

## Getting Started

### For Implementation Team

**Day 1 (Phase 1)**:
1. Read [Main Plan](./plan.md) - Executive summary
2. Review [Phase 1 Plan](./phase-01-database-schema.md) - Database changes
3. Download INSEE EPCI mapping data
4. Create Flyway migration script
5. Update `Commune.java` entity

**Day 2 (Phase 2)**:
1. Read [Phase 2 Plan](./phase-02-repository-layer.md)
2. Add repository methods
3. Create comprehensive unit tests
4. Verify TestContainers integration

**Days 3-4 (Phase 3)**:
1. Read [Phase 3 Plan](./phase-03-service-layer.md)
2. Update service layer fallback logic
3. Implement cache key strategy
4. Write Mockito tests

**Continue through remaining phases...**

### For Code Reviewers

**Phase 1 Checklist**:
- [ ] Flyway migration includes rollback commands
- [ ] Entity validation annotations complete
- [ ] EPCI code coverage >95%
- [ ] Test fixtures include EPCI data

**Phase 2 Checklist**:
- [ ] Repository methods follow Spring Data JPA conventions
- [ ] JPQL queries use named parameters
- [ ] Test coverage >80% for new methods
- [ ] EXPLAIN query plans show index usage

**Phase 3 Checklist**:
- [ ] Fallback chain logic correct (direct → EPCI → geodistance)
- [ ] Cache keys differentiate data sources
- [ ] Service tests cover all paths
- [ ] Logging statements clear and concise

---

## Success Metrics

### Technical
- ✅ Database migration <5 min for 75,000+ communes
- ✅ EPCI code coverage >95%
- ✅ Test coverage >80% (JaCoCo enforcement)
- ✅ Repository queries <50ms (indexed)
- ✅ Fallback chain <2s (cache miss + API + EPCI)
- ✅ Cache hit rate >85%

### Functional
- ✅ System fetches data using BOTH INSEE and EPCI codes
- ✅ Responses indicate data source (DIRECT/EPCI_AGGREGATE/ESTIMATED)
- ✅ EPCI check executes BEFORE geodistance fallback
- ✅ Existing API behavior unchanged (backward compatible)

### Quality
- ✅ Zero breaking changes to REST endpoints
- ✅ All tests pass (unit + integration)
- ✅ No regressions in existing features
- ✅ Documentation complete (CLAUDE.md, Swagger, runbook)

---

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| EPCI data quality issues | Medium | High | Validate against INSEE registry, allow nullable initially |
| Cache key collision | Low | Medium | Use prefixed keys: `commune:` vs `epci:` |
| Rate limiter quota exhaustion | Medium | High | Check DB first (no API call for EPCI lookup) |
| TestContainers integration breaks | Low | Medium | Update base class, add EPCI test fixtures |
| Migration locks production table | Low | High | Use ALGORITHM=INPLACE (MariaDB 11.6) |

---

## Questions & Support

### Unresolved Questions (from research)
1. Should we cache EPCI mappings from BANATIC or use INSEE API directly?
2. What's acceptable latency for EPCI relationship updates (quarterly/monthly)?
3. Should frontend UI show both commune + EPCI options in search? (Phase 6 deferred)

### Contact
- **Tech Lead**: Review architectural decisions
- **DevOps**: Coordinate production deployment window
- **QA**: Manual ATMO API validation

---

## Related Documentation

### AIRSEN Codebase
- `CLAUDE.md` - Architecture patterns, development guidelines
- `docs/backend/03-database/entity-relationship-diagram.md` - Current ER diagram
- `docs/backend/02-architecture/implementation-roadmap.md` - Feature roadmap

### External Resources
- [INSEE EPCI Open Data](https://www.insee.fr/fr/information/2028028)
- [ATMO France API Docs](https://api-qualitairdeguadeloupe.data.atmo-france.org/api/documentation)
- [BANATIC Database](https://www.banatic.interieur.gouv.fr/)

---

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-11-18 | 1.0 | Initial plan created | AI Planning Agent |

---

**Ready to implement?** Start with [Phase 1: Database Schema](./phase-01-database-schema.md) 🚀

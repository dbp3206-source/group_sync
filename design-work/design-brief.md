# Design Brief

## Audience and viewing context
Vietnamese university students and group organizers using GroupSync on laptops and phones to move from personal planning to shared study or badminton activity.

## Core message
GroupSync turns one personal schedule into clear group decisions, activities, and measurable results.

## Desired reaction or action
Users should immediately understand what happens next, where their attention is needed, and how to move from My Schedule to Group Availability to Group Activity to Results.

## Source authority
- Existing frontend routes, DTOs, and API clients in `frontend/src`.
- Product and scope documents in `docs/`.
- Existing `DESIGN.md` design system.
- User-supplied Dribbble references for team workspace, calendar, and sports competition patterns.

## Content hierarchy
1. Next action and current group context.
2. Personal schedule and shared availability.
3. Study or badminton operations.
4. Results, ranking, notifications, and profile settings.

## Visual territory
Focused team operations with a warm editorial base; calm indigo academic spaces; energetic orange sports spaces; deep green and restrained gold for tournaments.

## Brand and system constraints
Forest-green GroupSync identity, Sora display type, Be Vietnam Pro body type, 4px spacing rhythm, mobile-first behavior, 44px touch targets, one Lucide icon family, no backend changes.

## Anti-goals
Generic SaaS card grids, purple AI gradients, decorative glass effects, permanent long organizer forms, emoji icons, tiny mobile controls, invented data, and navigation that treats every feature as a top-level destination.

## Output contract
Production React/TypeScript/Vite UI in the existing frontend, validated at 1440, 1024, 768, and 390px, with build/lint and public deployment verification.

## Reference interpretation
Borrow Shakuro's decisive workspace shell and operational density; Claudio's calendar hierarchy and colored time blocks; BowlsLink's competition tables, hierarchy, and sports energy. Do not copy proprietary artwork, exact layouts, or branding.

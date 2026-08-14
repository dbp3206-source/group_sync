# Design Brief

## Audience and viewing context

Individual university learners and early-career knowledge workers using a responsive desktop-first web application, with a strong mobile reading and capture experience.

## Core message

KnowledgeOS turns a scattered learning library into a connected place to retrieve evidence and decide what to study next.

## Desired reaction or action

The user should feel oriented rather than overwhelmed, then either continue a resource or ask a grounded question.

## Source authority

`KnowledgeOS_Pack`, the approved architecture audit, the existing React application, and the supplied design references.

## Content hierarchy

Home prioritizes Focus Next and Ask KnowledgeOS. Library prioritizes resource discovery. Resource Workspace prioritizes reading, notes, and source-specific questions. Insights stays lightweight and explainable.

## Visual territory

Editorial research workspace. Dark ink surfaces, parchment-white reading planes, restrained cobalt accent, cropped document imagery, and an asymmetric composition that adapts cleanly to a single column on mobile.

## Brand and system constraints

Use Playfair Display for selected display moments because the handoff explicitly specifies it and the product is editorial. Use Be Vietnam Pro for UI and body copy. Preserve React, TypeScript, Vite, React Router, and the existing authentication flow. Use existing Lucide icons for this migration pass rather than adding a second icon dependency.

## Anti-goals

No old GroupSync visual language. No generic three-card SaaS dashboard. No violet AI glows, faux product screenshots, excessive pills, or decorative metric widgets.

## Output contract

Implement real application pages and verify at desktop, tablet, and mobile widths. Maintain explicit loading, empty, error, focus, and reduced-motion behavior.

## Reference interpretation

Borrow typographic confidence, framing, crop, contrast, and visual rhythm from the references. Do not reuse their branding, exact layouts, or legacy GroupSync patterns.

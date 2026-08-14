# GroupSync Design System

## Product character

GroupSync feels like a focused personal planner that becomes more energetic in an active group.
The interface is calm enough for schedules and clear enough for organizers running a badminton
session. It uses purposeful colour and familiar controls instead of decorative effects.

## Visual direction

- **Genre:** modern-minimal with a community/productivity tone.
- **Anchor:** deep forest green for the GroupSync product layer.
- **Study:** muted indigo blue for concentration and planning.
- **Badminton:** warm orange for activity, results, and live group energy.
- **Surfaces:** warm off-white canvas, white content surfaces, quiet borders, restrained shadows.
- **Typography:** `Sora` for display hierarchy when available and `Be Vietnam Pro` for Vietnamese body copy; system sans-serif is the offline fallback.
- **Iconography:** one line-icon family only. Icons support labels; they never replace a label on primary actions.

## Layout and interaction

- Desktop uses a compact left rail; mobile uses a fixed bottom navigation with five destinations.
- Group pages own their contextual navigation rather than adding more global links.
- A primary action is visually singular in each local context.
- Forms open in a focused surface or modal/drawer; they do not permanently dominate long pages.
- Motion is limited to opacity and small transforms over 160--220 ms. Respect reduced motion.
- All interactive controls have visible keyboard focus and a minimum 44 px touch target on mobile.

## Component voice

- Buttons are solid, compact, and purposeful; destructive actions are explicitly red.
- Cards are used to group information, not to wrap every sentence.
- Statuses use text and colour together.
- Empty states explain the next useful action without inventing product metrics or testimonials.

## Operational workspace refinement

- Global navigation stays limited to Home, My Schedule, Groups, Notifications, and Profile.
- Group tools use contextual tabs: Overview, Group Availability, Activity, and Tournament where relevant.
- The primary journey is visible in the interface: My Schedule → Group Availability → Group Activity → Results / Progress.
- Home behaves as a command center: next activity, current group, attention, and recent updates lead.
- Study uses calm indigo surfaces and academic pacing; Badminton uses orange accents, lifecycle states, court visuals, and score-first hierarchy; Tournament uses deep green with restrained gold.
- Mobile shows the current task first, converts wide content into ranked or horizontal flows, and never requires the desktop sidebar.
- Lucide is the sole production icon family. Initial-based avatars remain the fallback when no image is supplied.

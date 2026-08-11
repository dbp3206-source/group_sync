# PeSoc Reference Notes — Learn From It, Do Not Clone It

## 1. Why PeSoc is useful
The reference repo demonstrates a friendly community sports product rather than an abstract enterprise system. Its overall code complexity is a useful ceiling/benchmark for GroupSync beginners.

Useful concepts to carry into GroupSync:
- player profile with long-term history;
- ranking/Elo history idea;
- upcoming and past matches;
- tournament lifecycle as optional extension;
- news and comments;
- notifications;
- homepage/dashboard with live-feeling content.

## 2. Structural style worth keeping
PeSoc uses familiar Spring layers:
- configuration;
- controller;
- model;
- repository;
- service.

GroupSync intentionally remains close to this mental model, adding only:
- DTOs for React REST contracts;
- strategies for OOP algorithms;
- events/scheduler for automation;
- exception package for consistent API errors.

## 3. Things GroupSync should improve
### Auth/security
Do not copy custom/plain password approaches. Use Spring Security + BCrypt and keep credentials out of source.

### Domain behavior
PeSoc models can be fairly data-centric. GroupSync should let important objects enforce simple state rules rather than allowing arbitrary setter-based transitions everywhere.

### Doubles badminton
Do not model a match as only `player1` vs `player2`. Use MatchSide/participants so doubles is natural.

### Group-scoped sports identity
Do not store one global sport rating directly on User. A user's badminton rating/stats belong to a badminton group/season.

### Service size
Avoid growing one giant service containing registration, file upload, ranking, notification and every rule. Split services by coherent feature while keeping the classic layered structure.

### Secrets/config
Never commit Firebase keys, DB passwords, JWT secrets or Google credentials.

## 4. Features NOT copied
- TikTok-like video feed.
- WebSocket infrastructure for core.
- Firebase push notifications for MVP.
- tournament as mandatory feature.
- one-v-one match assumptions.

## 5. Product lesson from PeSoc
A group-management app is more engaging if users can return to see:
- history;
- next activity;
- recent results;
- ranking changes;
- news;
- personal profile/stat progress.

GroupSync combines that lesson with its unique core:
**Personal calendar/availability -> Group operations -> History/community -> Next activity.**

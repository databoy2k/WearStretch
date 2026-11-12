# Manual test checklist — watch↔phone workout sync

The workout hand-off was reworked to use the Wearable **DataClient** (persistent,
auto-syncing) instead of `MessageClient` (transient). The phone owns the Health
Connect sync and acks by deleting the DataItem. These paths are build-verified
only and need on-device testing on a paired watch + phone.

## 1. Offline delivery + honest sync indicator (highest priority)
- [ ] Start and finish a workout with the **phone powered off**.
- [ ] On the watch, the history record should show **"Not sent yet"** (gray cloud).
- [ ] Power the phone on. Once it syncs, the watch record should flip to **green**.
- [ ] **If it stays gray** even though the workout reached Health Connect, the
      phone's DataItem deletion is not propagating back to the watch → switch to
      an explicit ack message (`/workout_ack/<id>`). This is the key unverified
      assumption.

## 2. No duplicates
- [ ] Finish a workout normally; confirm exactly **one** entry in Health Connect.
- [ ] Toggle the watch app (background/foreground) and relaunch a few times;
      confirm no extra copies appear (clientRecordId dedup).
- [ ] Use "Resend to Phone" on the history screen; confirm still one entry.

## 3. No data loss when phone is unreachable
- [ ] Finish a workout with the phone in airplane mode / out of range.
- [ ] Bring the phone back; confirm the workout lands in Health Connect (not lost).

## 4. Steps
- [ ] Take some steps during a workout; confirm the count is **> 0** on the watch
      (regression check — the detector must fire on short sessions).
- [ ] Confirm a `StepsRecord` appears in Health Connect and in other step apps.

## 5. Location / ExerciseRoute
- [ ] With location tracking on, finish a workout; confirm location shows as a
      point in apps that read exercise routes.
- [ ] **If HC rejects the route** on a manual-entry session, the fallback drops
      only the route and still saves the session + steps + HR (no failure). Note
      which behavior occurs.

## 6. Health Connect write failure is surfaced
- [ ] Revoke the phone app's Health Connect permission, then finish a workout.
- [ ] Confirm the phone shows the **"Stretch Not Saved"** notification and the
      watch record stays gray (item retained for retry).
- [ ] Confirm the phone app has notification permission (needed for both the
      success and failure notifications).

## 7. Settings sync watch → phone
- [ ] Change a tracking setting on the watch; confirm it reflects on the phone
      (this path previously never fired — the phone had no DATA_CHANGED filter).

## 8. Process-death restart guard
- [ ] (Hard to force) After a mid-workout process kill, confirm no bogus
      "Unknown" empty workout is created.

---

# Manual test checklist — home-stack features (branch feature/home-stack)

Build-verified and matcher unit-tested only; needs a paired watch + phone.

## 9. Me tab shows only Wear Stretch stretches
- [ ] Log a workout in another app that writes to Health Connect (e.g. openGym strength).
- [ ] The phone's **Me → Recent Sessions** must NOT list it; Wear Stretch sessions still show.

## 10. Cool-down suggestion
- [ ] On first launch after updating, Health Connect asks for **background read** (if offered on the device). Allow it.
- [ ] Finish a 10+ minute workout in openGym titled e.g. "Leg day" (Health Connect sync on in openGym).
- [ ] Within ~30 min (or immediately when opening the phone app) a **"Time to stretch"** notification suggests a lower-body routine.
- [ ] Tap **Start on watch**: the watch app opens with a highlighted **Suggested after Leg day** card at the top. Tapping it starts the routine.
- [ ] Stretch first, then open the app: no notification for that workout.
- [ ] Settings → **Cool-down Suggestions** off: no more notifications.
- [ ] Without the background-read permission, suggestions only appear when the phone app is opened.

## 11. Tile + complication
- [ ] Add the **Wear Stretch** tile: shows streak, minutes today, and "Again: <last routine>" (or "Cool-down: …" right after a suggestion).
- [ ] Tile button opens the app with that routine pinned as Suggested.
- [ ] Add the **Stretch streak** complication (short text) to a watch face: shows e.g. "3d"; updates after finishing a workout.

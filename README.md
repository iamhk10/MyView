# MyView

Habits, workouts and water in one free Android app. No internet access, no ads, all data stays on the phone.

**Features (v1.1.0)**
- Habit tracker with streaks, best streak, 12-week heatmap, weekday schedule
- Workout counter: push-ups, pull-ups, squats and more (reps), plank and more (timer), custom exercises, daily goals, 7-day chart
- Water tracker: daily goal, quick add, custom amount, history, 7-day chart
- Step counter (only on phones with a step sensor; hidden otherwise), daily goal, 7-day chart; optional live counting service keeps the count and widget updating within seconds
- Stats and history: every day stored, bar or line chart over 7/30/90 days, highest, lowest, average and total, day-by-day list (steps, water, workout, habits, sleep, weight)
- Reminders: per habit, water (every 1-6 h), daily workout; notification buttons "Mark done" and "+ ml"
- Home-screen widgets: Habits (tap to tick), Water (quick add), Summary (steps, water, reps, habits)
- Sleep log with quality stars and goal, weight log with trend chart and kg/lb, mood diary with 4-week map
- Backup and restore of all data as one JSON file (no internet needed)
- Premium design: daily score dashboard, animated rings, water wave, confetti on goals, springy navigation
- Themes: Auto/Light/Dark/AMOLED, 9 accent colours, wallpaper colours, liquid glass, 6 font styles, text size
- Adaptive launcher icon with Android 13 themed (monochrome) version

## Build the APK (GitHub Actions)
1. Create a new GitHub repository (public, so anyone can download).
2. Upload everything from this folder to it (keep the `.github` folder).
3. Open the **Actions** tab. The workflow "Build MyView APK" runs on every push; download `MyView-apk` from the finished run.

## Release with a direct download link
1. Repo Settings > Actions > General > Workflow permissions > **Read and write**.
2. Create a tag: `git tag v0.1.0 && git push origin v0.1.0` (or Releases > Draft a new release with tag `v0.1.0`).
3. The workflow attaches `MyView.apk` to the release.
4. Direct link: `https://github.com/<user>/<repo>/releases/latest/download/MyView.apk`

The app is signed with the committed `app/myview-debug.keystore`, so a new version installs over the old one.

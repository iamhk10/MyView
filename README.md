# MyView

Habits, workouts and water in one free Android app. No internet access, no ads, all data stays on the phone.

**v0.1.0 features**
- Habit tracker with streaks, best streak, 12-week heatmap, weekday schedule
- Workout counter: push-ups, pull-ups, squats and more (reps), plank and more (timer), custom exercises, daily goals, 7-day chart
- Water tracker: daily goal, quick add, custom amount, history, 7-day chart
- Themes: Auto/Light/Dark/AMOLED, accent colours, wallpaper colours, liquid glass

Coming next: step counter, reminders, home-screen widgets, then sleep and weight logs.

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

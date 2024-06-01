# Introduction
Absolute Tomato is a Pomodoro timer. It consists of two configurable timers called "Focus" and "Relax" while only one is running at a time. Starting one timer automatically stops and resets the other timer. They continue to run even if the app is in background. As soon as the either of the timers expires a notification is emitted. The timer configuration is persistent even if the app is stopped but not the timers.

# Test Scenario

## Configuration

Open app and use slider to configure Focus interval to 5 Minutes and Relax interval to 1 Minutes. Also set Notification interval to 10 Seconds.
Check that timer counters below the upper two buttons have updated to "5m00s" and "1m00s" 

## Timer toggling

Tap button "Start Timer". The first timer starts to count down.
Use the Focus interval to increase the timeout and watch how the running timer updates accordingly.
Tap button Relax. The first timer stops to count down and the second timer starts.
Use the Relax interval to increase the timeout and watch how the running timer updates accordingly.
Tap button Focus and the both timers resets and the first starts again. Set the Focus interval back to 5 Minutes

## Background and Notification

Use the Android Navigation to switch to the Android Home Screen and push Absolute Tomato into background.
Wait for the five minutes to expire timer and observe the notification coming up. Wait for the notification interval and observe repetition of the notification.
Navigate back to the Absolute Tomato app.
Tap button Relax and observe the notification disappear and the lower timer starts. Set the Relax interval back to 1 Minutes.
Wait for the second timer to expire and observe the notification coming up.
Tap button Stop. The notification disappears and both timer are stopped and reset.

## Persistance of configuration

Press button Start Timer and observe the timer starts.
Navigate to the Android Home Screen.
Tap the Android Recent Apps button (also known as Overview screen) and stop the Absolute Tomato app by swiping it up (or down on some Android devices). The app is removed from the Recents list.
Open the Absolute Tomato app again. The timer has stopped but the Focus interval has still the 5 Minutes configured and the Relax interval has 1 Minutes.




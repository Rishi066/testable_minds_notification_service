**!!!For educational purposes only. This content is intended solely for learning and informational purposes and should not be considered professional advice or a recommendation to take any action.!!!**


# Testable Minds Study Notifier

Watches your Testable Minds dashboard and pops up a desktop notification the moment a new study becomes available, instead of waiting on their delayed notifications :)

## Requirements



- Java 17 or higher installed
- Maven installed

Check if you already have them:

```
java -version
mvn -version
```

If not installed:
- Java: https://adoptium.net/
- Maven: https://maven.apache.org/download.cgi

## Setup 

1. Copy this whole project folder to your computer (download as zip & unzip it (OR) git clone).

2. Get your session cookies for minds.testable.org by running from the project folder:
```
mvn compile exec:java -Dexec.mainClass=com.notifier.LoginSetup  
```

     You'll be asked to pick a login method:

   > Choose **[1] Sign in via Credentials** if you signed up with email + password
   - A browser window opens automatically. Log in normally in that window, then come back to the terminal and press ENTER.

   > Choose **[2] Sign in via Google/Facebook** if that's how you signed up
   - Install the [Cookie-Editor](https://chromewebstore.google.com/detail/cookie-editor/hlkenndednhfkekhgcdicdfddnkalmdm?hl=en) browser extension (free & open source).
   - Log into minds.testable.org normally in your everyday browser.
   - Open Cookie-Editor on that tab, find the `testable_minds_session` cookie, copy its value.
   - Paste it into the terminal when prompted.

## Running it

From the project folder:

```
mvn compile exec:java -Dexec.mainClass=com.notifier.TestableNotifier
```

Leave this running in the background. It checks the studies page every 40 seconds (randomized) and will show a popup notification when a new study appears.

To stop it, press Ctrl+C in the terminal.

## When cookies expire

If you see `Session expired` in the console, your login session ran out. Just repeat the LoginSetup step above.

## Notes

For Feedback/Issue Report: https://forms.gle/tfaSCbboQf7fHjcJ8

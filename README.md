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

2. Get your session cookies for minds.testable.org:  

  > Follow below steps if you signed up via **Google/Facebook**
   - Install the [Cookie-Editor](https://chromewebstore.google.com/detail/cookie-editor/hlkenndednhfkekhgcdicdfddnkalmdm?hl=en) browser extension. (Cookie-Editor is Free & Open Source)
   - Run LoginSetup.java file.
   - Open Cookie-Editor on the tab you logged in to Testable, copy "testable_minds_session" cookie and paste it into terminal.  

  > Follow below steps if you signed up with **Credentials (Email and Password)**
  - Run LoginSetup.java file. Follow the displayed instructions.

## Running it

From the project folder:

```
mvn compile exec:java -Dexec.mainClass=com.notifier.TestableNotifier
```

Leave this running in the background. It checks the studies page every 1-2 minutes (randomized) and will show a popup notification when a new study appears.

To stop it, press Ctrl+C in the terminal.

## When cookies expire

If you see `Session expired` in the console, your login session ran out. Just repeat the cookie export step above.

## Notes

For Feedback/Issue Report: https://forms.gle/tfaSCbboQf7fHjcJ8

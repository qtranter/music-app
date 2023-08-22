# Audiomack

![Build Status](https://app.bitrise.io/app/a7c71d187e4fda61/status.svg?token=7oMETppjsU5o3mqsn7QQkA&branch=development) [![codecov](https://codecov.io/gh/audiomack/audiomack-android/branch/development/graph/badge.svg?token=UktL09wA18)](https://codecov.io/gh/audiomack/audiomack-android)

[Android app on Google Play](https://play.google.com/store/apps/details?id=com.audiomack)

## Project setup

### Requirements
- [Android Studio 4.0.0 or higher](https://developer.android.com/studio/index.html)
- [JDK 1.8](https://www.google.it/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0ahUKEwjCo8mYzbTZAhXBbxQKHaRLBjQQFggnMAA&url=http%3A%2F%2Fwww.oracle.com%2Ftechnetwork%2Fjava%2Fjavase%2Fdownloads%2Fjdk8-downloads-2133151.html&usg=AOvVaw27mFVHV9M4wo4ENQuM77C5)
- [Android SDK 10 (API level 29)](https://developer.android.com/studio/index.html)
- Android SDK build tools (API level 29)
- Android SDK tools (API level 29)
- Android support library (API level 29)
- Android repository (API level 29)
- [Kotlin plugin for Android Studio 1.4.0 or higher](https://kotlinlang.org/docs/tutorials/kotlin-android.html)

### Getting started
#### Secrets
All the sensible keys and secrets are read from a file named secrets.properties and placed at the root of the project.
This file is added to the .gitignore list on purpose.
Please ask someone in the team for the content of such file.
In case such file is not present on your local setup you'll see a message similar to the following:
> "secrets.properties file not found. If you are running the project locally please ask your team for instructions."
#### Git hooks
Please add the following pre-commit hook on your machine: this will prevent breaking builds on the CI by enforcing the ktLint check before committing any changes to the codebase.
~~~~
#!/bin/sh
echo "Running static code analysis..."
./gradlew AM:ktlint --daemon
status=$?
if [ "$status" = 0 ] ; then
    echo "...everything is ok!"
    exit 0
else
    echo 1>&2 "...found some errors, commit aborted. Please run ./gradlew ktlintFormat and fix any additional errors."
    exit 1
fi
~~~~
#### Code style
The code style definition follows the [official Kotlin style guide](https://developer.android.com/kotlin/style-guide) with the following changes/additional rules not mentioned there:
- no copyright notice is included at the top of files created by the Audiomack team;
- all comments must be written in English, so they follow the written English rules.

## Important wiki pages
* [Localization](https://github.com/audiomack/audiomack/wiki/App-Localization)
* [Mobile Branching Process](https://github.com/audiomack/audiomack/wiki/Mobile-Branching-Strategy)
* [Release Train Process](https://github.com/audiomack/audiomack/wiki/Mobile-Release-Train-Process)
* [Mobile CI Setup](https://github.com/audiomack/audiomack/wiki/Mobile-CI-setup)
* [Push Notifications](https://github.com/audiomack/audiomack/wiki/Mobile-App-Push-Notifications)
* [Deeplinks](https://github.com/audiomack/audiomack/wiki/Mobile-app-deeplinks)
* [Ads](https://github.com/audiomack/audiomack/wiki/Mobile-App-Ads)
* [Unit testing](https://github.com/audiomack/audiomack/wiki/Android-app-unit-testing)
* [Browse all wiki](https://github.com/audiomack/audiomack/wiki#mobile-app)

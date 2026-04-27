#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
    mkdir -p "$APP_HOME/gradle/wrapper"
    if command -v curl > /dev/null 2>&1; then
        curl -sL -o "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
    fi
fi

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

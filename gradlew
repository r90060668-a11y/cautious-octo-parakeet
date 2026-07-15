#!/bin/sh
APP_HOME=$(dirname "$0")
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null
JAVA_EXE="${JAVA_HOME}/bin/java"
[ -z "$JAVA_HOME" ] && JAVA_EXE="java"
exec "$JAVA_EXE" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"

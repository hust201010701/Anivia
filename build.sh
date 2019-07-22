# ./gradlew :generate-patch-plugin:uploadArchives
# ./gradlew :patch-entry-plugin:uploadArchives
./gradlew -q clean
./gradlew assembleDebug --stacktrace --info

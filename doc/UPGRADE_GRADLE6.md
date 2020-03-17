# Upgrading Gradle, fast and slow

Upgrading Gradle is one of the simplest thing you can do to make your build faster and more reliable.

Follow the guide

# The fastest way to upgrade Gradle

If you are feeling lucky, this is the fastest way to upgrade Gradle:

Open https://docs.gradle.org/current/userguide/userguide.html

[![https://user-images.githubusercontent.com/459464/75775525-729fd400-5d52-11ea-82a2-b2f8e73088ec.png]()](https://docs.gradle.org/current/userguide/userguide.html)

Here you find that the latest available version is, for example, 6.2.1

Now you can upgrade Gradle with this command:

`$ ./gradlew wrapper --gradle-version 6.2.1`

If you have an Android project, also make sure to upgrade the **Android Gradle plugin**.

Hint: It's the same version as the [latest release of Android Studio](https://developer.android.com/studio/)

```diff
// build.gradle
buildscript {
    dependencies {
-        classpath 'com.android.tools.build:gradle:3.x.x'
+        classpath 'com.android.tools.build:gradle:3.6.1'
    }
}
```

If your project sync, that's all what you have to do.

If it doesn't work, do a `git revert` and follow the steps below.

# The surest way to upgrade Gradle

Gradle follows a [semantic versioning](https://semver.org/) which means that in practice upgrading a minor version is usually a non-brainer.

If however you upgrade to a major version of Gradle, you need to make sure first that the plugins applied to your build are not using deprecated feature. The easiest way to do it is to use the Gradle Build Scan - also known as the Gradle Enterprise Plugin

{% post https://dev.to/jmfayard/the-one-gradle-trick-that-supersedes-all-the-others-5bpg %}

Try running `$ gradle help --scan` and view the deprecations view of the generated build scan. If there are no warnings, the **Deprecations** tab will not appear.

```
$ gradle help --scan`
> Task :help
(....)
BUILD SUCCESSFUL

Publishing a build scan to scans.gradle.com requires accepting the Gradle Terms of Service defined at https://gradle.com/terms-of-service. Do you accept these terms? [yes, no]

Publishing build scan...
https://gradle.com/s/dztmvas5c26no
```

Note: If you have a **really** old version of Gradle, first upgrade to the latest version of Gradle 4

`$ ./gradlew wrapper --gradle-version 4.10.3`



# Issues

This is a work in progress used to discover issues with setting up refreshVersions and dependencies

See my attempts here

https://github.com/jmfayard/pentagame/commits/jmfayard-refreshVersions

https://github.com/jmfayard/k-9/commits/jmfayard-refreshVersions

# Links

refreshVersions expect dependencies with a version placeholder _
https://github.com/jmfayard/refreshVersions/issues/160

Upgrading an old project to Gradle 6 and AGP 3.5.3 #156
https://github.com/jmfayard/refreshVersions/issues/156

Configuring Gradle with "gradle.properties"
https://dev.to/jmfayard/configuring-gradle-with-gradle-properties-211k

Upgrading your build from Gradle 5.x to 6.0
https://docs.gradle.org/current/userguide/upgrading_version_5.html

Upgrading your build from Gradle 4.x to 5.0
https://docs.gradle.org/current/userguide/upgrading_version_4.html

Gradle plugin (build-scan) manual
https://docs.gradle.com/enterprise/gradle-plugin/

# How to install refreshVersions

# Update to Gradle 6

```
$ ./gradle --scan help
Deprecated Gradle features were used in this build, making it incompatible with Gradle 6.0.

$ ./gradle wrapper --gradle-version 4.10.2
$ ./gradle wrapper --gradle-version 5.6.4
$ ./gradle wrapper --gradle-version 6.2

```

Look at [the deprecations view of the generated build scan](https://gradle.com/enterprise/releases/2018.4). If there are no warnings, the Deprecations tab will not appear. Deprecated usages information requires Gradle 4.10+ and build scan plugin 1.16+.

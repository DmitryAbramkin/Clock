# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Room entities / database are referenced by generated code.
-keep class com.olivka.clock.data.** { *; }

# AppWidgetProvider is instantiated by the system from the manifest.
-keep class com.olivka.clock.widget.** { *; }

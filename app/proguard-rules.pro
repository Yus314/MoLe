# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================
# Kotlinx Serialization (R8 fullMode support)
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# MoLe JSON serializable classes
-keep,includedescriptorclasses class net.ktnx.mobileledger.json.**$$serializer { *; }
-keepclassmembers class net.ktnx.mobileledger.json.** {
    *** Companion;
}
-keepclasseswithmembers class net.ktnx.mobileledger.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# MoLe Backup model serializable classes
-keep,includedescriptorclasses class net.ktnx.mobileledger.backup.model.**$$serializer { *; }
-keepclassmembers class net.ktnx.mobileledger.backup.model.** {
    *** Companion;
}
-keepclasseswithmembers class net.ktnx.mobileledger.backup.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

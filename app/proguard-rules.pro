# Project specific ProGuard / R8 rules for Phone
# Keep Compose and ViewModel classes if minification is enabled in future versions
-keepattributes *Annotation*
-dontwarn java.lang.invoke.**

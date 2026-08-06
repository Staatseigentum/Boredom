# kotlinx.serialization keeps the generated serializers referenced only reflectively.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.staatseigentum.kollaps.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.staatseigentum.kollaps.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.staatseigentum.kollaps.core.**$$serializer { *; }

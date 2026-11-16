# MyView R8 rules. Room and Compose ship their own consumer rules.
-keep class com.x.myview.MyViewApp { *; }
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.jspecify.**
# ViewModels are created reflectively by the default factory (MainViewModel(Application)).
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

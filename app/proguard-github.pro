# GitHub flavor only — ML Kit barcode (bundled). F-Droid never merges this file.
# Consumer rules cover most keeps; these cover reflective / Task paths used by the decoder.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

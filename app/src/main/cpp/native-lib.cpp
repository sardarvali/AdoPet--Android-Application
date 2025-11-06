#include <jni.h>
#include <string>

// WARNING: These are PLACEHOLDER values
// YOU MUST REPLACE these with your actual base64-encoded keys after generating them
// See KEY_ENCRYPTION_GUIDE.md for instructions
// The keys are base64 encoded to avoid encoding issues

extern "C" JNIEXPORT jstring JNICALL
Java_com_syed_security_SecureKeys_getGoogleMapsSecretKey(JNIEnv* env, jobject /* this */) {
    // Replace this with your base64-encoded 32-byte secret key from encryption process
    std::string secretKeyBase64 = "9rck57blamPN5d3I4s+5vawbmLv8ILcOexX6VdJ40GY=";
    return env->NewStringUTF(secretKeyBase64.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_syed_security_SecureKeys_getGoogleMapsIv(JNIEnv* env, jobject /* this */) {
    // Replace this with your base64-encoded 16-byte IV from encryption process
    std::string ivBase64 = "GY6FJUNRqaN5YE2MHKEDyA==";
    return env->NewStringUTF(ivBase64.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_syed_security_SecureKeys_getGeminiSecretKey(JNIEnv* env, jobject /* this */) {
    // Replace this with your base64-encoded 32-byte secret key for Gemini API
    std::string secretKeyBase64 = "ICAwGOqFjH4jOAc5SjkMHiMOQkd6ushm8yaWj14WZlY=";
    return env->NewStringUTF(secretKeyBase64.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_syed_security_SecureKeys_getGeminiIv(JNIEnv* env, jobject /* this */) {
    // Replace this with your base64-encoded 16-byte IV for Gemini API
    std::string ivBase64 = "5r5LuV0FYxP98m7PKCxSWg==";
    return env->NewStringUTF(ivBase64.c_str());
}

// Obfuscation helper - makes reverse engineering harder
extern "C" JNIEXPORT jstring JNICALL
Java_com_syed_security_SecureKeys_getAppSalt(JNIEnv* env, jobject /* this */) {
    std::string salt = "PetAdoption2025SecureSalt!@#$%";
    return env->NewStringUTF(salt.c_str());
}


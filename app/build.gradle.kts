
// plugins {
//     id("com.android.application")
// }

// android {
//     namespace = "com.example.defaultsmsapp"
//     compileSdk = 34

//     defaultConfig {
//         applicationId = "com.example.defaultsmsapp"
//         minSdk = 29
//         targetSdk = 34
//         versionCode = 1
//         versionName = "1.0"
//         testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//     }

//     buildTypes {
//         getByName("release") {
//             isMinifyEnabled = false
//             proguardFiles(
//                 getDefaultProguardFile("proguard-android-optimize.txt"),
//                 "proguard-rules.pro"
//             )
//         }
//     }

//     compileOptions {
//         sourceCompatibility = JavaVersion.VERSION_1_8
//         targetCompatibility = JavaVersion.VERSION_1_8
//     }
//     buildToolsVersion = "34.0.0"
//     ndkVersion = "27.1.12297006"
// }

// dependencies {
//     implementation("androidx.appcompat:appcompat:1.6.1")
//     implementation("com.google.android.material:material:1.10.0")
//     implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//     implementation("androidx.recyclerview:recyclerview:1.3.2")
//     implementation("androidx.core:core:1.12.0")

//     testImplementation("junit:junit:4.13.2")
//     androidTestImplementation("androidx.test.ext:junit:1.1.5")
//     androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
// }

// default sms app k  liye ok 

plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.defaultsmsapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.defaultsmsapp"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildToolsVersion = "34.0.0"
    ndkVersion = "27.1.12297006"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core:1.12.0")
    
    // SwipeRefreshLayout for pull-to-refresh functionality
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Work Manager for background tasks (alternative approach)
    implementation("androidx.work:work-runtime:2.8.1")
    
    // Lifecycle components for better service management
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
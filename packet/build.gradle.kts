
plugins {
    id ("com.android.library")
    id ("kotlin-android")
    id ("maven-publish")
}

android {
    compileSdk = 33

    defaultConfig {

        minSdk = 21
        targetSdk = 33
        vectorDrawables.useSupportLibrary = true

    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation ("androidx.core:core-ktx:1.10.1")
}

afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release
                groupId = "com.github.walaankit"
                artifactId = "Packet"
                version = "0.1.1"
            }
        }
    }
}
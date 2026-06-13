import java.net.URL
import java.net.URI
import java.net.HttpURLConnection
import java.security.cert.X509Certificate
import java.security.SecureRandom
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import javax.net.ssl.HttpsURLConnection

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.enox.enoxpay"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.enox.enoxpay.bmdxyz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      // Enable minification and resource shrinking for release (strong obfuscation)
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.androidx.work.runtime.ktx)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("uploadApk") {
  doLast {
    val rootDirFile = file("${rootDir}/app-release.apk")
    val buildDirFile = file("${projectDir}/build/outputs/apk/release/app-release.apk")
    val apkFile = if (rootDirFile.exists()) rootDirFile else if (buildDirFile.exists()) buildDirFile else null

    if (apkFile == null || !apkFile.exists()) {
      throw GradleException("APK file not found! Please compile with 'gradle :app:assembleRelease' first.")
    }

    println("Found APK file: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
    
    // Bypass SSL Verification
    try {
      val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
        override fun checkClientTrusted(certs: Array<X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(certs: Array<X509Certificate>?, authType: String?) {}
      })
      val sc = SSLContext.getInstance("SSL")
      sc.init(null, trustAllCerts, SecureRandom())
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
      HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
      println("SSL verification bypassed successfully.")
    } catch (e: Exception) {
      println("Failed to bypass SSL verification: ${e.message}")
    }

    var success = false

    // Attempt 1: bashupload.com (with SSL bypassed)
    println("Attempting upload to bashupload.com...")
    try {
      val url = URI("https://bashupload.com/app-debug.apk").toURL()
      val connection = url.openConnection() as HttpURLConnection
      connection.doOutput = true
      connection.requestMethod = "PUT"
      connection.connectTimeout = 15000
      connection.readTimeout = 30000
      connection.setRequestProperty("Content-Type", "application/vnd.android.package-archive")
      connection.setRequestProperty("Content-Length", apkFile.length().toString())

      apkFile.inputStream().use { input ->
        connection.outputStream.use { output ->
          input.copyTo(output)
        }
      }

      if (connection.responseCode in 200..299) {
        val downloadUrl = connection.inputStream.bufferedReader().use { it.readText() }.trim()
        println("\n====================================================")
        println("SUCCESS (bashupload.com) Download Link:")
        println(downloadUrl)
        println("====================================================\n")
        success = true
      } else {
        println("bashupload.com responded with code: ${connection.responseCode}")
      }
    } catch (e: Exception) {
      println("bashupload.com upload failed: ${e.message}")
    }

    // Helper for Multipart uploading
    val uploadMultipart = { urlStr: String, fileParamName: String ->
      var resultLink: String? = null
      try {
        val boundary = "===Boundary" + System.currentTimeMillis() + "==="
        val url = URI(urlStr).toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.doOutput = true
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        conn.connectTimeout = 20000
        conn.readTimeout = 45000
        
        conn.outputStream.use { out ->
          val writer = out.bufferedWriter()
          writer.write("--$boundary\r\n")
          writer.write("Content-Disposition: form-data; name=\"$fileParamName\"; filename=\"${apkFile.name}\"\r\n")
          writer.write("Content-Type: application/vnd.android.package-archive\r\n\r\n")
          writer.flush()
          
          apkFile.inputStream().use { input ->
            input.copyTo(out)
          }
          
          writer.write("\r\n--$boundary--\r\n")
          writer.flush()
        }
        
        if (conn.responseCode in 200..299) {
          resultLink = conn.inputStream.bufferedReader().use { it.readText() }.trim()
        } else {
          println("POST to $urlStr returned code: ${conn.responseCode}")
        }
      } catch (e: Exception) {
        println("Multipart POST upload to $urlStr failed: ${e.message}")
      }
      resultLink
    }

    // Attempt 2: tmpfiles.org
    println("Attempting upload to tmpfiles.org...")
    try {
      val responseText = uploadMultipart("https://tmpfiles.org/api/v1/upload", "file")
      if (responseText != null && responseText.contains("\"url\":")) {
        // Parse the URL from JSON: "url":"https://tmpfiles.org/xxxxx/app-debug.apk"
        val regex = "\"url\":\"([^\"]+)\"".toRegex()
        val matchResult = regex.find(responseText)
        val rawUrl = matchResult?.groups?.get(1)?.value
        if (rawUrl != null) {
          // Replace tmpfiles.org with tmpfiles.org/dl for direct download link
          val dlUrl = rawUrl.replace("tmpfiles.org/", "tmpfiles.org/dl/")
          println("\n====================================================")
          println("SUCCESS (tmpfiles.org) View Link:")
          println(rawUrl)
          println("SUCCESS (tmpfiles.org) Direct Download Link:")
          println(dlUrl)
          println("====================================================\n")
          success = true
        }
      } else if (responseText != null) {
        println("tmpfiles.org response: $responseText")
      }
    } catch (e: Exception) {
      println("tmpfiles.org parsing / upload failed: ${e.message}")
    }

    // Attempt 3: file.io
    println("Attempting upload to file.io...")
    try {
      val responseText = uploadMultipart("https://file.io", "file")
      if (responseText != null && responseText.contains("\"link\":")) {
        val regex = "\"link\":\"([^\"]+)\"".toRegex()
        val matchResult = regex.find(responseText)
        val linkUrl = matchResult?.groups?.get(1)?.value
        if (linkUrl != null) {
          println("\n====================================================")
          println("SUCCESS (file.io) Single-use Download Link:")
          println(linkUrl)
          println("====================================================\n")
          success = true
        }
      } else if (responseText != null) {
        println("file.io response: $responseText")
      }
    } catch (e: Exception) {
      println("file.io parsing / upload failed: ${e.message}")
    }

    if (!success) {
      throw GradleException("Failed to upload the APK to any host.")
    }
  }
}

tasks.register<Zip>("createFullZip") {
    archiveFileName.set("FullProjectSource.zip")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/zip"))
    from(rootDir) {
        exclude("**/build/**", "**/.gradle/**", "**/.idea/**", "**/.build-outputs/**")
    }
}



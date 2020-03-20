plugins {
    java
    id("com.github.rodm.teamcity-server") version "1.2.2"
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.apache.httpcomponents:httpclient:4.5.10")
    implementation("com.google.code.gson:gson:2.8.6")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = sourceCompatibility
}

teamcity {
    version = "2018.2"
    server {
        descriptor {
            name = project.name
            displayName = "Chime notify"
            version = project.version.toString()
            vendorName = "Magnus Jason"
            vendorUrl = "https://github.com/magJ/teamcity-chime-notify"
            description = "Sends build notifications to a chime webhook"
            downloadUrl = vendorUrl
            useSeparateClassloader = true
            allowRuntimeReload = true
        }
    }
}
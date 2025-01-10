pluginManagement {
	repositories {
		mavenCentral();
		gradlePluginPortal();
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0";
}

// rootProject.name = properties.project_name;

include(":lib");
include(":common");
include(":repl");
include(":runtime");
include(":compilation");
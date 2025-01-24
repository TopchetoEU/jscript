pluginManagement {
	repositories {
		mavenCentral();
		gradlePluginPortal();
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0";
}

rootProject.name = extra.properties["project_name"].toString();

include(":lib");
include(":common");
include(":repl");
include(":runtime");
include(":compilation");

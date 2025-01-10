plugins {
	id("base");
}

version = properties["project_version"].toString();
group = properties["project_group"].toString();
description = "ES5-compliant JavaScript interpreter";

tasks.wrapper {
	gradleVersion = "8.10";
}

tasks.build {
	subprojects.forEach { proj ->
		dependsOn(proj.tasks.named("build"));
		doLast {
			copy {
				from(proj.buildDir.resolve("libs"));
				into("$buildDir/libs");
			}
		}
	}
}
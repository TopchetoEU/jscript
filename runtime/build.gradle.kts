plugins {
	id("common-java");
}

description = "The runtime of J2S, used to execute J2S bytecode";

tasks.processResources {
	filesMatching("metadata.json", {
		expand(
			"version" to properties["project_version"],
			"name" to properties["project_name"],
		);
	});
}

tasks.test {
	useJUnitPlatform();
}

dependencies {
	implementation(project(":common"));
}

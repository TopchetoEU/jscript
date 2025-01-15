plugins {
	id("common-java");
}

description = "A collection of utils and structures for the rest of the project";

tasks.processResources {
	filesMatching("metadata", {
		expand(
			"version" to properties["project_version"],
			"name" to properties["project_name"],
		);
	});
}

tasks.test {
	useJUnitPlatform();
}

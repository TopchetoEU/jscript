plugins {
	id("common-java");
}

description = "A compiler of EcmaScript 5 code to J2S bytecode";

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

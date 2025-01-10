import com.github.gradle.node.npm.task.NpmTask;

plugins {
	id("common");
	id("com.github.node-gradle.node") version "5.0.0";
}

tasks.compileJava {
	enabled = false;
}
tasks.classes {
	enabled = false;
}

node {
	version = "20.0.0";
	npmVersion = "8.0.0";
	download = true;
}

tasks.register<NpmTask>("compileStdlib") {
	dependsOn("npmInstall");

	inputs.files("rollup.config.js");
	inputs.dir("src/stdlib");
	outputs.files("build/js/stdlib.js");

	args.set(listOf("run", "build-env"));
}
tasks.register<NpmTask>("compileTranspiler") {
	dependsOn("npmInstall");

	inputs.files("rollup.config.js");
	inputs.dir("src/transpiler");
	outputs.files("build/js/transpiler.js");
	// nom nom tasty ram
	environment.put("NODE_OPTIONS", "--max-old-space-size=4096");

	args.set(listOf("run", "build-ts"));
}

tasks.jar {
	manifest {
		attributes(
			"Main-Class" to properties["main_class"].toString(),
			"Build-Author" to "TopchetoEU",
		);
	}
}

tasks.processResources {
	dependsOn("compileStdlib");
	dependsOn("compileTranspiler");

	from("build/js") {
		into("lib");
	}
	from("src/lib") {
		into("lib");
	}

	filesMatching("metadata.json", {
		expand(
			"version" to properties["project_version"].toString(),
			"name" to properties["project_name"].toString(),
		);
	})
}

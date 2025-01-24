import com.github.gradle.node.npm.task.NpmTask;

plugins {
	id("common-java");
	id("com.github.node-gradle.node") version "5.0.0";
}

dependencies {
	implementation(project(":common"));
	implementation(project(":compilation"));
	implementation(project(":runtime"));
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
tasks.register<NpmTask>("compileBabel") {
	dependsOn("npmInstall");

	inputs.files("rollup.config.js");
	inputs.dir("src/transpiler");
	outputs.files("build/js/babel.js");
	// nom nom tasty ram
	environment.put("NODE_OPTIONS", "--max-old-space-size=4096");

	args.set(listOf("run", "build-babel"));
}
tasks.register<NpmTask>("compileTypescript") {
	dependsOn("npmInstall");

	inputs.files("rollup.config.js");
	inputs.dir("src/transpiler");
	outputs.files("build/js/typescript.js");
	// nom nom tasty ram
	environment.put("NODE_OPTIONS", "--max-old-space-size=4096");

	args.set(listOf("run", "build-typescript"));
}
tasks.register<NpmTask>("compileCoffee") {
	dependsOn("npmInstall");

	inputs.files("rollup.config.js");
	inputs.dir("src/transpiler");
	outputs.files("build/js/coffee.js");
	// nom nom tasty ram
	environment.put("NODE_OPTIONS", "--max-old-space-size=4096");

	args.set(listOf("run", "build-coffee"));
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
	dependsOn("compileTypescript");
	dependsOn("compileBabel");
	dependsOn("compileCoffee");

	from("build/js") {
		into("lib");
	}
	from("src/lib") {
		into("lib");
	}
}

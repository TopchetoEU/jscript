plugins {
	id("j2s-common-java");
	id("com.gradleup.shadow") version "9.0.0-beta4";
}

description = "A simple REPL for the interpreter, can be used for simple prototyping";

tasks.test {
	useJUnitPlatform();
}

tasks.jar {
	dependsOn("shadowJar");

	manifest {
		attributes(
			"Main-Class" to properties["main_class"],
			// 'Build-Timestamp': new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()),
			// 'Build-Branch': versioning.info.branch,
			// 'Build-Revision': versioning.info.commit,
			// 'Build-Jdk': "${System.properties['java.version']} (${System.properties['java.vendor']} ${System.properties['java.vm.version']})",
			// 'Build-Author': 'TopchetoEU',
		);
	}
}

dependencies {
	implementation(project(":common"));
	implementation(project(":compilation"));
	implementation(project(":runtime"));
	implementation(project(":lib"));
}

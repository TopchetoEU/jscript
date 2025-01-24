plugins {
	id("common-java");
}

description = "The runtime of J2S, used to execute J2S bytecode";

tasks.test {
	useJUnitPlatform();
}

dependencies {
	implementation(project(":common"));
}

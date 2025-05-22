plugins {
	id("j2s-common-java");
}

description = "A compiler of EcmaScript 5 code to J2S bytecode";

tasks.test {
	useJUnitPlatform();
}

dependencies {
	implementation(project(":common"));
}

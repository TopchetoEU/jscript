plugins {
	id("common");
}

java {
	sourceCompatibility = JavaVersion.VERSION_17;
	targetCompatibility = JavaVersion.VERSION_17;

	toolchain {
		languageVersion = JavaLanguageVersion.of(17);
	}

	withJavadocJar();
	withSourcesJar();
}

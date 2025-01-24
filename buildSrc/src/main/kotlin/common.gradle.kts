plugins {
	id("java");
	id("maven-publish");
}

version = rootProject.version;
group = rootProject.group;
base.archivesName = "${properties["project_name"]}-${project.name}";

tasks.named<JavaCompile>("compileJava") {
	options.release.set(8);
}

repositories {
	mavenCentral();
}

dependencies {
	annotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:0.4.2");
	compileOnly("com.github.bsideup.jabel:jabel-javac-plugin:0.4.2");
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2");
	testRuntimeOnly("org.junit.platform:junit-platform-launcher");
}


publishing {
	repositories {
		maven {
			name = "Gitea";
			url = uri(System.getenv("REPO_URL") ?: "");

			credentials(HttpHeaderCredentials::class) {
				name = "Authorization";
				value = "token ${System.getenv("ACCESS_TOKEN")}";
			}

			authentication {
				create<HttpHeaderAuthentication>("header");
			}
		}
	}
	publications {
		create<MavenPublication>("maven") {
			from(components["java"]);
		}
	}
}

plugins {
	id("alpha.java-library-conventions")
}

dependencies {
	api(project(":alpha-api"))
	api(project(":alpha-commons"))
	implementation(project(":alpha-core"))
}

tasks.test {
	useJUnitPlatform()
}

tasks.register("clean") {
    group = "build"
    description = "Cleans the default IntelliJ plugin modules in this repository."
    dependsOn(":mybatis-sql-capture:clean")
}

tasks.register("test") {
    group = "verification"
    description = "Runs tests for the default IntelliJ plugin modules in this repository."
    dependsOn(":mybatis-sql-capture:test")
}

tasks.register("buildPlugin") {
    group = "build"
    description = "Builds the default IntelliJ plugin artifacts in this repository."
    dependsOn(":mybatis-sql-capture:buildPlugin")
}

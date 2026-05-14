# IDEA Plugins

`idea` is the repository root for IntelliJ IDEA plugins under `SUPPER-MR-CODE`.

## Layout

- `mybatis-sql-capture/`: MyBatis SQL log capture plugin

Future plugins should be added as new top-level folders in this repository, not placed directly in the root.

## Build

From the repository root:

```powershell
.\gradlew.bat test buildPlugin
```

Plugin artifact output:

```text
mybatis-sql-capture/build/distributions/
```

You can also build a specific plugin module directly:

```powershell
.\gradlew.bat :mybatis-sql-capture:buildPlugin
```

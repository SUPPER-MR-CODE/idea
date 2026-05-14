# MyBatis SQL Capture

An IntelliJ IDEA plugin project that captures MyBatis log output from IntelliJ IDEA run/debug sessions and restores it into formatted executable SQL.

## MVP Scope

- Console/editor selection to SQL restore
- Mapper XML / annotation preview for executable SQL
- Dialog-based flow with `Copy`, `Open in SQL Scratch`, and best-effort `Run`
- Editor/Tools menu action: `Restore MyBatis SQL`
- Editor popup action: `Preview Executable SQL`
- Multi-statement parsing
- String, numeric, boolean, and null parameter handling

## Supported Log Pattern

```text
==>  Preparing: SELECT * FROM user WHERE id = ? AND status = ?
==> Parameters: 42(Long), ACTIVE(String)
```

Output:

```sql
SELECT * FROM user WHERE id = 42 AND status = 'ACTIVE';
```

## Run

On Windows:

```powershell
.\gradlew.bat runIde
```

Build the plugin ZIP:

```powershell
.\gradlew.bat buildPlugin
```

## JetBrains Marketplace

The current build artifact for manual upload is generated in:

```text
build/distributions/
```

For the first Marketplace release, JetBrains requires a manual upload in the Marketplace UI.
After the plugin exists in Marketplace, later versions can be published through Gradle with a personal access token.

## Usage

- In an editor or console, select MyBatis log text and right-click `Restore MyBatis SQL`
- Or use `Tools > Restore MyBatis SQL` and paste the log into the dialog
- In mapper XML or `@Select/@Update/@Insert/@Delete` annotations, right-click `Preview Executable SQL`

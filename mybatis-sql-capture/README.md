# MyBatis SQL Capture

An IntelliJ IDEA plugin that captures MyBatis SQL logs from run/debug sessions and restores them into formatted executable SQL automatically.

## Features

- Background capture of MyBatis `Preparing` and `Parameters` logs
- Automatic SQL restore and formatting
- Dedicated `MyBatis SQL` tool window with history
- CRUD-aware coloring for `SELECT`, `INSERT`, `UPDATE`, and `DELETE`
- Copy, clear, and appearance controls directly in the `MyBatis SQL` panel
- Right-click copy and `Ctrl+C` support for selected SQL entries
- Custom SQL colors, font size, and bold font option in `Settings > Tools > MyBatis SQL`
- Manual fallback actions for restoring selected logs
- Mapper XML / annotation SQL preview actions

## Example

Input log:

```text
==>  Preparing: SELECT * FROM user WHERE id = ? AND status = ?
==> Parameters: 42(Long), ACTIVE(String)
```

Captured result:

```sql
SELECT *
FROM user
WHERE id = 42
AND status = 'ACTIVE';
```

## Usage

### Automatic capture

1. Run your application from IntelliJ IDEA.
2. Make sure MyBatis SQL logs are printed to the Run/Debug console.
3. The plugin captures matching log lines automatically.
4. Open the `MyBatis SQL` tool window to view formatted SQL history.

### Manual restore

- Select MyBatis log text in an editor or console
- Right-click `Restore MyBatis SQL`

### Mapper preview

- Place the caret inside a mapper XML statement or MyBatis annotation
- Right-click `Preview Executable SQL`

## Build

From the repository root, run tests:

```powershell
.\gradlew.bat test
```

Build the plugin ZIP from the repository root:

```powershell
.\gradlew.bat buildPlugin
```

Or build only this plugin module:

```powershell
.\gradlew.bat :mybatis-sql-capture:buildPlugin
```

The packaged plugin is generated under:

```text
mybatis-sql-capture/build/distributions/
```

## Compatibility

- IntelliJ IDEA 2022.1 to 2025.3
- Build range: `221` to `253.*`
- Verified with IntelliJ Plugin Verifier across representative IDEA Community / Ultimate releases in that range

## Artifact

Latest local build artifact name:

- `mybatis-sql-capture-0.3.2.zip`

## Marketplace

For the first Marketplace release, upload the ZIP manually in the JetBrains Marketplace UI.

Official docs:

- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [Plugin Configuration File](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)

## License

[MIT](../LICENSE)

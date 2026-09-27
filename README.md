# RealUtils

Shared utilities for the Real* plugins (RealMines, RealHoppers, RealScoreboard, RealSkywars, RealLogin).

## What's inside

- `joserodpt.realutils.dialog`: Minecraft dialog screens (1.21.6+), shown through
  [UniDialog](https://github.com/ProjectUnified/UniDialog) on Paper and Spigot.
  - `Dialogs`: setup and shutdown, the on/off switch (`useDialogs`), default button labels, and `confirm(...)` yes/no questions.
  - `DialogForm`: forms with text boxes, switches and sliders, plus item icons and inline sprites on Paper.
  - `DialogMenu`: a menu of buttons.
  - `SettingsDialog` + `SettingsStore`: a plugin's config as a menu of categories, each opening a form.
- `joserodpt.realutils.input.PlayerInput`: asks a player to type something, in a dialog text box where the server has dialogs, otherwise in chat.
- `joserodpt.realutils.gui`:
  - `GUIBuilder`: inventory screens of buttons, with close actions, title updates and slots that mirror another inventory.
  - `MaterialPickerGUI`: a searchable, paged chest to pick a material from.
  - `Pagination`: a list read a page at a time.
- `joserodpt.realutils.text`: `Text` colours and sends messages with the plugin's colouring and prefix. `ForestColorAPI` adds hex colours and gradients on 1.16+.
- `joserodpt.realutils.item`: `Items` makes GUI items. `ItemStackSpringer` turns items into maps or JSON and back.
- `joserodpt.realutils.config.YamlConfig`: a BoostedYAML file in the plugin's folder, optionally versioned and auto-updated.
- `joserodpt.realutils.command.LampExceptionHandler`: sends Lamp's command errors through the plugin's own messages.
- `joserodpt.realutils.update.UpdateChecker`: reads the latest version off SpigotMC.

Every dialog shows nothing unless `Dialogs.isSupported()`. Screens keep their chat or inventory version for servers without dialogs, and for when `useDialogs` is off.

UniDialog is built for Java 21, while the plugins still run on older servers. None of its classes are loaded until the server is known to have dialogs.

## Using it

```xml
<dependency>
    <groupId>com.github.joserodpt</groupId>
    <artifactId>RealUtils</artifactId>
    <version>1.1.0</version>
</dependency>
```

Shade it into the plugin and relocate both it and UniDialog. That way every plugin keeps its own copy, its own dialogs and its own click actions:

```xml
<relocation>
    <pattern>joserodpt.realutils</pattern>
    <shadedPattern>joserodpt.myplugin.shaded.realutils</shadedPattern>
</relocation>
<relocation>
    <pattern>io.github.projectunified.unidialog</pattern>
    <shadedPattern>joserodpt.myplugin.shaded.unidialog</shadedPattern>
</relocation>
<relocation>
    <pattern>dev.dejvokep.boostedyaml</pattern>
    <shadedPattern>joserodpt.myplugin.shaded.boostedyaml</shadedPattern>
</relocation>
```

BoostedYAML (for `YamlConfig`) and Lamp (for `LampExceptionHandler`) are `provided`: the plugin brings its own copy and relocates it. A plugin that uses neither never loads those classes.

With `minimizeJar`, keep all of `com.github.joserodpt:RealUtils` and `io.github.projectunified:*`. The dialog backends are loaded by name, so minimizing would remove them.

```java
// onEnable
RealUtils.setup(this); // registers the GUIBuilder and MaterialPickerGUI listeners
Text.prefix(() -> config.file().getString("Prefix") + " &r");
Dialogs.setup(this, () -> getConfig().getBoolean("useDialogs", true));
PlayerInput.setup(this, p -> titles, p -> dialogText, p -> p.sendMessage("Cancelled"), p -> p.sendMessage("Error"));
getServer().getPluginManager().registerEvents(PlayerInput.getListener(), this);

// onDisable
Dialogs.shutdown();
```

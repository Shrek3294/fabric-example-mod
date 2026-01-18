# Lifesteal Auto Sell

Fabric client-side mod that monitors your inventory and automatically runs `/sell`, shifts your configured items into the sell GUI, and clicks the green-dye confirmation button whenever the condition you choose is met.

## Key features
- Configurable trigger:
  - **Empty slots** — run `/sell` once your free slots are at or below a threshold.
  - **Item count** — run `/sell` once the total count of selected items reaches or exceeds a threshold.
- Specify exactly which items to move into the sell GUI and what command to run.
- Automatically clicks the confirm button (green dye by default) and optionally closes the screen afterward.
- Built-in config screen (press `O` by default) plus Mod Menu support.
- Respects an adjustable action delay/cooldown to avoid spamming commands.

## Prerequisites
- Minecraft 1.21.11 with Fabric Loader ≥ 0.18.2.
- Fabric API 0.139.4+ recommended.
- Optional: [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) to expose the config screen from the mod list.

## Building the mod

You can build a jar locally with Gradle:

```powershell
.\gradlew.bat clean build
```

The client-side jar will be located at `build/libs/lifesteal-auto-sell-0.1.0.jar`. Drop it into your `mods` folder alongside Fabric API (and Mod Menu, if you are using the optional integration).

## In-game usage

- Press `O` (default keybind) to open the Lifesteal Auto Sell config screen anywhere on-screen.
- If Mod Menu is installed, head to the mod list → **Lifesteal Auto Sell** → **Config** for the same screen.
- Configure the trigger mode, threshold, which command to run (`/sell` by default), the confirm item (green dye), and which items to sell.

### GUI options explained

- **Enabled**: turn the automation on or off without leaving the game.
- **Trigger mode** toggles between counting empty slots (0 = full inventory) and counting total copies of your selected items.
- **Items**: comma-separated item identifiers (`minecraft:cobblestone,minecraft:diamond_ore`). Invalid entries are ignored.
- **Threshold**: interpreted either as “empty slots at most” or “count of selected items”, depending on the trigger mode.
- **Command**: whichever command you want the mod to run (no leading `/` required).
- **Confirm item**: the item that represents the “Confirm” button inside the sell GUI (default `minecraft:green_dye`).
- **Save/Cancel**: Save writes `.minecraft/config/lifesteal-auto-sell.json`; Cancel throws away the changes.

## Config file

Location: `<minecraft folder>/config/lifesteal-auto-sell.json`. Example structure:

```json
{
  "enabled": true,
  "triggerMode": "EMPTY_SLOTS_AT_MOST",
  "emptySlotsThreshold": 0,
  "itemCountThreshold": 0,
  "command": "sell",
  "confirmItemId": "minecraft:green_dye",
  "itemIdsToSell": ["minecraft:cobblestone"],
  "actionDelayTicks": 2,
  "cooldownTicks": 40,
  "closeScreenAfterConfirm": true,
  "debugChat": false
}
```

- **actionDelayTicks** and **cooldownTicks** smooth the automation: increase them if `/sell` takes a moment to load on your server.
- **closeScreenAfterConfirm** closes the sell GUI after clicking confirm (set to `false` if your server uses a custom flow).
- **debugChat** enables a brief chat notification when the command runs.

## Testing

```powershell
.\gradlew.bat clean build
```

Ensures the mod compiles and produces the remapped jar under `build/libs`.

## License

CC0-1.0 (same as the Fabric example template). Use freely.

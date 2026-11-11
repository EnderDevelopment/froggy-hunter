# Minecraft Plugin: Froggy Hunter

Froggy Hunter is a Minecraft plugin that adds a hostile Froggy hunter mob to your Paper server. This Minecraft plugin uses a marked slime entity that chases nearby players, jumps across land, moves over water, attacks with a long-range tongue, bites players at close range, and can inflict a temporary knockout effect.

## Install

1. Use Java 21 and a Paper 1.21 server.
2. Build the plugin with Maven from the project directory:

   `mvn package`

3. Copy the generated `target/plugin.jar` file into your server's `plugins` folder.
4. Start or restart the server.
5. Confirm that FroggyHunter loads successfully in the server console.

The plugin does not require a configuration file or database.

## Use

1. Join the server with a player account.
2. Run:

   `/froggy spawn`

3. Froggy appears at your current location and begins searching for nearby players.
4. Stay within approximately 30 blocks for Froggy to pursue you.
5. Froggy jumps while moving on land and adjusts its movement to travel across water.
6. At range, Froggy periodically lashes its tongue toward a target and pulls the player closer.
7. At close range, Froggy bites the player, dealing damage and applying Slowness and Weakness.
8. Some hits have an 18 percent chance to apply a temporary knockout effect using Blindness, heavy Slowness, and Weakness.
9. Use `/froggy help` to display the spawn command.
10. Froggy hunters created by this plugin are removed when the plugin is disabled.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/350e91c6-c00f-4b89-b9ba-df48fab99d2a?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=350e91c6-c00f-4b89-b9ba-df48fab99d2a

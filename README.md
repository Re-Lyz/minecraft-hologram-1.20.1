# Hologram
## Introduction
Hologram plugin for Minecraft.

It was made as part of a video: https://youtu.be/ae_Gns9ZBqY

This plugin is very experimental and untested in multiplayer. Use at your own risk.


## Installation
1. Download the JAR from the [releases page](https://github.com/TheCymaera/minecraft-hologram/releases/).
2. Set up a [Paper](https://papermc.io/downloads) or [Spigot](https://getbukkit.org/download/spigot) server. (Instructions below)
3. Add the JAR to the `plugins` folder.
<!--4. Download the world folder from [Planet Minecraft](https://www.planetminecraft.com/project/spider-garden/).-->
<!--5. Place the world folder in the server directory. Name it `world`.-->

## Running a Server
1. Download a server JAR from [Paper](https://papermc.io/downloads) or [Spigot](https://getbukkit.org/download/spigot).
2. Run the following command `java -Xmx1024M -Xms1024M -jar server.jar nogui`.
3. I typically use the Java runtime bundled with my Minecraft installation so as to avoid version conflicts.
   - In Modrinth, you can find the Java runtime location inside the profile options menu.
4. Accept the EULA by changing `eula=false` to `eula=true` in the `eula.txt` file.
5. Join the server with `localhost` as the IP address.


## Commands
### Globe
Summon a globe:
```
summon minecraft:marker ~ ~1 ~-1 {Tags:["globe"],Rotation:[0f,0f]}
```

Remove with transition:
```
tag @e[tag=globe] add globe_close
```

Toggle with sound effects (Run in sequence):
```
tag @n[tag=globe] add globe_close
execute unless entity @e[tag=globe_close] unless entity @e[tag=globe] run summon minecraft:marker ~ ~ ~ {Tags:["globe"],Rotation:[-90f,0f]}
execute as @e[tag=globe] unless entity @s[tag=globe_close] at @s run playsound minecraft:block.beacon.activate block @a ~ ~ ~ 1 1
execute as @e[tag=globe_close] at @s run playsound minecraft:block.beacon.deactivate block @a ~ ~ ~ 1 1
```

Load preset (Autocomplete will show available options):
```
globe_preset <name>
```

Read/set options (Autocomplete will show available options):
```
globe_settings <name>
globe_settings <name> <value>
```

### Static Models
Create static models:
```
summon minecraft:marker ~ ~ ~ {Tags:["mountainray"]}
summon minecraft:marker ~ ~ ~ {Tags:["mountainray_juvenile"]}
summon minecraft:marker ~ ~ ~ {Tags:["utah_teapot"]}
summon minecraft:marker ~ ~ ~ {Tags:["suzanne"]}
```

### 3D Grapher
```
summon minecraft:marker ~ ~ ~ {Tags:["3d_grapher"]}
```

### Marching Cubes
Create and configure marching cubes:
```
summon minecraft:marker ~ ~ ~ {Tags:["marching_cubes"]}
data modify entity @n[tag=marching_cubes] BukkitValues merge value {"hologram:optimize":true}
data modify entity @n[tag=marching_cubes] BukkitValues merge value {"hologram:render_debug":true}
data modify entity @n[tag=marching_cubes] BukkitValues merge value {"hologram:isovalue":.5f}
```

## Development
1. Clone or download the repo.
2. Run Maven `package` to build the plugin. The resulting JAR will be in the `target` folder.
3. For convenience, set up a symlink and add the link to the server `plugins` folder.
   - Windows: `mklink /D newFile.jar originalFile.jar`
   - Mac/Linux: `ln -s originalFile.jar newFile.jar`

## Attribution
Satellite images from NASA:
https://visibleearth.nasa.gov/collection/1484/blue-marble-next-generation?page=4
https://visibleearth.nasa.gov/images/144898/earth-at-night-black-marble-2016-color-maps

Basketball texture from Robin Wood:
https://www.robinwood.com/Catalog/FreeStuff/Textures/TexturePages/BallMaps.html

Some assets have been modified.

Utah Teapot model by Martin Newell:
https://en.wikipedia.org/wiki/Utah_teapot

Blender Monkey model by the Blender Foundation:
https://www.blender.org/

Marching Cubes algorithm adapted from Nihal Jain:
https://github.com/nihaljn/marching-cubes

## License
3rd party assets are under their respective licenses.

You may use the plugin and source code for both commercial or non-commercial purposes.

Attribution is appreciated but not due.

Do not resell without making substantial changes.
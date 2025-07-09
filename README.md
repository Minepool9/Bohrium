# THIS JAR REQUIRES [JAVA 17 RUNTIME](https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/tag/jre17-ec28559), POJAV OR MOJO CANT RUN JARS ON 21 NOR THE JAR ITSELF CAN RUN WITH RUNTIME 8

## Overview

just a basic mod downloader and (soon to be) manager for stuff like mojolauncher and povjavlauncher and etc written in swing

for now until i get a curseforge api key, this is modrinth sourced mostly.

warning: kinda my second "big" java project, except bad code, like a lot.

## instructions for teh new 

1. download a jar from release
2. install the java 17 runtime if you havent into your runtimes
3. click execute jar in your launcher
4. pick the bohrium jar
5. set your mod folder via the settings and versions menu
   * for Pojav: if you arent using a modpack and just are using a mc version with a loader, its your `Android/data/whateverpovjavsappidis/files/.minecraft/mods` otherwise if its a modpack its custominstances instead of .minecraft and the modpack name
   * for mojo: `Android/data/mojo'scoolappname/files/instances/your instance name/mods`
6. set your mc version (soon to add more, my bad guys)
7. set your mc loader
8. hit save
9. find the mods you want
10. select them, highlighted mods are in the download queue and hitting them again while they are blue unhighlights them and removes them from queue
11. hit download selected after picking your mods
12. review your list, delete any from queue if needed
13. wait
14. done, your mods should my downloaded

## TODO
- [ ] add curgeforge
- [ ] add a search bar
- [x] add more mc versions
- [ ] add the ability to select mod versions to basically downgrade
- [x] add a fucking mod manager into this
- [ ] add a notification for if you are offline
- [x] idk? probably not adding bugs?

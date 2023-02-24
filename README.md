# zeldabot
A bot can beat NES Zelda in under an hour

# Status reports
- Hey I just started this project!

2/2023
- Defeat Level 7 Grabby Hands screen
1. If can't use sword, move to safe area inside ring
2. If there is a hand, move to the attack spot, once at the attack spot attack continuously
3. If there is no hand, move to the attract cell
4. If all the hands are dead and there are only suns, proceed.

12/2022
- Zeldabot can beat level 1, 2, and 3 most of the time, with a clumsy, wiggly romp. Written in 6500 lines of Kotlin. Go zeldabot go!

# rules
Hacks, AI, hard coded solutions, anything possible, must complete within 1 hour limit, all items must be collected

# links
* http://thealmightyguru.com/Games/Hacking/Wiki/index.php/The_Legend_of_Zelda#Load_Room_With_Enemies
* https://github.com/zoq/nes/blob/master/SuperMarioBros/super_mario_bros.lua

# Emulators
* https://humbletoolsmith.com/2018/04/25/creating-a-bot-to-play-nes-games-with-csharp/

# maps
* https://github.com/asweigart/nes_zelda_map_data
* http://inventwithpython.com/blog/2012/12/10/8-bit-nes-legend-of-zelda-map-data/
* https://nesmaps.com/maps/Zelda/ZeldaBG.html

# Maps
* http://media.tojicode.com/zelda/
* https://nesmaps.com/maps/Zelda/ZeldaOverworldQ1.html
* https://www.ign.com/wikis/the-legend-of-zelda/Overworld_Map
* https://www.zeldaxtreme.com/legend-of-zelda/maps/ (extreme!)
* https://i.imgur.com/oXLeDkn.png labeled where stuff is
* https://twitter.com/MetroidMike64/status/1059878242839076864/photo/1 map grid
* https://retrocomputing.stackexchange.com/questions/6388/were-the-dungeons-in-legend-of-zelda-designed-to-fit-together [level maps together]
* https://ian-albert.com/games/legend_of_zelda_maps/zelda-dungeon6.png [labelled dungeon maps]

# AI gym
* https://naereen.github.io/gym-nes-mario-bros/
* https://openai.com/blog/gym-retro/
* https://retro.readthedocs.io/en/latest/integration.html

# playing zelda
* https://www.zeldaclassic.com/downloads/
* http://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda#Known_Dumps

# game dev links
* https://www.libretro.com/index.php/api/

# rom / ram memory / dev maps
* https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:ROM_map [most complete]
* https://www.romhacking.net/?page=documents&game=712
* http://www.bwass.org/romhack/zelda1/zelda1rammap.txt
* https://userpages.monmouth.com/~colonel/videogames/zelda/moonmap.html
* https://github.com/aldonunez/zelda1-disassembly/tree/master/src

# game bot
* https://humbletoolsmith.com/2018/04/25/creating-a-bot-to-play-nes-games-with-csharp/
* https://meatfighter.com/castlevaniabot/

# emulator
* https://nintaco.com/

# sub problem: finding shortest paths between places on the map
Need a grid-based dykstra's algorithm
* https://levelup.gitconnected.com/dijkstras-shortest-path-algorithm-in-a-grid-eb505eb3a290

# graphs
* https://graphviz.org/Gallery/undirected/grid.html vis?
* https://jgrapht.org/guide/UserOverview#graph-algorithms Algorithms
* https://github.com/breandan/galoisenne Kotlin
* A star https://github.com/div5yesh/ai-explorer/blob/master/a_star.py
* https://github.com/Suwadith/A-Star-Shortest-Pathfinding-Algorithm-Square-Grid-Java * Star grid

* more a star
https://github.com/hoc081098/Astar-Dijkstra-GreedyBestFirstSearch-BreadthFirstSearch-DepthFirstSearch/tree/master/src/main/kotlin/com/hoc/kotlin

Questions to answer:
* What is optimal path to reach all levels and all items?
  * without constraint?
  * adding "need item for level X" constraint
  * making it easier for player: Prioritize levels 1,2, hearts and white sword

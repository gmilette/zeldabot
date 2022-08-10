# zeldabot
A bot can beat NES Zelda in under an hour

# Status reports
- Hey I just started this project!

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

# Maps
* http://media.tojicode.com/zelda/
* https://nesmaps.com/maps/Zelda/ZeldaOverworldQ1.html
* https://www.ign.com/wikis/the-legend-of-zelda/Overworld_Map

# AI gym
* https://naereen.github.io/gym-nes-mario-bros/
* https://openai.com/blog/gym-retro/
* https://retro.readthedocs.io/en/latest/integration.html

# playing zelda
* https://www.zeldaclassic.com/downloads/
* http://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda#Known_Dumps

# game dev links
* https://www.libretro.com/index.php/api/

# rom 
* https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:ROM_map
* https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda

# game bot
* https://humbletoolsmith.com/2018/04/25/creating-a-bot-to-play-nes-games-with-csharp/

# emulator
* https://nintaco.com/

# sub problem: finding shortest paths between places on the map
Need a grid-based dykstra's algorithm
* https://levelup.gitconnected.com/dijkstras-shortest-path-algorithm-in-a-grid-eb505eb3a290

# graphs
* https://graphviz.org/Gallery/undirected/grid.html vis?
* https://jgrapht.org/guide/UserOverview#graph-algorithms Algorithms
* https://github.com/breandan/galoisenne Kotlin

Questions to answer:
* What is optimal path to reach all levels and all items?
  * without constraint?
  * adding "need item for level X" constraint
  * making it easier for player: Prioritize levels 1,2, hearts and white sword

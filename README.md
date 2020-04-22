# L2jOrion_Interlude_C6
L2jOrion Source https://www.l2jorion.com/index.php?/topic/66-changeset-16/

# SERVER INFO

> L2jOrion supports all Lineage 2 Interlude official features and has many own additional unique features. Just a few mentioned below for now and i will update it later. Also, any feature can be turned off inside pack config. All quests fully working and updated by Lineage 2 Interlude official information. Everything can be checked in test server with full npcs and special commands for testers.

# Events

>- Team vs Team (Possible add more than 2 teams)
>- Deathmatch
>- Capture the Flag
>- Special NPCS

>- Buffer - Possible use like simple or mixed mode with schemes (save/load). Buffs for pets and players. Cancellation of buffs can be one by one or all at once. Also, buffer has a heal function, premium buffs, auto buff lists (fighter/mage), and buffs for special item. Available special command for player and add to Community Board too.
>- Global gatekeeper - player can use simple, premium and for special item teleports. Available special command for player and add to Community Board too. Also, possible to change the time of gk animation. 
>- GM Shop - it has special donate item and special functions for donators like: no karma, clan functions, premium time, name/tile colors and etc.  Available special command for player and add to Community Board too.
>- Marketer - It's selling/buying system where player can add any item for special item (unlimited currency) and also possible add enchanted or augmented items.  Available special command for player and add to Community Board too.
>- Class Manager - Can be like npc or pop up menu. Possible add price or reward for class, pets upgrade. Special command available.
>- Wedding Manager.

# Geo-Engine

> Added newest geo-engine which works like Lineage 2 official.

# Special Player Commands

> - .menu - Additional player functions in one place: exp on/off, title with pvp/pk (live update), block buffs, auto pick up on/off, trade on/off, private messages on/off, screen text pop up like critical, damage and etc on/off, falling to textures auto correction, change password, repair character and auto potions (working with all potions).
> - .boss - Simple and epic bosses in one place. Possible see alive status, dead time, boss lvl and clicked on boss name you can get location to boss.- .votereward -  Vote system for reward . Added Topzone, Hopzone, L2network and L2jBrasil).
> - .bank - Adena and your chosen item exchanger.
> - .sub - All sub-classes in one place.
> - .sellbuffs - Possible sell your own buffs.

# UPDATES

>Some imports corrections by L2OFF information. Our priority is L2OFF and all updates are doing by this info except some modifications which possible easy turn off.
Added a new test server system 1.05 which is closer to L2OFF and it doesn't have modifications from the newer l2 clients.

###### Geo-engine:
- Re-worked character movement in the water. Fixed falling damage and hard dipping. This issue worked mostly in catacomb or necropolis when character moves in/out, because of geo-data.
- Updated synchronization system between client<->server.
- Updated character update position system. 
- Updated geo-engine system.
###### Misc
- Class Master updates: added lvl checking on the class change and heal's config.,
- Geo-engine: a little more corrections for better performance.,
- Fixed a bug report: (Server w/o Auto Learn skill) For Example: If you have toggle Skill LvL1 in USE, and learn LvL2, then you cant shut down Skill (and still is use lvl1) Only RR help.
Boats system
- Re-worked entire system and fixed characters movement to in/out the boat. Added all L2OFF system messages, routes and etc. Totally re-worked from core side with L2BoatAI, L2Vehicle, vehicle stats  and fully updated client/server packets (For now to test it is available only in TEST server)
###### AI
- Re-worked castle siege guards AI system. Old system had lots issues and made a lag for players in the some castles.
- Re-worked Faenor system and added medals.xml, valentines.xml events. In this case we don't need these files in libs folder anymore: bsh-2.0b6.jar, bsh-engine.jar
- Re-worked python/java engines and we don't need these files in libs folder anymore: java-engine-1.8.jar, jython-engine-2.2.1.jar

###### Other implements

ADDED: a premium account teleport system. Just change one word „...goto..“ to „...premium_goto...“for example: „bypass -h custom_dotele premium_goto 2503“.

CORRECTED: character movement issues on attack mode.

Re-worked - .boss command. Added: Raid Bosses list by levels. Also, possible see death time, aggro, level and location on map.

Fixed - some small issues in Queen Ant AI.

Added - Some donate functions. Possible to add more colors for name and title. All prices you can change inside config - ljstudio.ini

FIXED - Some FOG mobs which had wrong locations on random spawn after kill.

UPDATED - Seven Sings teleport locations to the hunting zones which haven't correct locations.

UPDATED - knownlist system and improved performance. Also, decreased CPU usage.

RE-WORKED - command: .votereward. From now on, you can get reward for each one vote. Also, I added a new top  - L2jbrasil. All configs are inside pack - ljstudio.ini. Up there you can to put your item id and set up min/max items in random for each vote.

UPDATED - mobs AI knownlist system. Some mobs which have faction ID (clan) need a faster reaction to help each other.  Now they have it.

UPDATED - powerpack system and re-worked global gk, gm shop and buffer. Since this made, from gmshop, gatekeeper and buffer folders htm files are using same for NPC and player commands. Also, possible use in community board.

Added a new global gk. Available menus: All towns + noble menu, catacombs, necropolises, teleportation to cursed weapon for special item, teleportation to any zone for special item or just only for premium accounts.

UPDATED - Buffer menu and added possibility remove buff one by one.

UPDATED - Class balance system and added classes balance control  by second class (damages - to/by fighters and mages). 3rd  class was before.

ADDED - A new command: .bank and also, possible use like additional selection in any menu. All configs inside powerpack.ini:

UPDATED - our synchronization system CLIENT<->SERVER and improved performance. Don't forget use a config option: CoordSynchronize = 3 in geodata.ini

FIXED - Skill: Lucky. Impossible get Death Penalty when you are 1-9lv by L2OFF.

CLEANED - many unnecessary code/configs for better performance.

UPDATED - option : MaxDriftRange and added geodata checking which doesn't allow mobs go to wall if they are nearby wall.

ADDED - option: MaxRespawnRange. From now on, all mobs can be re-spawned by random loc like L2OFF. Also, added geodata checking for those mobs which can be re-spawned to wall.

ADDED -  new MMO core system and removed unnecessary protection configs.

RE-WORKED - flood protection system and now it has better performance.

ADDED -  new protection for multisell of .shop command which doesn't allow use any mulltisell id by PHX. It wil be added for all multisells later.

CLEAN UP - multisells which are not default and unusing.

FIXED - 1000+ bugged creatures which had wrong loc or were underground  (under textures) and that's all what our system found which means no more bugged mobs.  This happened after a new geo-data system installation when some mobs had a bad Z loc before and later get bugged. Also, some were left since very old time.

UPDATED - MaxDriftRange and MaxRespawnRange functions for better geo-data checking and it doesn't allow walk or be re-spawned to wall on bigger range.

FIXED - character heading. From now on, you can see correctly character direction on spawn (teleport, enter to world and etc.). basically, on first seeing character will show correctly direction, not the same like always was before.

ADDED - a new command .sellbuffs and .cancelsellbuffs with some additional configs  (BETA)

ADDED - New logging system.

ADDED - New Game Time Controller system.

ADDED - New Geo-Engine which works much better than old.

ADDED - New DeadLock system with auto server restart.

UPDATED - Auto Save Manager.

CLEAN UP - Unnecessary imports, configs and settings.

UPDATED - Command .gk/Global teleport and added possibility turn off teleport animation (animation = same like /unstuck, escape or etc.)

RE-WORKED - Grand Bosses system and from now on possible add separate Fix Time. Also, changed time settings.

FIXED -  All town pets which were missed.

RE-WORKED - Buffer and improved system. From now on all buffs in one place. Possible save your buffs as scheme. Added more nice features. Available to check on the test server.

RE-WORKED - Market and improved system - added special command: .market, added special configurations to pack side, possible sell augmented items, money sending directly to inventory (offline player included), email notifications (in game), search system, latest items and etc.

UPDATED - Community Board and fixed some menus which weren't work correctly before: all search systems, paging, clan management, clan mail, region menu, favorites menu and etc.

UPDATED - siege system to make this work better.

UPDATED - boss: Benom (siege boss),  fixed some small issues and added missing drop.

CLEAN UP - imports/unused codes and configs for better performance.

FIXED - Can not see target bug for doors. After a new geo-engine installing this had a small mistake which is fixed now.

ADDED - Custom config for market . From now on you can add any item like a currency (unlimited).

ADDED - Premium buff time multiplier.

ADDED - Random enchanted items to drop list. Possible add enchanted items with min and max options.

CORRECTED - Walking speed. Some creatures had wrong speed. (Based on Lineage 2 Interlude Official information)

CORRECTED - All pets skills, some of them were generating aggro what is wrong. (Based on Lineage 2 Interlude Official information)

CORRECTED - Skill: Break Duress, it must remove effect of Root, not only Slow. (Based on Lineage 2 Interlude Official information)

CORRECTED - Skills: Noblesse Blessing and Salvation. They can not work together. (Based on Lineage 2 Interlude Official information)

REWORKED - Buylist system. and fixed some small issues.

FIXED -  Devastated Castle issue;

FIXED -  Castle's doors bug. All doors must be attackable for all players on the siege progress and not only for castle's attackers. (Based on Lineage 2 Interlude Official information)

FIXED -  Pet's bug. If pet gets a stun he doesn't come back to owner when stun is gone. Now it comes back.

FIXED -  From now on all pets can get herbs and if player has summoned the pet the time of herbs is dividing. (Based on Lineage 2 Interlude Official information)

FIXED -  Soul Crystals levelling up. Before it did not work correctly and now it works like L2OFF.

FIXED -  Skill: EarthQuake. It did not work in the Olympiad before the match. Now works.

FIXED - Alliance ketra/varka quest. It shouldn't remove  an alliance level on quest abortion.

ADDED - A new geo-data to some regions to fix some issues.

CORRECTED - SP and EXP,  some creatures had wrong SP and EXP numbers. (Based on Lineage 2 Interlude Official information)

CORRECTED - SKill: Corpse Burst. Before skill's explosion didn't do a damage for creatures around and now it does.

CORRECTED - SKill: Frost Bolt. Before it worked like a de-buff, but also it must does a damage for target. (Based on Lineage 2 Interlude Official information)

RE-WORKED - Resurrection system and fixed 2 bugs on siege. 1. If attacker clan has a siege flag - they can use a resurrection for a clan members. 2. If defender clan has a control crystal - they can use a resurrection for clan members. (Based on Lineage 2 Interlude Official information)

RE-WORKED - All grand bosses instances and fixed some small issues. (Based on Lineage 2 Interlude Official information)

RE-WORKED - The Raid Boss: Ice Fairy Sirra. (Based on Lineage 2 Interlude Official information)

ADDED - A new config for special teleport. From now on you can to choose which item you want to use for teleportation (Config inside PowerPack.ini).  Also, you need to change a text inside htm file „goto“ -> „custom_goto“.

ADDED - A new config to Premium Account System for Seal stones rates.

RE-WORKED - All core platform and  increased performance. Faster loading and less CPU usage.



[![forthebadge made-with-python](http://ForTheBadge.com/images/badges/made-with-python.svg)](https://www.python.org/)
[![ForTheBadge built-with-love](http://ForTheBadge.com/images/badges/built-with-love.svg)](https://GitHub.com/Naereen/)

[![Github all releases](https://img.shields.io/github/downloads/Naereen/StrapDown.js/total.svg)](https://github.com/Hl4p3x/L2jOrion_Interlude/releases) [![GPLv3 license](https://img.shields.io/badge/License-GPLv3-blue.svg)](http://perso.crans.org/besson/LICENSE.html) [![Open Source Love svg1](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/)

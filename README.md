# L2jOrion_Interlude_C6
L2jOrion Source https://www.l2jorion.com/index.php?/forum/21-updates/ LATEST UPDATE IS 27/24.

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

# UPDATES

Reworked Statistics RB "Captain of the Ice Queen's Royal Guard".
Fixed Height for Monster "Freya's Servant" and NPC "Freya's Steward".
Seductive Whispers,
A Game Of Cards,
In Search of Dimensional Fragments,
The Zero Hour,
Hunt of the Golden Ram Mercenary Force,
Warehouse Keepers Pastime. 
Guardians of the holy grail

- CORRECTED - Soul Crystal absorb type for Raid boss: Roaring Skylancer, Beast Lord Behemoth.
- CORRECTED - Soul Crystal absorb chance for: Last Hit, One Party Member Random, Full Party.
- CORRECTED - Heine teleport: Field of Silence.
- CORRECTED - Tanor Canyon spawn point, and one monster group that was too close was moved.
- CORRECTED - Ice Queen's Castle Location
- CORRECTED - Random Quest Item drop for Quests (In Party):
- CORRECTED - Number of  treasure chests near Eihalder von Hellmann.
- CORRECTED - Tanor Canyon spawn point.
- CORRECTED - Spawns on Abandoned Camp and Orc Barracks.
- CORRECTED - Spawns near Oren.
- CORRECTED - Skills using on attack (Like L2OFF).
- CORRECTED - Buffer, gatekeeper and shop. From now on all of them work in Community Board without issues.
- CORRECTED - Player's command /unstuck (Like L2OFF).
- CORRECTED - Attacking by Bow (Like L2OFF) .
- CORRECTED - Character movement (in water too, especially going to Catacomb or Necropolis and out).
- CORRECTED - 7Signs teleports and moved from core side to database. Also, fixed exploit.
- CORRECTED - Pets Food and movement Run/Walk.
- CORRECTED - NPC Gremory height.
- CORRECTED - Spawn after Teleport: Rune Castle Town Guild and Spawn after Teleport: Rune Castle Town Temple.
- CORRECTED - Problem with spawn 2 Core Minions near Cruma Marshlands.
- CORRECTED - Boss Core minions number.
- CORRECTED - Imperial Gravekeeper and Imperial Slave (Cemetery) count and changed faction range.
- CORRECTED - 503_PursuitClanAmbition quest.
- CORRECTED - RB Spawn Location: Sejarr's Servitor.
- CORRECTED - Broken mobs spawn in Swamp of Screams.
- CORRECTED - Location of one broken spawn Ant Larva near Wasteland.
- CORRECTED - Broken spawns in Ivory Tower Crater.
- CORRECTED - A few wrong spawns on Valley Of Saints.
- CORRECTED - Varka Silenos Hunter atk.spd.
- CORRECTED - Wrong Used Pet Item Name in Chat Log.
- CORRECTED - Raid Boss Skill: Cancel Magic - Increased chance for success and changed type from One Target to Target Aura.
- CORRECTED - Random PvP Zone system and from now on you won't be available see any other creatures outside/inside zone except players (enemies) inside zone. Zones were moved to new instance.
- CORRECTED - Seven Sign: teleport places and prices (Like L2OFF).
- CORRECTED - Spawns on Dragon Valley Entrance.
- CORRECTED - Olympiad's competitors damage counting issue.
- CORRECTED - Geo-Engine movement issues.
- CORRECTED - Doors system and fixed a few doors issues.
- CORRECTED - Siegeable clanhalls buy lists.
- CORRECTED - Simple and spoiled monster decay time by L2Off.
- CORRECTED - Simple and spoiled monster decay time by L2Off.
- CORRECTED - Skill type imports. A part of code was in wrong place which came from Daddy's DNA - L2jserver (basic platform).
- CORRECTED - All skills which have a type: SPOIL. Now it works like L2 official.
- CORRECTED - Seven Signs,  now you can do a teleport  by closest town region.  (Based on Lineage 2 Interlude Official information)
- CORRECTED - After teleportation to Catacomb and Necropolis now your spawn is on surface. You need swim to the underground.  (Based on Lineage 2 Interlude Official information)
- CORRECTED - Quest: Repent Your Sins. (Based on Lineage 2 Interlude Official information)
- CORRECTED - Casting and attacking system to more similar like Lineage 2 official. If i missed something, please let me know.
- CORRECTED - Simple and spoiled monster decay time by L2Off.
- CORRECTED - Heroes Clan Reputation points on the new period of Olympiad.
- CORRECTED - Walking speed. Some creatures had wrong speed. (Based on Lineage 2 Interlude Official information)
- CORRECTED - All pets skills, some of them were generating aggro what is wrong. (Based on Lineage 2 Interlude Official information)
- CORRECTED - Skill: Break Duress, it must remove effect of Root, not only Slow. (Based on Lineage 2 Interlude Official information)
- CORRECTED - Skills: Noblesse Blessing and Salvation. They can not work together. (Based on Lineage 2 Interlude Official information)
- CORRECTED - SP and EXP,  some creatures had wrong SP and EXP numbers. (Based on Lineage 2 Interlude Official information)
- CORRECTED - SKill: Corpse Burst. Before skill's explosion didn't do a damage for creatures around and now it does.
- CORRECTED - SKill: Frost Bolt. Before it worked like a de-buff, but also it must does a damage for target. (Based on Lineage 2 Interlude Official information)
- CORRECTED - character movement issues on attack mode.
- CORRECTED - Walking speed. Some creatures had wrong speed. (Based on Lineage 2 Interlude Official information)
- CORRECTED - All pets skills, some of them were generating aggro what is wrong. (Based on Lineage 2 Interlude Official information)
- CORRECTED - Skill: Break Duress, it must remove effect of Root, not only Slow. (Based on Lineage 2 Interlude Official information)
- CORRECTED - Skills: Noblesse Blessing and Salvation. They can not work together. (Based on Lineage 2 Interlude Official information)
- CORRECTED - SP and EXP,  some creatures had wrong SP and EXP numbers. (Based on Lineage 2 Interlude Official information)
- CORRECTED - SKill: Corpse Burst. Before skill's explosion didn't do a damage for creatures around and now it does.
- CORRECTED - SKill: Frost Bolt. Before it worked like a de-buff, but also it must does a damage for target. (Based on Lineage 2 Interlude Official information)

# --------------------

- FIXED - Clan's level up achievement issue.
- FIXED - Treasure Chest's movement bug.
- FIXED - Skill: Provoke. It won't work on Guards (Like L2OFF).
- FIXED - Premium account buffs time.
- FIXED - Buying of clan reputation bug. If you haven't the right level of clan the donate coin was taken anyway.
- FIXED - Buffer's issue when you have more than 50 buffs. It ruins whole panel and can't see full choices.
- FIXED - Wrong target bug when you are attacking more than 1 enemies and using the skill. https://www.youtube.com/watch?v=Ckjjz8spOnw (Thanks to Just_Jap for the report)
- FIXED - Attack speed bug when attack speed is bigger than 1,5k. https://www.youtube.com/watch?v=zdWuu3Vg0Uc (Thanks to Just_Jap for the report)
- FIXED - Treasure Chest's movement bug.
- FIXED - Quest: Path To Scavenger issue.
- FIXED - Quest: Magical Power Of Water Part 2 issue.
- FIXED - Over Hit issue.
- FIXED - Town War Event peaceful zone issue.
- FIXED - Skill: Backstab heading bug on just spawned creature (it was bugged only for creatures-monsters).
- FIXED - Skill enchanter bug. In short, different max level of the same skill did not work for all classes and you couldn't enchant skill.
- FIXED - Olympiad's dual box config. It did not work with new Olympiad engine before.
- FIXED - Devastated Castle issue https://www.l2jorion.com/index.php?/topic/79-devasted-casle-bug-with-buylist/
- FIXED - Castle's doors bug. All doors must be attackable for all players on the siege progress and not only for castle's attackers. (Based on Lineage 2 Interlude Official information)
- FIXED - Pet's bug. If pet gets a stun he doesn't come back to owner when stun is gone. Now it comes back.
- FIXED - From now on all pets can get herbs and if player has summoned the pet the time of herbs is dividing. (Based on Lineage 2 Interlude Official information)
- FIXED - Soul Crystals levelling up. Before it did not work correctly and now it works like L2OFF.
- FIXED - Skill: EarthQuake. It did not work in the Olympiad before the match. Now works.
- FIXED - Alliance ketra/varka quest. It shouldn't remove  an alliance level on quest abortion.
- FIXED - some small issues in Queen Ant AI.
- FIXED - Some FOG mobs which had wrong locations on random spawn after kill.
- FIXED - Skill: Lucky. Impossible get Death Penalty when you are 1-9lv by L2OFF.
- FIXED - 1000+ bugged creatures which had wrong loc or were underground  (under textures) and that's all what our system found which means no more bugged mobs.  This happened after a new geo-data system installation when some mobs had a bad Z loc before and later get bugged. Also, some were left since very old time.
- FIXED - character heading. From now on, you can see correctly character direction on spawn (teleport, enter to world and etc.). basically, on first seeing character will show correctly direction, not the same like always was before.
- FIXED - All town pets which were missed.
- FIXED - Can not see target bug for doors. After a new geo-engine installing this had a small mistake which is fixed now.
- FIXED - Devastated Castle issue;
- FIXED - Castle's doors bug. All doors must be attackable for all players on the siege progress and not only for castle's attackers. (Based on Lineage 2 Interlude Official information)
- FIXED - Pet's bug. If pet gets a stun he doesn't come back to owner when stun is gone. Now it comes back.
- FIXED - From now on all pets can get herbs and if player has summoned the pet the time of herbs is dividing. (Based on Lineage 2 Interlude Official information)
- FIXED - Soul Crystals levelling up. Before it did not work correctly and now it works like L2OFF.
- FIXED - Skill: EarthQuake. It did not work in the Olympiad before the match. Now works.
- FIXED - Alliance ketra/varka quest. It shouldn't remove  an alliance level on quest abortion.
- FIXED - Fixed Walk Speed.
- FIXED - Bug with leveling Soul Crystals in Party, now player must be in Party Range.
- FIXED - Bug with get Quest Items in Party, now player must be in Party Range.
- FIXED - Issue with Anchor/Paralyze, when character got anchor can't attack when debuff ends, must move and then attack, now this will work normally.
- FIXED - Apella Sets Debuffs (Like L2OFF).
- FIXED - Problem with lost window before "start test" in quest: Get A Pet.
- FIXED - Crest of dusk and Crest of dawn functions, now when Win DAWN we will see Dawn Flags near Catacombs and Necropolis, if Win DUSK then Dusk Flags (Like L2OFF).
- FIXED - Pets Exp system. (Like L2OFF).
- FIXED - Strider Quest "Little Wings Big Adventure" fully reworked (Like L2OFF).
- FIXED - Blue / deep blue monsters Drop/Spoil chance (Like L2OFF).
- FIXED - Scroll of Escape and /unstuck for class race forbidden (Elf and Dark Elf zones).
- FIXED - Skill: Lure. Now it takes just one monster.
- FIXED - Skills: Dark Vortex, Wind Vortex, Fire Vortex, Light Vortex, Ice Vortex - damage after successful debuff. Enemy gets full damage on first casting, not second like it was before (Like L2OFF).

# --------------------

- ADDED - Missing Quest: „Kamael: A window to the Future.“
- ADDED - AI for Swamp Of Screams Monsters. Now mobs have a chance to go for help when they have 50% HP or less (Like L2OFF).
- ADDED - AI for Orc Barracks Monsters. Now mobs have a chance to go for help when they have 50% HP or less (Like L2OFF).
- ADDED - AI for Abandoned Camp Monsters. Now mobs have a chance to go for help when they have 50% HP or less (Like L2OFF).
- ADDED - AI for Plains of Dion - Monsters "Lizardman". Now when mob is under attack it has a chance to go for help from other monsters around (Like L2OFF).
- ADDED - AI for Quest Monster: Cats Eye Bandit (Like L2OFF).
- ADDED - AI for Monster Timak Orc Overlord. Now uses "Ultimate Buff" when HP is 50% or lower (Like L2OFF).
- ADDED - AI for Monster Timak Orc Troop Leader. Now spawns minions when got hit by player (Like L2OFF).
- ADDED - Missing Hot Springs NPC "Yeti" (Near Master Chef Donath).
- ADDED - Item protection in the warehouse for depositing. From now on the system will check how many maximum item allowed to deposit.
- ADDED - Encoding/Decoding system for htm/html files. No more exploits with external files.
- ADDED - New DressMe system for weapon, armor set, hair and face. Also, shows the skin in character selection window and stays after character restart/offline.
- ADDED - 1 missing stat for Sin eater pet.
- ADDED - Missing Minions: Frenzy Stakato Soldier and Frenzy Stakato Drone for Monsters: Splinter Stakato Drone and Needle Stakato Drone.
- ADDED - Request: Cursed Weapon teleport price and item id  https://www.l2jorion.com/index.php?/topic/239-ability-to-change-the-cursed-teleport-price/
- ADDED - Request: The New Exclude function for account without premium status https://www.l2jorion.com/index.php?/topic/240-ability-to-deactivate-voice-commands-for-no-premium-users/
- ADDED - Request: Updated Captcha Punishment system https://www.l2jorion.com/index.php?/topic/235-new-antibo-captcha/
- ADDED - A new config for warehouse deposit  price
- ADDED - New event: Tournament (BETA)
- ADDED - Random PvP Zone with timer and auto pvp flag. Possible to change all configs in xml file.
- ADDED - Achievement system with 50+ different  achievements (will be more).
- ADDED - New configs for DressMe
- ADDED - New player's command: .dressme
- ADDED - A new option to config: ShowNpcCrest = True/False, only in towns if castle has a lord.
- ADDED - A new option to config: SiegeDelay = 14, Siege Delay by default is 14 days (2 weeks) like L2off
- ADDED - New vote reward for L2Top.Gr
- ADDED - New vote reward for L2Top.Online
- ADDED - A table to database which gives an item immediately to player without character restart. You just need to do the rest job from donate system (website).
- ADDED - New Olympiad engine.
- ADDED - Missing teleport Dawn/Dusk from Oracle to Dimensional Rift. (Based on Lineage 2 Interlude Official information)
- ADDED - Missing npcs to Fortress of the Dead and Wild Beast Reserve clan halls.
- ADDED - New Zones engine system.
- ADDED - New Doors engine which works much better with geo-engine.
- ADDED - New engine of map regions.
- ADDED - Custom config for market . From now on you can add any item like a currency (unlimited).
- ADDED - Premium buff time multiplier.
- ADDED - Random enchanted items to drop list. Possible add enchanted items with min and max options.
- ADDED - premium account teleport system. Just change one word „...goto..“ to „...premium_goto...“for example: „bypass -h custom_dotele premium_goto 2503“.
- ADDED - Some donate functions. Possible to add more colors for name and title. All prices you can change inside config - ljstudio.ini
- ADDED - A new command: .bank and also, possible use like additional selection in any menu. All configs inside powerpack.ini:
- ADDED - option: MaxRespawnRange. From now on, all mobs can be re-spawned by random loc like L2OFF. Also, added geodata checking for those mobs which can be re-spawned to wall.
- ADDED - new MMO core system and removed unnecessary protection configs.
- ADDED - a new command .sellbuffs and .cancelsellbuffs with some additional configs  (BETA)
- ADDED - New logging system.
- ADDED - New Game Time Controller system.
- ADDED - New Geo-Engine which works much better than old.
- ADDED - New DeadLock system with auto server restart.
- ADDED - Custom config for market . From now on you can add any item like a currency (unlimited).
- ADDED - Premium buff time multiplier.
- ADDED - Random enchanted items to drop list. Possible add enchanted items with min and max options.
- ADDED - A new geo-data to some regions to fix some issues.
- ADDED - A new config for special teleport. From now on you can to choose which item you want to use for teleportation (Config inside PowerPack.ini).  Also, you need to change a text inside htm file „goto“ -> „custom_goto“.
- ADDED - A new config to Premium Account System for Seal stones rates.
- ADDED - missing Minions.
- ADDED - missing Drop for group leaders and minions.
- ADDED - missing Drop and Skills for RB "Captain of the Ice Queen's Royal Guard".
- ADDED - a new global gk. Available menus: All towns + noble menu, catacombs, necropolises, teleportation to cursed weapon for special item, teleportation to any zone for special item or just only for premium accounts.
- ADDED - new protection for multisell of .shop command which doesn't allow use any mulltisell id by PHX. It wil be added for all multisells later.
- ADDED - A new geo-data to some regions to fix some issues. (https://www.l2jorion.com/index.php?/files/file/9-geodata/)
- ADDED - A new config for special teleport. From now on you can to choose which item you want to use for teleportation (Config inside PowerPack.ini).  Also, you need to change a text inside htm file „goto“ -> „custom_goto“.
- ADDED - A new config to Premium Account System for Seal stones rates.

# --------------------

- UPDATED - Effects saving system on character/server disconnection. Also, character won't loss his buffs.
- UPDATED - Sync system between server and client.
- UPDATED - mmocore.ini -> ClientFloodProtection config and from now on possible turn it off fully. Just in case if you got some kind of issues with this protection.
- UPDATED - A lof of old code that had a bad performance and changed to new one like characters AI system, movements, attacking and casting behavior. Also,  added next, save action system and new intention system.
- UPDATED - Doors engine and fixed some small mistakes.
- UPDATED - Geo-engine and improved working/performance.
- UPDATED - Shift+click function for players.
- UPDATED - All skills formula of chance to more closer like Official.
- UPDATED - Admin's menu htm files.
- UPDATED - Client Flood protection system. Also, added possibility turn it off.
- UPDATED - Seven Sings teleport locations to the hunting zones which haven't correct locations.
- UPDATED - knownlist system and improved performance. Also, decreased CPU usage.
- UPDATED - mobs AI knownlist system. Some mobs which have faction ID (clan) need a faster reaction to help each other.  Now they have it.
- UPDATED - powerpack system and re-worked global gk, gm shop and buffer. Since this made, from gmshop, gatekeeper and buffer folders htm files are using same for NPC and player commands. Also, possible use in community board.
- UPDATED - Buffer menu and added possibility remove buff one by one.
- UPDATED - Class balance system and added classes balance control  by second class (damages - to/by fighters and mages). 3rd  class was before.
- UPDATED - our synchronization system CLIENT<->SERVER and improved performance. Don't forget use a config option: CoordSynchronize = 3 in geodata.ini
- UPDATED - option : MaxDriftRange and added geodata checking which doesn't allow mobs go to wall if they are nearby wall.
- UPDATED - MaxDriftRange and MaxRespawnRange functions for better geo-data checking and it doesn't allow walk or be re-spawned to wall on bigger range.
- UPDATED - Auto Save Manager.
- UPDATED - Community Board and fixed some menus which weren't work correctly before: all search systems, paging, clan management, clan mail, region menu, favorites menu and etc.
- UPDATED - siege system to make this work better.
- UPDATED - boss: Benom (siege boss),  fixed some small issues and added missing drop.
- UPDATED - Command .gk/Global teleport and added possibility turn off teleport animation (animation = same like /unstuck, escape or etc.)

# --------------------

- RE-WORKED - .boss command. Added: Raid Bosses list by levels. Also, possible see death time, aggro, level and location on map.
- RE-WORKED - command: .votereward. From now on, you can get reward for each one vote. Also, I added a new top - L2jbrasil. All configs are inside pack - ljstudio.ini. Up there you can to put your item id and set up min/max items in random for each vote.
- RE-WORKED - Buylist system. and fixed some small issues.
- RE-WORKED - Resurrection system and fixed 2 bugs on siege. 1. If attacker clan has a siege flag - they can use a resurrection for a clan members. 2. If defender clan has a control crystal - they can use a resurrection for clan members. (Based on Lineage 2 Interlude Official information)
- RE-WORKED - All grand bosses instances and fixed some small issues. (Based on Lineage 2 Interlude Official information)
- RE-WORKED - The Raid Boss: Ice Fairy Sirra. (Based on Lineage 2 Interlude Official information)
- RE-WORKED - All core platform and  increased performance. Faster loading and less CPU usage.
- RE-WORKED - Quest engine and fixed some small issue by L2Off info.
- RE-WORKED - Admin's command npc id/name + go to (//list_spawns) and from now on you can use any id of creature + all bosses included.
- RE-WORKED - Castle system and added some missing functions to Castle Chamberlain.
- RE-WORKED - Grand Bosses system and from now on possible add separate Fix Time. Also, changed time settings.
- RE-WORKED - Buffer and improved system. From now on all buffs in one place. Possible save your buffs as scheme. Added more nice features. Available to check on the test server.
- RE-WORKED - Market and improved system - added special command: .market, added special configurations to pack side, possible sell augmented items, money sending directly to inventory (offline player included), email notifications (in game), search system, latest items and etc.
- RE-WORKED - flood protection system and now it has better performance.
- RE-WORKED - All core platform and  increased performance. Faster loading and less CPU usage.
- RE-WORKED - Resurrection system and fixed 2 bugs on siege. 1. If attacker clan has a siege flag - they can use a resurrection for a clan members. 2. If defender clan has a control crystal - they can use a resurrection for clan members. (Based on Lineage 2 Interlude Official information)
- RE-WORKED - All grand bosses instances and fixed some small issues. (Based on Lineage 2 Interlude Official information)
- RE-WORKED - The Raid Boss: Ice Fairy Sirra. (Based on Lineage 2 Interlude Official information)
- RE-WORKED - Buylist system. and fixed some small issues.
- RE-WORKED - Skills enchanting system.
- RE-WORKED - Movement in some engines and removed unnecessary imports.
- RE-WORKED - All core engine and optimised performance. Also, corrected a few small issues and done unnecessary code clean up.
- RE-WORKED - All AI types for better working/performance.
- RE-WORKED - Unnecessary code clean up, some old code corrections and whole code re-format for better working/performance.
- RE-WORKED - Npc instances (Like L2OFF).
- RE-WORKED - Attacking system (Like L2OFF).
- RE-WORKED - Frozen Labyrinth: Changed Spawns/Groups and Changed Mobs Social Aggression.

# --------------------

- CLEAN UP - multisells which are not default and unusing.
- CLEAN UP - Unnecessary imports, configs and settings.
- CLEAN UP - imports/unused codes and configs for better performance.
- CLEAN UP - many unnecessary code/configs for better performance.
- CLEAN UP - A lot of unnecessary code which left from older updates.

# --------------------

- REMOVED - Buffs inside nests on Swamp Of Scream, replaced it by Debuffs Poison and Decrease P.Def (Like L2OFF).
- REMOVED - Tanor Canyon Fast Spot (Too much mobs in one place).

# --------------------

- IMPROVED - Geodata near Swamp of Screams region.

# --------------------

- INCREASED - Interval of sending packets for: Attack follow and Follow. Also, added a config for this.


[![forthebadge made-with-python](http://ForTheBadge.com/images/badges/made-with-python.svg)](https://www.python.org/)
[![ForTheBadge built-with-love](http://ForTheBadge.com/images/badges/built-with-love.svg)](https://GitHub.com/Naereen/)

[![Github all releases](https://img.shields.io/github/downloads/Naereen/StrapDown.js/total.svg)](https://github.com/Hl4p3x/L2jOrion_Interlude/releases) [![GPLv3 license](https://img.shields.io/badge/License-GPLv3-blue.svg)](http://perso.crans.org/besson/LICENSE.html) [![Open Source Love svg1](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/)

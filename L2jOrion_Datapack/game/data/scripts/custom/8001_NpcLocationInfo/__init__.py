import sys
from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "8001_NpcLocationInfo"

NPC = [30598,30599,30600,30601,30602]

#NpcId:[x,y,z] #name
RADAR={

# Talking Island
30006:[-84108,244604,-3729], #Gatekeeper Roxxy
30039:[-82236,241573,-3728], #Captain Gilbert
30040:[-82515,241221,-3728], #Guard Leon
30041:[-82319,244709,-3727], #Guard Arnold
30042:[-82659,244992,-3717], #Guard Abellos
30043:[-86114,244682,-3727], #Guard Johnstone
30044:[-86328,244448,-3724], #Guard Chiperan
30045:[-86322,241215,-3727], #Guard Kenyos
30046:[-85964,240947,-3727], #Guard Hanks
30283:[-85026,242689,-3729], #Blacksmith Altran
30003:[-83789,240799,-3717], #Trader Silvia
30004:[-84204,240403,-3717], #Trader Katerina
30001:[-86385,243267,-3717], #Trader Lector
30002:[-86733,242918,-3717], #Trader Jackson
30031:[-84516,245449,-3714], #High Priest Biotin
30033:[-84729,245001,-3726], #Magister Baulro
30035:[-84965,245222,-3726], #Magister Harrys
30032:[-84981,244764,-3726], #Priest Yohanes
30036:[-85186,245001,-3726], #Priest Petron
30026:[-83326,242964,-3718], #Grand Master Bitz
30027:[-83020,242553,-3718], #Master Gwinter
30029:[-83175,243065,-3718], #Master Minia
30028:[-82809,242751,-3718], #Master Pintage
30054:[-81895,243917,-3721], #Warehouse Keeper Rant
30055:[-81840,243534,-3721], #Warehouse Keeper Rolfe
30005:[-81512,243424,-3720], #Warehouse Keeper Wilford
30048:[-84436,242793,-3729], #Darin
30312:[-78939,240305,-3443], #Lighthouse Keeper Rockswell
30368:[-85301,244587,-3725], #Lilith
30049:[-83163,243560,-3728], #Bonnie
30047:[-97131,258946,-3622], #Wharf Manager Firon
30497:[-114685,222291,-2925],#Edmond
30050:[-84057,242832,-3729], #Elias
30311:[-100332,238019,-3573],#Sir Collin Windawood
30051:[-82041,242718,-3725], #Cristel

# Dark Elf Village
30134:[9670,15537,-4499],  #Gatekeeper Jasmine
30224:[15120,15656,-4301], #Sentry Knight Rayla
30348:[17306,13592,-3649], #Sentry Nelsya
30355:[15272,16310,-4302], #Sentry Roselyn
30347:[6449,19619,-3619],  #Sentry Marion
30432:[-15404,71131,-3370],#Sentry Irene
30356:[7490,17397,-4378],  #Sentry Altima
30349:[17102,13002,-3668], #Sentry Jenna
30346:[6532,19903,-3618],  #Sentry Kayleen
30433:[-15648,71405,-3376],#Sentry Kathaway
30357:[7634,18047,-4378],  #Sentry Kristin
30431:[-1301,75883,-3491], #Sentry Eriel
30430:[-1152,76125,-3491], #Sentry Trionell
30307:[10584,17574,-4557], #Blacksmith Karrod
30138:[12009,15704,-4555], #Trader Minaless
30137:[11951,15661,-4555], #Trader Vollodos
30135:[10761,17970,-4558], #Trader Iria
30136:[10823,18013,-4558], #Trader Payne
30143:[11283,14226,-4167], #Master Trudy
30360:[10447,14620,-4167], #Master Harant
30145:[11258,14431,-4167], #Master Vlasty
30135:[10761,17970,-4558], #Magister Harne
30144:[10344,14445,-4167], #Tetrarch Vellior
30358:[10775,14190,-4167], #Tetrarch Thifiell
30359:[11235,14078,-4167], #Tetrarch Kaitar
30141:[11012,14128,-4167], #Tetrarch Talloth
30139:[13380,17430,-4544], #Warehouse Keeper Dorankus
30140:[13464,17751,-4544], #Warehouse Keeper Erviante
30350:[13763,17501,-4544], #Warehouse Freightman Carlon
30421:[-44225,79721,-3577],#Varika
30419:[-44015,79683,-3577],#Arkenia
30130:[25856,10832,-3649], #Abyssal Celebrant Undrias
30351:[12328,14947,-4499], #Astaron
30353:[13081,18444,-4498], #Jughead
30354:[12311,17470,-4499], #Jewel

# Elven Village
30146:[46926,51511,-2977], #Gatekeeper Mirabel
30285:[44995,51706,-2803], #Sentinel Gartrandell
30284:[45727,51721,-2803], #Sentinel Knight Alberius
30221:[42812,51138,-2996], #Sentinel Rayen
30217:[45487,46511,-2996], #Sentinel Berros
30219:[47401,51764,-2996], #Sentinel Veltress
30220:[42971,51372,-2996], #Sentinel Starden
30218:[47595,51569,-2996], #Sentinel Kendell
30216:[45778,46534,-2996], #Sentinel Wheeler
30363:[44476,47153,-2984], #Blacksmith Aios
30149:[42700,50057,-2984], #Trader Creamees
30150:[42766,50037,-2984], #Trader Herbiel
30148:[44683,46952,-2981], #Trader Ariel
30147:[44667,46896,-2982], #Trader Unoren
30155:[45725,52105,-2795], #Master Ellenia
30156:[44823,52414,-2795], #Master Cobendell
30157:[45000,52101,-2795], #Magister Greenis
30158:[45919,52414,-2795], #Magister Esrandell
30154:[44692,52261,-2795], #Hierarch Asterios
30153:[47780,49568,-2983], #Warehouse Keeper Markius
30152:[47912,50170,-2983], #Warehouse Keeper Julia
30151:[47868,50167,-2983], #Warehouse Freightman Chad
30423:[28928,74248,-3773], #Northwind
30414:[43673,49683,-3046], #Rosella
31853:[50592,54896,-3376], #Treant Bremec
30223:[42978,49115,-2994], #Arujien
30362:[46475,50495,-3058], #Andellia
30222:[45859,50827,-3058], #Alshupes
30371:[51210,82474,-3283], #Thalia
31852:[49262,53607,-3216], #Pixy Murika

# Dwarven Village
30540:[115072,-178176,-906], #Gatekeeper Wirphy
30541:[117847,-182339,-1537],#Protector Paion
30542:[116617,-184308,-1569],#Defender Runant
30543:[117826,-182576,-1537],#Defender Ethan
30544:[116378,-184308,-1571],#Defender Cromwell
30545:[115183,-176728,-791], #Defender Proton
30546:[114969,-176752,-790], #Defender Dinkey
30547:[117366,-178725,-1118],#Defender Tardyon
30548:[117378,-178914,-1120],#Defender Nathan
30531:[116226,-178529,-948], #Iron Gate's Lockirin
30532:[116190,-178441,-948], #Golden Wheel's Spiron
30533:[116016,-178615,-948], #Silver Scale's Balanki
30534:[116190,-178615,-948], #Bronze Key's Keef
30535:[116103,-178407,-948], #Filaur of the Gray Pillar
30536:[116103,-178653,-948], #Black Anvil's Arin
30525:[115468,-182446,-1434],#Head Blacksmith Bronk
30526:[115315,-182155,-1444],#Blacksmith Brunon
30527:[115271,-182692,-1445],#Blacksmith Silvera
30518:[115900,-177316,-915], #Trader Garita
30519:[116268,-177524,-914], #Trader Mion
30516:[115741,-181645,-1344],#Trader Reep
30517:[116192,-181072,-1344],#Trader Shari
30520:[115205,-180024,-870], #Warehouse Chief Reed
30521:[114716,-180018,-871], #Warehouse Freightman Murdoc
30522:[114832,-179520,-871], #Warehouse Keeper Airy
30523:[115717,-183488,-1483],#Collector Gouph
30524:[115618,-183265,-1483],#Collector Pippi
30537:[114348,-178537,-813], #Daichir, Priest of the Eart
30650:[114990,-177294,-854], #Priest of the Earth Gerald
30538:[114426,-178672,-812], #Priest of the Earth Zimenf
30539:[114409,-178415,-812], #Priestess of the Earth Chichirin
30671:[117061,-181867,-1413],#Captain Croto
30651:[116164,-184029,-1507],#Wanderer Dorf
30550:[115563,-182923,-1448],#Gauri Twinklerock
30554:[112656,-174864,-611], #Miner Bolter
30553:[116852,-183595,-1566],#Maryse Redbonnet

# Orc Village
30576:[-45264,-112512,-235], #Gatekeeper Tamil
30577:[-46576,-117311,-242], #Praetorian Rukain
30578:[-47360,-113791,-237], #Centurion Nakusin
30579:[-47360,-113424,-235], #Centurion Tamai
30580:[-45744,-117165,-236], #Centurion Parugon
30581:[-46528,-109968,-250], #Centurion Orinak
30582:[-45808,-110055,-255], #Centurion Tiku
30583:[-45731,-113844,-237], #Centurion Petukai
30584:[-45728,-113360,-237], #Centurion Vapook
30569:[-45952,-114784,-199], #Prefect Brukurse
30570:[-45952,-114496,-199], #Prefect Karukia
30571:[-45863,-112621,-200], #Seer Tanapi
30572:[-45864,-112540,-199], #Seer Livina
30564:[-43264,-112532,-220], #Blacksmith Sumari
30560:[-43910,-115518,-194], #Trader Uska
30561:[-43950,-115457,-194], #Trader Papuma
30558:[-44416,-111486,-222], #Trader Jakal
30559:[-43926,-111794,-222], #Trader Kunai
30562:[-43109,-113770,-221], #Warehouse Keeper Grookin
30563:[-43114,-113404,-221], #Warehouse Keeper Imantu
30565:[-46768,-113610,-3],   #Flame Lord Kakai
30566:[-46802,-114011,-112], #Atuba Chief Varkees
30567:[-46247,-113866,-21],  #Neruga Chief Tantus
30568:[-46808,-113184,-112], #Urutu Chief Hatos
30585:[-45328,-114736,-237], #Tataru Zu Hestui
30587:[-44624,-111873,-238], #Gantaki Zu Urutu
}

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player) :
   htmltext=event
   st = player.getQuestState(qn)
   if not st: return
   if event.isdigit() :
     htmltext = None
     npcId = int(event)
     if npcId in RADAR.keys() :
       x,y,z=RADAR[npcId] #L2EMU_EDIT Visor123 fix
       st.addRadar(x,y,z)
       htmltext = "MoveToLoc.htm"
     st.exitQuest(1)
   return htmltext

 def onTalk (Self,npc,player):
   npcId = npc.getNpcId()
   if npcId in NPC :
     htmltext = str(npcId) + ".htm"
   return htmltext

QUEST = Quest(8001,qn,"custom")

for i in NPC:
    QUEST.addStartNpc(i)
    QUEST.addTalkId(i)
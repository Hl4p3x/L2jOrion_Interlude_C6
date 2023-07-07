import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import RadarControl

qn = "8000_RaidbossInfo"

NPC = range(31729,31842)

#NpcId:[x,y,z] #name (lvl)
RADAR={

# lvl20 list
25001:[-54464,146572,-2400],  #Greyclaw Kutus (lv23)
25019:[7352,169433,-3172],    #Pan Dryad (lv25)
25038:[-57366,186276,-4804],  #Tirak (lv28)
25060:[-60427,188266,-4352],  #Unrequited Kael (lv24)
25076:[-61041,127347,-2512],  #Princess Molrang (lv25)
25095:[-37799,198120,-2200],  #Elf Renoa (lv29)
25127:[-47634,219274,-1936],  #Langk Matriarch Rashkos (lv24)
25146:[-13698,213796,-3300],  #Evil spirit Bifrons (lv21)
25149:[-12652,138200,-3120],  #Zombie Lord Crowl (lv25)
25166:[-21778,152065,-2636],  #Ikuntai (lv25)
25272:[49194,127999,-3161],   #Partisan Leader Talakin (lv28)
25357:[-3451,112819,-3032],   #Sukar Wererat Chief (lv21)
25360:[29064,179362,-3128],   #Tiger Hornet (lv26)
25362:[-55791,186903,-2856],  #Tracker Leader Sharuk (lv23)
25365:[-62171,190489,-3160],  #Patriarch Kuroboros (lv26)
25366:[-62342,179572,-3088],  #Kuroboros' Priest (lv23)
25369:[-45713,111186,-3280],  #Soul Scavenger (lv25)
25372:[48003,243395,-6562],   #Discarded Guardian (lv20)
25373:[9661,76976,-3652],     #Malex, Herald of Dagoniel (lv21)
25375:[22523,80431,-2772],    #Zombie Lord Farakelsus (lv20)
25378:[-53970,84334,-3048],   #Madness Beast (lv20)
25380:[-47412,51647,-5659],   #Kaysha, Herald of Icarus (lv21)
25426:[-18053,-101274,-1580], #Freki, Betrayer of Urutu (lv25)
25429:[172122,-214776,-3064], #Mammon's Collector Talloth (lv25)

#lvl30 list
25004:[-94208,100240,-3520],  #Turek Mercenary Captain (lv30)
25020:[90384,125568,-2128],   #Breka Warlock Pastu (lv34)
25023:[27280,101744,-3696],   #Swamp Stakato Queen Zyrnna (lv34)
25041:[10416,126880,-3676],   #Remmel (lv35)
25063:[-91024,116304,-3466],  #Chertuba of Great Soul (lv35)
25079:[53712,102656,-1072],   #Cat's Eye (lv30)
25082:[88512,140576,-3483],   #Leader of Cat Gang (lv39)
25098:[-5937,175004,-2940],   #Sejarr's Servitor (lv35)
25112:[116128,139392,-3640],  #Meana, Agent of Beres (lv30)
25118:[50896,146576,-3645],   #Guilotine, Warden of the Execution Grounds (lv35)
25128:[17696,179056,-3520],   #Vuku Grand Seer Gharmash (lv33)
25152:[43872,123968,-2928],   #Flame Lord Shadar (lv35)
25169:[-54464,170288,-3136],  #Ragraman (lv30)
25170:[26064,121808,-3738],   #Lizardmen Leader Hellion (lv38)
25185:[88123,166312,-3412],   #Tasaba Patriarch Hellena (lv35)
25188:[88256,176208,-3488],   #Apepi (lv30)
25189:[68832,203024,-3547],   #Cronos's Servitor Mumu (lv34)
25211:[76352,193216,-3648],   #Sebek (lv36)
25223:[43152,152352,-2848],   #Soul Collector Acheron (lv35)
25352:[-16912,174912,-3264],  #Giant Wastelands Basilisk (lv30)
25354:[-16096,184288,-3817],  #Gargoyle Lord Sirocco (lv35)
25383:[51632,153920,-3552],   #Ghost of Sir Calibus (lv34)
25385:[53600,143472,-3872],   #Evil Spirit Tempest (lv36)
25388:[40128,101920,-1241],   #Red Eye Captain Trakia (lv35)
25391:[45600,120592,-2455],   #Nurka's Messenger (lv33)
25392:[29928,107160,-3708],   #Captain of Queen's Royal Guards (lv32)
25394:[101888,200224,-3680],  #Premo Prime (lv38)
25398:[5000,189000,-3728],    #Eye of Beleth (lv35)
25401:[117808,102880,-3600],  #Skyla (lv32)
25404:[35992,191312,-3104],   #Corsair Captain Kylon (lv33)
#25501:[,,,],                 #Grave Robber Boss Akata (30) - not spawned yet
#25504:[,,,],                 #Nellis' Vengeful Spirit (39) - not spawned yet
#25506:[,,,],                 #Rayito The Looter (37) - not spawned yet

#lvl40 list
25007:[124240,75376,-2800],   #Retreat Spider Cletu (lv42)
25026:[92976,7920,-3914],     #Katu Van Leader Atui (lv49)
25044:[107792,27728,-3488],   #Barion (lv47)
25047:[116352,27648,-3319],   #Karte (lv49)
25057:[107056,168176,-3456],  #Biconne of Blue Sky (lv45)
25064:[92528,84752,-3703],    #Mystic of Storm Teruk (lv40)
25085:[66944,67504,-3704],    #Timak Orc Chief Ranger (lv44)
25088:[90848,16368,-5296],    #Crazy Mechanic Golem (lv43)
25099:[64048,16048,-3536],    #Rotten Tree Repiro (lv44)
25102:[113840,84256,-2480],   #Shacram (lv45)
25115:[94000,197500,-3300],   #Icarus Sample 1 (lv40)
25134:[87536,75872,-3591],    #Leto Chief Talkin (lv40)
25155:[73520,66912,-3728],    #Shaman King Selu (lv40)
25158:[77104,5408,-3088],     #King Tarlk (lv48)
25173:[75968,110784,-2512],   #Tiger King Karuta (lv45)
25192:[125920,190208,-3291],  #Earth Protector Panathen (lv43)
25208:[73776,201552,-3760],   #Water Couatle Ateka (lv40)
25214:[112112,209936,-3616],  #Fafurion's Page Sika (lv40)
25260:[93120,19440,-3607],    #Iron Giant Totem (lv45)
25395:[15000,119000,-11900],  #Archon Suscepter (lv45)
25410:[72192,125424,-3657],   #Road Scavenger Leader (lv40)
25412:[81920,113136,-3056],   #Necrosentinel Royal Guard (lv47)
25415:[128352,138464,-3467],  #Nakondas (lvl40)
25418:[62416,8096,-3376],     #Dread Avenger Kraven (lv44)
25420:[42032,24128,-4704],    #Orfen's Handmaiden (lv48)
25431:[79648,18320,-5232],    #Flame Stone Golem (lv44)
25437:[67296,64128,-3723],    #Timak Orc Gosmos (lv45)
25438:[107000,92000,-2272],   #Thief Kelbar (lv44)
25441:[111440,82912,-2912],   #Evil Spirit Cyrion (lv45)
25456:[133632,87072,-3623],   #Mirror of Oblivion (lv49)
25487:[83056,183232,-3616],   #Water Spirit Lian (lv40)
25490:[86528,216864,-3584],   #Gwindorr (lv40)
25498:[126624,174448,-3056],  #Fafurion's Henchman Istary (lv45)

#lvl50 list
25010:[113920,52960,-3735],   #Furious Thieles (lv55)
25013:[169744,11920,-2732],   #Spiteful Soul of Peasant Leader (lv50)
25029:[54941,206705,-3728],   #Atraiban (lv53)
25032:[88532,245798,-10376],  #Eva's Guardian Millenu (58)
25050:[125520,27216,-3632],   #Verfa (lv51)
25067:[94992,-23168,-2176],   #Shaka, Captain of the Red Flag (lv52)
25070:[125600,50100,-3600],   #Enchanted Forest Watcher Ruell (lv55)
25089:[165424,93776,-2992],   #Soulless Wild Boar (lv59)
25103:[135872,94592,-3735],   #Sorcerer Isirr (lv55)
25119:[121872,64032,-3536],   #Berun, Messenger of the Fairy Queen (lv50)
25122:[86300,-8200,-3000],    #Hopeful Refugee Leo (lv56)
25131:[75488,-9360,-2720],    #Carnage Lord Gato (lv50)
25137:[125280,102576,-3305],  #Sephia, Seer of Bereth (lv55)
25159:[124984,43200,-3625],   #Unicorn Paniel (lv54)
25176:[92544,115232,-3200],   #Black Lily (55)
25182:[41966,215417,-3728],   #Demon Kurikups (59)
25217:[89904,105712,-3292],   #Cursed Clara (lv50)
25230:[66672,46704,-3920],    #Ragoth, Seer of Timak (lv57)
25238:[155000,85400,-3200],   #Abyss Brukunt (59)
25241:[165984,88048,-2384],   #Harit Hero Tamash (lv55)
25259:[42050,208107,-3752],   #Zaken's Butcher Krantz (lv55)
25273:[23800,119500,-8976],   #Carnamakos (50)
25277:[54651,180269,-4976],   #Lilith's Witch Marilion (lv50)
25280:[85622,88766,-5120],    #Pagan Watcher Cerberon (lv55)
25434:[104096,-16896,-1803],  #Bandit Leader Barda (lv55)
25460:[150304,67776,-3688],   #Deadman Ereve (lv51)
25463:[166288,68096,-3264],   #Harit Guardian Garangky (lv56)
25473:[175712,29856,-3776],   #Grave Robber Kim (lv52)
25475:[183568,24560,-3184],   #Ghost Knight Kabed (lv55)
25481:[53517,205413,-3728],   #Magus Kenishee (lv53)
25484:[43160,220463,-3680],   #Zaken's Mate Tillion (lv50)
25493:[83174,254428,-10873],  #Eva's Spirit Niniel (lv55)
25496:[88300,258000,-10200],  #Fafurion's Envoy Pingolpin (lv52)
#25509:[,,,],                 #Dark Shaman Varangka (53) - not spawned yet
#25512:[,,,],                 #Gigantic Chaos Golem (52) - not spawned yet
#29060:[,,,],                 #Captain Of The Ice Queen's Royal Guard (59) - not spawned yet

#lvl60 list
25016:[76787,245775,-10376],  #The 3rd Underwater Guardian (lv60)
25051:[117760,-9072,-3264],   #Rahha (lv65)
25073:[143265,110044,-3944],  #Bloody Priest Rudelto (lv69)
25106:[173880,-11412,-2880],  #Lidia, Ghost of the Well (lv60)
25125:[170656,85184,-2000],   #Fierce Tiger King Angel (lv65)
25140:[191975,56959,-7616],   #Hekaton Prime (lv65)
25162:[194107,53884,-4368],   #Giant Marpanak (lv60)
25179:[181814,52379,-4344],   #Karum, Guardian Of The Statue Of the Giant (60)
25226:[104240,-3664,-3392],   #Roaring Lord Kastor (lv62)
25233:[185800,-26500,-2000],  #Spiteful Soul of Andras the Betrayer (lv69)
25234:[120080,111248,-3047],  #Ancient Weird Drake (lv65)
25255:[170048,-24896,-3440],  #Gargoyle Lord Tiphon (lv65)
25256:[170320,42640,-4832],   #Taik High Prefect Arak (lv60)
25263:[144400,-28192,-1920],  #Kernon's Faithful Servant Kelone (67)
25322:[93296,-75104,-1824],   #Demon's Agent Falston (lv66)
25407:[115072,112272,-3018],  #Lord Ishka (lv60)
25423:[113600,47120,-4640],   #Fairy Queen Timiniel (61)
25444:[113232,17456,-4384],   #Enmity Ghost Ramdal (lv65)
25467:[186192,61472,-4160],   #Gorgolos (lv64)
25470:[186896,56276,-4576],   #Utenus, the Last Titan (lv66)
25478:[168288,28368,-3632],   #Hisilrome, Priest of Shilen (lv65)
#29056:[,,,],                 #Ice Fairy Sirra (60) - not spawned yet

#lvl70 list
25035:[180968,12035,-2720],   #Shilen's Messenger Cabrio (lv70)
25054:[113432,16403,3960],    #Kernon (lv75)
25092:[116151,16227,1944],    #Korim (lv70)
25109:[152660,110387,-5520],  #Cloe, Priest of Antharas (lv74)
25126:[116263,15916,6992],    #Longhorn Golkonda (lv79)
25143:[113102,16002,6992],    #Shuriel, Fire of Wrath (lv78)
25163:[130500,59098,3584],    #Roaring Skylancer (lv70)
25198:[102656,157424,-3735],  #Fafurion's Messenger Loch Ness (lv70)
25199:[108096,157408,-3688],  #Fafurion's Seer Sheshark (lv72)
25202:[119760,157392,-3744],  #Crokian Padisha Sobekk (lv74)
25205:[123808,153408,-3671],  #Ocean's Flame Ashakiel (lv76)
25220:[113551,17083,-2120],   #Death Lord Hallate (lv73)
25229:[137568,-19488,-3552],  #Storm Winged Naga (lv75)
25235:[116400,-62528,-3264],  #Vanor Chief Kandra (lv72)
25244:[187360,45840,-5856],   #Last Lesser Giant Olkuth (lv75)
25245:[172000,55000,-5400],   #Last Lesser Giant Glaki (lv78)
25248:[127903,-13399,-3720],  #Doom Blade Tanatos (lv72)
25249:[147104,-20560,-3377],  #Palatanos of the Fearsome Power (lv75)
25252:[192376,22087,-3608],   #Palibati Queen Themis (lv70)
25266:[188983,13647,-2672],   #Bloody Empress Decarbia (lv75)
25269:[123504,-23696,-3481],  #Beast Lord Behemoth (lv70)
25276:[154088,-14116,-3736],  #Death Lord Ipos (lv75)
25281:[151053,88124,-5424],   #Anakim's Nemesis Zakaron (lv70)
25282:[179311,-7632,-4896],   #Death Lord Shax (lv75)
25293:[134672,-115600,-1216], #Hestia, Guardian Deity of the Hot Springs (lv78)
25325:[91008,-85904,-2736],   #Barakiel, the Flame of Splendor (lv70)
25328:[59331,-42403,-3003],   #Eilhalder Von Hellman (lv71)
25447:[113200,17552,-1424],   #Immortal Savior Mardil (lv71)
25450:[113600,15104,9559],    #Cherub Galaxia (lv79)
25453:[156704,-6096,-4185],   #Minas Anor (lv70)
#25523:[,,,],                 #Plague Golem (lvl73) - not spawned yet
25524:[144143,-5731,-4722],   #Flamestone Giant (lvl76)
#25296:[,,,],                 #Icicle Emperor Bumpalump (lvl74) - quest spawn - not spawned yet
#25290:[,,,],                 #Daimon The White-Eyed (lvl78) - quest spawn - not spawned yet

#lvl80 list
25299:[148154,-73782,-4364],  #Ketra's Hero Hekaton (lv80)
25302:[145553,-81651,-5464],  #Ketra's Commander Tayr (lv84)
25305:[144997,-84948,-5712],  #Ketra's Chief Braki (lv87)
25309:[115537,-39046,-1940],  #Varka's Hero Shadith (lv80)
25312:[109296,-36103,-648],   #Varka's Commander Mos (lv84)
25315:[105654,-42995,-1240],  #Varka's Chief Horus (lv87)
25319:[185700,-106066,-6184], #Ember (lv85)
25514:[79635,-55612,-5980],   #Spiked stakato Queen Shyeed (lvl80)
25517:[112793,-76080,286],     #Master Anays (lvl87)
29062:[-16373,-53562,-10197], #High Priestess van Halter (lvl87)
25283:[185060,-9622,-5104],   #Lilith (lvl80)
25286:[185065,-12612,-5104],  #Anakim (lvl80)
25306:[142368,-82512,-6487],  #Nastron, Spirit of Fire (lvl87)
25316:[105452,-36775,-1050],  #Ashutar, Spirit of water (lvl87)
25527:[3776,-6768,-3276],     #Uruka (lvl86)
29065:[26528,-8244,-2007]     #Sailren (lvl87)
}


class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player) :
   htmltext=event
   st = player.getQuestState(qn)
   if not st: return
   if event.isdigit() :
     htmltext = None
     rbid = int(event)
     if rbid in RADAR.keys():
       x,y,z=RADAR[rbid]
       player.sendPacket(RadarControl(2, 2, x, y, z))
       player.sendPacket(RadarControl(0, 1, x, y, z))
     st.exitQuest(1)
   return htmltext

 def onTalk (Self,npc,player):
   return "info.htm"

QUEST       = Quest(8000,qn,"custom")
CREATED     = State('Start',   QUEST)

QUEST.setInitialState(CREATED)
for i in NPC:
    QUEST.addStartNpc(i)
    QUEST.addTalkId(i)
import sys

from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "elven_human_fighters_1"

#Quest items
MEDALLION_OF_WARRIOR    = 1145
SWORD_OF_RITUAL         = 1161
BEZIQUES_RECOMMENDATION = 1190
ELVEN_KNIGHT_BROOCH     = 1204
REORIA_RECOMMENDATION   = 1217
#Reward Item
SHADOW_WEAPON_COUPON_DGRADE = 8869
#PABRIS,RAINS,RAMOS
NPCS=[30066,30288,30373]
#event:[newclass,req_class,req_race,low_ni,low_i,ok_ni,ok_i,req_item]
#low_ni : level too low, and you dont have quest item
#low_i: level too low, despite you have the item
#ok_ni: level ok, but you don't have quest item
#ok_i: level ok, you got quest item, class change takes place
CLASSES = {
    "EK":[19,18,1,"18","19","20","21",ELVEN_KNIGHT_BROOCH],
    "ES":[22,18,1,"22","23","24","25",REORIA_RECOMMENDATION],
    "HW":[1,0,0,"26","27","28","29",MEDALLION_OF_WARRIOR],
    "HK":[4,0,0,"30","31","32","33",SWORD_OF_RITUAL],
    "HR":[7,0,0,"34","35","36","37",BEZIQUES_RECOMMENDATION]
    }
#Messages
default = "No Quest"

def change(st,player,newclass,item) :
   st.takeItems(item,1)
   st.playSound("ItemSound.quest_fanfare_2")
   player.setClassId(newclass)
   player.setBaseClass(newclass)
   player.broadcastUserInfo()
   return

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player) :
   npcId    = npc.getNpcId()
   htmltext = default
   suffix = ''
   st = player.getQuestState(qn)
   if not st : return
   race     = player.getRace().ordinal()
   classid  = player.getClassId().getId()
   level    = player.getLevel()
   if npcId not in NPCS : return
   if not event in CLASSES.keys() :
     return event
   else :
     newclass,req_class,req_race,low_ni,low_i,ok_ni,ok_i,req_item=CLASSES[event]
     if race == req_race and classid == req_class :
        item = st.getQuestItemsCount(req_item)
        if level < 20 :
           suffix = "-"+low_i+".htm"
           if not item :
              suffix = "-"+low_ni+".htm"
        else :
           if not item :
              suffix = "-"+ok_ni+".htm"
           else :
              suffix = "-"+ok_i+".htm"
              st.giveItems(SHADOW_WEAPON_COUPON_DGRADE,15)
              change(st,player,newclass,req_item)
     st.exitQuest(1)
     htmltext = str(npcId)+suffix
   return htmltext

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   race    = player.getRace().ordinal()
   classId = player.getClassId()
   id = classId.getId()
   htmltext = default
   if player.isSubClassActive() :
      st.exitQuest(1)
      return htmltext
   # Elven and Human fighters only
   if npcId in NPCS :
     htmltext = str(npcId)
     if race in [0,1] :
       if classId.level() == 1 :   # first occupation change already made
         htmltext += "-38.htm"
       elif classId.level() >= 2 : # second/third occupation change already made
         htmltext += "-39.htm"
       elif id == 18 :        # elven fighter
         return htmltext+"-01.htm"
       elif id == 0 :         # human fighter
         return htmltext+"-08.htm"
       else :                 # elven/human mages
         htmltext += "-40.htm"
     else :
       htmltext += "-40.htm"  # other races
   st.exitQuest(1)
   return htmltext

QUEST   = Quest(99995,qn,"village_master")
CREATED = State('Start', QUEST)

QUEST.setInitialState(CREATED)

for npc in NPCS :
    QUEST.addStartNpc(npc)
    QUEST.addTalkId(npc)
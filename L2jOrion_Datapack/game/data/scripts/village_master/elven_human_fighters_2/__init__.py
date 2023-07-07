import sys

from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import SystemMessage
from l2jorion.game.network import SystemMessageId

qn = "elven_human_fighters_2"

#Quest items
MARK_OF_CHALLENGER  = 2627
MARK_OF_DUTY        = 2633
MARK_OF_SEEKER      = 2673
MARK_OF_TRUST       = 2734
MARK_OF_DUELIST     = 2762
MARK_OF_SEARCHER    = 2809
MARK_OF_HEALER      = 2820
MARK_OF_LIFE        = 3140
MARK_OF_CHAMPION    = 3276
MARK_OF_SAGITTARIUS = 3293
MARK_OF_WITCHCRAFT  = 3307

#Reward Item
SHADOW_WEAPON_COUPON_CGRADE = 8870

#HANNAVALT,BLACKBIRD,SIRIA,SEDRICK,MARCUS,HECTOR,SCHULE
NPCS=[30109,30187,30689,30849,30900,31965,32094]
#event:[newclass,req_class,req_race,low_ni,low_i,ok_ni,ok_i,req_item]
#low_ni : level too low, and you dont have quest item
#low_i: level too low, despite you have the item
#ok_ni: level ok, but you don't have quest item
#ok_i: level ok, you got quest item, class change takes place
CLASSES = {
    "TK":[20,19,1,"36","37","38","39",[MARK_OF_DUTY,MARK_OF_LIFE,MARK_OF_HEALER]],
    "SS":[21,19,1,"40","41","42","43",[MARK_OF_CHALLENGER,MARK_OF_LIFE,MARK_OF_DUELIST]],
    "PL":[ 5, 4,0,"44","45","46","47",[MARK_OF_DUTY,MARK_OF_TRUST,MARK_OF_HEALER]],
    "DA":[ 6, 4,0,"48","49","50","51",[MARK_OF_DUTY,MARK_OF_TRUST,MARK_OF_WITCHCRAFT]],
    "TH":[ 8, 7,0,"52","53","54","55",[MARK_OF_SEEKER,MARK_OF_TRUST,MARK_OF_SEARCHER]],
    "HE":[ 9, 7,0,"56","57","58","59",[MARK_OF_SEEKER,MARK_OF_TRUST,MARK_OF_SAGITTARIUS]],
    "PW":[23,22,1,"60","61","62","63",[MARK_OF_SEEKER,MARK_OF_LIFE,MARK_OF_SEARCHER]],
    "SR":[24,22,1,"64","65","66","67",[MARK_OF_SEEKER,MARK_OF_LIFE,MARK_OF_SAGITTARIUS]],
    "GL":[ 2, 1,0,"68","69","70","71",[MARK_OF_CHALLENGER,MARK_OF_TRUST,MARK_OF_DUELIST]],
    "WL":[ 3, 1,0,"72","73","74","75",[MARK_OF_CHALLENGER,MARK_OF_TRUST,MARK_OF_CHAMPION]]    
    }
#Messages
default = "No Quest"

def change(st,player,newclass,items) :
   for item in items :
      st.takeItems(item,1)
   st.playSound("ItemSound.quest_fanfare_2")
   st.giveItems(SHADOW_WEAPON_COUPON_CGRADE,15)
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
        item = True
        for i in req_item :
            if not st.getQuestItemsCount(i):
               item = False
        if level < 40 :
           suffix = low_i
           if not item :
              suffix = low_ni
        else :
           if not item :
              suffix = ok_ni
           else :
              suffix = ok_i
              change(st,player,newclass,req_item)
     st.exitQuest(1)
     htmltext = "30109-"+suffix+".htm"
   return htmltext

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   race = player.getRace().ordinal()
   classId = player.getClassId()
   id = classId.getId()
   htmltext = default
   if player.isSubClassActive() :
      st.exitQuest(1)
      return htmltext
   if npcId in NPCS :
     htmltext = "30109"
     if race in [0,1] :    # Human and Elves only
       if id == 19 :       # elven knight
         return htmltext+"-01.htm"
       elif id == 4 :      # human knight
         return htmltext+"-08.htm"
       elif id == 7 :      # rogue
         return htmltext+"-15.htm"
       elif id == 22 :     # elven scout
         return htmltext+"-22.htm"
       elif id == 1 :      # human warrior
         return htmltext+"-29.htm"
       elif classId.level() == 0 :            # first occupation change not made yet
         htmltext += "-76.htm"
       elif classId.level() >= 2 :            # second/third occupation change already made
         htmltext += "-77.htm"
       else :
         htmltext += "-78.htm"                # other conditions
     else :
       htmltext += "-78.htm"                  # other races
   st.exitQuest(1)
   return htmltext

QUEST   = Quest(99991,qn,"village_master")
CREATED = State('Start', QUEST)

QUEST.setInitialState(CREATED)

for npc in NPCS:
    QUEST.addStartNpc(npc)
    QUEST.addTalkId(npc)

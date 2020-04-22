# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


#Complete - 100%.
qn = "286_FabulousFeathers"

#NPC'S
ERINU = 32164

#ITEM'S
FEATHER = 9746

#MOB'S
MOBS = range(22253,22257)+[22251]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
  
 def onEvent (self,event,st) :
     htmltext = event
     feather = st.getQuestItemsCount(FEATHER)
     if event == "32164-03.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
     elif event == "32164-06.htm" :
       st.giveItems(57,4160)
       st.takeItems(FEATHER,-1)
       st.playSound("ItemSound.quest_finish")
       st.unset("cond")
       st.exitQuest(1)
     return htmltext

 def onTalk (self,npc,player):
     npcId = npc.getNpcId()
     htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
     st = player.getQuestState(qn)
     if not st : return htmltext
     id = st.getState()
     cond = st.getInt("cond")
     feather = st.getQuestItemsCount(FEATHER)
     if id == CREATED and npcId == ERINU :
       if player.getLevel() < 17 :
         htmltext = "32164-02.htm"
         st.exitQuest(1)
       else :
         htmltext = "32164-01.htm"
     elif id == STARTED and npcId == ERINU :
       if cond == 1 :
         htmltext = "32164-04.htm"
       elif cond == 2 :
         htmltext = "32164-05.htm"
     return htmltext
    
 def onKill(self,npc,player,isPet) :
     st = player.getQuestState(qn)
     if not st: return
     if st.getState() == STARTED :
       npcId = npc.getNpcId()
       chance = st.getRandom(100)
       feather = st.getQuestItemsCount(FEATHER)
       if (npcId in MOBS) and (chance < 70) and feather < 80: #Retail statistic info. 113 mob's - 80 feathers
         st.giveItems(FEATHER,1)
         st.playSound("ItemSound.quest_itemget")
         if st.getQuestItemsCount(FEATHER) == 80 :
           st.set("cond","2")
           st.playSound("ItemSound.quest_middle")
     return

QUEST       = Quest(286, qn, "Fabulous Feathers")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(ERINU)

QUEST.addTalkId(ERINU)

for mob in MOBS :
    QUEST.addKillId(mob)

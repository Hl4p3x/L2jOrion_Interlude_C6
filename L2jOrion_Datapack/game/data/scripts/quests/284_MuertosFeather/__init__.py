# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

#Complete - 100%.
qn = "284_MuertosFeather"

#NPC'S
TREVOR = 32166

#ITEM'S
FEATHER = 9748

#MOB'S
MOBS = range(22239,22241)+range(22242,22244)+range(22245,22247)

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
  
 def onEvent (self,event,st) :
     htmltext = event
     feather = st.getQuestItemsCount(FEATHER)
     if event == "32166-03.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
     elif event == "32166-06.htm" :
       st.giveItems(57,feather*45)
       st.takeItems(FEATHER,-1)
     elif event == "32166-08.htm" :
       st.takeItems(FEATHER,-1)
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
     if id == CREATED and npcId == TREVOR :
       if player.getLevel() < 11 :
         htmltext = "32166-02.htm"
         st.exitQuest(1)
       else :
         htmltext = "32166-01.htm"
     elif id == STARTED and npcId == TREVOR :
       if not feather :
         htmltext = "32166-04.htm"
       else :
         htmltext = "32166-05.htm"
     return htmltext
    
 def onKill(self,npc,player,isPet) :
     st = player.getQuestState(qn)
     if not st: return
     if st.getState() == STARTED :
       npcId = npc.getNpcId()
       chance = st.getRandom(100)
       if (npcId in MOBS) and (chance < 70) : #Retail statistic info. 20 mob's - 14 feathers
         st.giveItems(FEATHER,1)
         st.playSound("ItemSound.quest_itemget")
     return

QUEST       = Quest(284, qn, "Muertos Feather")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(TREVOR)

QUEST.addTalkId(TREVOR)

for mob in MOBS :
    QUEST.addKillId(mob)

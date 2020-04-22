# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "269_InventionAmbition"

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [10866]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "32486-03.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    elif event == "32486-05.htm" :
      st.exitQuest(1)
      st.playSound("ItemSound.quest_finish")
    return htmltext

 def onTalk (self,npc,player):
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   st = player.getQuestState(qn)
   if not st : return htmltext

   npcId = npc.getNpcId()
   id = st.getState()
   EnergyOres = st.getQuestItemsCount(10866)
   if id == CREATED :
      if player.getLevel() < 18 :
         htmltext = "32486-00.htm"
         st.exitQuest(1)
      else :
         htmltext = "32486-01.htm"
   elif EnergyOres > 0:
      htmltext = "32486-07.htm"
      bonus = 0
      if EnergyOres >= 20:
         bonus = 2044
      st.giveItems(57,EnergyOres*50+bonus)
      st.takeItems(10866,-1)
   else :
      htmltext = "32486-04.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st : return 
   if st.getState() != STARTED : return 
   
   if st.getRandom(10)<6 :
     st.giveItems(10866,1)
     st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(269,qn,"Invention Ambition")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(32486)
QUEST.addTalkId(32486)
for mob in range(21124,21132) :
    QUEST.addKillId(mob)

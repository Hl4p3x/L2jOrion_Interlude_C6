# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "268_TracesOfEvil"

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [10869]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "30559-02.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    return htmltext

 def onTalk (self,npc,player):
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   st = player.getQuestState(qn)
   if not st : return htmltext

   npcId = npc.getNpcId()
   id = st.getState()
   if id == CREATED :
      if player.getLevel() < 15 :
         htmltext = "30559-00.htm"
         st.exitQuest(1)
      else :
         htmltext = "30559-01.htm"
   elif st.getQuestItemsCount(10869) >= 30:
      htmltext = "30559-04.htm"
      st.takeItems(10869,-1)
      st.giveItems(57,2474)
      st.addExpAndSp(8738,409)
      st.playSound("ItemSound.quest_finish")
      st.exitQuest(1)
   else :
      htmltext = "30559-03.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st : return 
   if st.getState() != STARTED and st.getInt("cond")!=1: return 
   if st.getQuestItemsCount(10869) < 29:
      st.playSound("ItemSound.quest_itemget")
   elif st.getQuestItemsCount(10869) >= 29:
      st.playSound("ItemSound.quest_middle")
      st.set("cond","2")
   st.giveItems(10869,1)
   return

QUEST       = Quest(268,qn,"Traces of Evil")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(30559)
QUEST.addTalkId(30559)
for mob in [20474,20476,20478] :
    QUEST.addKillId(mob)

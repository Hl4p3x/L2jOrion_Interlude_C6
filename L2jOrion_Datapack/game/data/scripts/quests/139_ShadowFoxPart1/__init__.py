# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "139_ShadowFoxPart1"

# NPCs
MIA = 30896

# ITEMs
FRAGMENT = 10345
CHEST = 10346
# MONSTERs
NPC=[20784,20785,21639,21640]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [FRAGMENT,CHEST]

 def onEvent (self,event,st) :
    htmltext = event
    id = st.getState()
    cond = st.getInt("cond")
    if event == "30896-03.htm" :
       st.set("cond","1")
       st.playSound("ItemSound.quest_accept")
    elif event == "30896-11.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
    elif event == "30896-14.htm" :
       st.takeItems(FRAGMENT, -1)
       st.takeItems(CHEST, -1)
       st.set("talk","1")
    elif event == "30896-16.htm" :
       st.playSound("ItemSound.quest_finish")
       st.unset("talk")
       st.exitQuest(False)
       st.giveItems(57, 14050)
       if st.getPlayer().getLevel() >= 37 and st.getPlayer().getLevel() <= 42:
          st.addExpAndSp(30000,2000)
       st.setState(COMPLETED)
    return htmltext

 def onTalk (self,npc,player):
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
    st = player.getQuestState(qn)
    if not st : return htmltext

    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    if id == CREATED : return htmltext
    if id == COMPLETED :
       htmltext = "<html><body>This quest has already been completed.</body></html>"
    elif npcId == MIA :
       if cond == 0 :
          if player.getLevel() >= 37:
             htmltext = "30896-01.htm"
          else:
             htmltext = "30896-00.htm"
             st.exitQuest(1)
       elif cond == 1 :
          htmltext = "30896-03.htm"
       elif cond == 2 :
          if st.getQuestItemsCount(FRAGMENT) >= 10 and st.getQuestItemsCount(CHEST) >= 1:
             htmltext = "30896-13.htm"
          elif st.getInt("talk"):
             htmltext = "30896-14.htm"
          else:
             htmltext = "30896-12.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED : return
    if st.getInt("cond")==2 :
       st.playSound("ItemSound.quest_itemget")
       st.giveItems(FRAGMENT,1)
       if st.getRandom(100) <= 2 :
          st.giveItems(CHEST,1)
    return

 def onFirstTalk (self,npc,player):
   st = player.getQuestState(qn)
   if not st :
      st = self.newQuestState(player)
   qs = player.getQuestState("138_TempleChampionPart2")
   if qs :
      if qs.getState().getName() == 'Completed' and st.getState() == CREATED :
         st.setState(STARTED)
   npc.showChatWindow(player)
   return

QUEST       = Quest(139,qn,"Shadow Fox - 1")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addFirstTalkId(MIA) #this quest doesnt have starter npc, quest will appear in list only when u finish quest 137
QUEST.addTalkId(MIA)
for mob in NPC :
   QUEST.addKillId(mob)

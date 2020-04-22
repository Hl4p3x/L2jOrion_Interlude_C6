# Rewritten by RayzoR
import sys
from l2jorion.game.managers import QuestManager
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "141_ShadowFoxPart3"

# NPCs
NATOOLS = 30894

# ITEMs
REPORT = 10350

# MONSTERs
NPC=[20791,20792,20135]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [REPORT]

 def onEvent (self,event,st) :
    htmltext = event
    id = st.getState()
    cond = st.getInt("cond")
    if event == "30894-02.htm" :
       st.set("cond","1")
       st.playSound("ItemSound.quest_accept")
    elif event == "30894-04.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
    elif event == "30894-15.htm" :
       st.set("cond","4")
       st.unset("talk")
       st.playSound("ItemSound.quest_middle")
    elif event == "30894-18.htm" :
       st.playSound("ItemSound.quest_finish")
       st.exitQuest(False)
       st.giveItems(57, 88888)
       player = st.getPlayer()
       if player.getLevel() >= 37 and player.getLevel() <= 42:
          st.addExpAndSp(278005,17058)
       st.setState(COMPLETED)
       qs = player.getQuestState("998_FallenAngelSelect")
       if not qs:
           q = QuestManager.getInstance().getQuest("998_FallenAngelSelect")
           if q :
               qs = q.newQuestState(player)
       qs.setState(STARTED)
    return htmltext

 def onTalk (self,npc,player):
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
    st = player.getQuestState(qn)
    if not st : return htmltext

    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    if id == COMPLETED :
       htmltext = "<html><body>This quest has already been completed.</body></html>"
    elif id == STARTED :
       if cond == 0 :
          if player.getLevel() >= 37:
             htmltext = "30894-01.htm"
          else:
             htmltext = "30894-00.htm"
       elif cond == 1 :
          htmltext = "30894-02.htm"
       elif cond == 2 :
          htmltext = "30894-05.htm"
       elif cond == 3 :
          if st.getInt("talk"):
             htmltext = "30894-07.htm"
          else:
             htmltext = "30894-06.htm"
             st.takeItems(REPORT, -1)
             st.set("talk","1")
       elif cond == 4 :
          htmltext = "30894-16.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED : return
    if st.getInt("cond")==2 and st.getRandom(100) <= 80 and st.getQuestItemsCount(REPORT)<30:
       st.giveItems(REPORT,1)
       if st.getQuestItemsCount(REPORT)>=30:
          st.set("cond","3")
          st.playSound("ItemSound.quest_middle")
       else:
          st.playSound("ItemSound.quest_itemget")
    return

 def onFirstTalk (self,npc,player):
   st = player.getQuestState(qn)
   if not st :
      qs = player.getQuestState("140_ShadowFoxPart2")
      st = self.newQuestState(player)
      if qs :
          if qs.getState().getName() == 'Completed' and st.getState() == CREATED :
              st.setState(STARTED)
   elif st.getState() == COMPLETED and player.getLevel() >= 38 :
      qs2 = player.getQuestState("998_FallenAngelSelect")
      qs3 = player.getQuestState("142_FallenAngelRequestOfDawn")
      qs4 = player.getQuestState("143_FallenAngelRequestOfDusk")
      if qs2 :
         if qs.getState().getName() == 'Completed' and not (qs3 or qs4) :
             qs2.setState(STARTED)
   npc.showChatWindow(player)
   return

QUEST       = Quest(141,qn,"Shadow Fox - 3")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addFirstTalkId(NATOOLS) #this quest doesnt have starter npc, quest will appear in list only when u finish quest 140
QUEST.addTalkId(NATOOLS)
for mob in NPC :
   QUEST.addKillId(mob)
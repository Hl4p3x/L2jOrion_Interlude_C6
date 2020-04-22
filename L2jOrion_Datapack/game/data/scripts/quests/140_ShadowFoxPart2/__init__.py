# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "140_ShadowFoxPart2"

# NPCs
KLUCK = 30895
XENOVIA = 30912

# ITEMs
CRYSTAL = 10347
OXYDE = 10348
CRYPT = 10349

# MONSTERs
NPC=[20789,20790,20791,20792]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [CRYSTAL,OXYDE,CRYPT]

 def onEvent (self,event,st) :
    htmltext = event
    id = st.getState()
    cond = st.getInt("cond")
    if event == "30895-02.htm" :
       st.set("cond","1")
       st.playSound("ItemSound.quest_accept")
    elif event == "30895-05.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
    elif event == "30895-09.htm" :
       st.playSound("ItemSound.quest_finish")
       st.unset("talk")
       st.exitQuest(False)
       st.giveItems(57, 18775)
       if st.getPlayer().getLevel() >= 37 and st.getPlayer().getLevel() <= 42:
          st.addExpAndSp(30000,2000)
       st.setState(COMPLETED)
    elif event == "30912-07.htm" :
       st.set("cond","3")
       st.playSound("ItemSound.quest_middle")
    elif event == "30912-09.htm" :
       st.takeItems(CRYSTAL, 5)
       if st.getRandom(100) <= 60 :
          st.giveItems(OXYDE,1)
          if st.getQuestItemsCount(OXYDE) >= 3 :
             htmltext = "30912-09b.htm"
             st.set("cond","4")
             st.playSound("ItemSound.quest_middle")
             st.takeItems(CRYSTAL, -1)
             st.takeItems(OXYDE, -1)
             st.giveItems(CRYPT,1)
       else:
          htmltext = "30912-09a.htm"
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
    elif npcId == KLUCK :
       if cond == 0 :
          if player.getLevel() >= 37:
             htmltext = "30895-01.htm"
          else:
             htmltext = "30895-00.htm"
       elif cond == 1 :
          htmltext = "30895-02.htm"
       elif cond in [2,3] :
          htmltext = "30895-06.htm"
       elif cond == 4 :
          if st.getInt("talk"):
             htmltext = "30895-08.htm"
          else:
             htmltext = "30895-07.htm"
             st.takeItems(CRYPT, -1)
             st.set("talk","1")
    elif npcId == XENOVIA :
       if cond == 2 :
          htmltext = "30912-01.htm"
       elif cond == 3 :
          if st.getQuestItemsCount(CRYSTAL) >= 5 :
             htmltext = "30912-08.htm"
          else:
             htmltext = "30912-07.htm"
       elif cond == 4 :
          htmltext = "30912-10.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED : return
    if st.getInt("cond")==3 and st.getRandom(100) <= 80 :
       st.playSound("ItemSound.quest_itemget")
       st.giveItems(CRYSTAL,1)
    return

 def onFirstTalk (self,npc,player):
    st = player.getQuestState(qn)
    if not st :
       st = self.newQuestState(player)
    qs = player.getQuestState("139_ShadowFoxPart1")
    if qs :
       if qs.getState().getName() == 'Completed' and st.getState() == CREATED :
           st.setState(STARTED)
    npc.showChatWindow(player)
    return

QUEST       = Quest(140,qn,"Shadow Fox - 2")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addFirstTalkId(KLUCK) #this quest doesnt have starter npc, quest will appear in list only when u finish quest 139
QUEST.addTalkId(KLUCK)
QUEST.addTalkId(XENOVIA)

for mob in NPC :
   QUEST.addKillId(mob)
# Rewritten by Qwerty
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "138_TempleChampionPart2"

# NPCs
SYLVAIN = 30070
PUPINA = 30118
ANGUS = 30474
SLA = 30666

# ITEMs
MANIFESTO = 10341
RELIC = 10342
ANGUS_REC = 10343
PUPINA_REC = 10344

# MONSTERs
NPC=[20176,20550,20551,20552]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [MANIFESTO,RELIC,ANGUS_REC,PUPINA_REC]

 def onEvent (self,event,st) :
    htmltext = event
    id = st.getState()
    cond = st.getInt("cond")
    if event == "30070-02.htm" :
      st.set("cond","1")
      st.playSound("ItemSound.quest_accept")
      st.giveItems(MANIFESTO, 1)
    elif event == "30070-05.htm" :
      st.giveItems(57, 84593)
      st.playSound("ItemSound.quest_finish")
      st.exitQuest(False)
      if st.getPlayer().getLevel() >= 36 and st.getPlayer().getLevel() <= 41:
        st.addExpAndSp(187062,11307)
      st.setState(COMPLETED)
    elif event == "30070-03.htm" :
      st.set("cond","2")
      st.playSound("ItemSound.quest_middle")
    elif event == "30118-06.htm" :
      st.set("cond","3")
      st.playSound("ItemSound.quest_middle")
    elif event == "30118-09.htm" :
      st.set("cond","6")
      st.playSound("ItemSound.quest_middle")
      st.set("talk","0")
      st.giveItems(PUPINA_REC, 1)
    elif event == "30474-02.htm" :
      st.set("cond","4")
      st.playSound("ItemSound.quest_middle")
    elif event == "30666-02.htm" :
      st.set("talk","1")
      st.takeItems(PUPINA_REC, -1)
    elif event == "30666-03.htm" :
      st.set("talk","2")
      st.takeItems(MANIFESTO, -1)
    elif event == "30666-08.htm" :
      st.set("cond","7")
      st.playSound("ItemSound.quest_middle")
      st.unset("talk")
    return htmltext

 def onTalk (self,npc,player):
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
    st = player.getQuestState(qn)
    if not st :
      return htmltext
    if st.getState() == CREATED :
      qs = player.getQuestState("137_TempleChampionPart1")
      if qs :
        if qs.getState().getName() == 'Completed' :
          st.setState(STARTED)
    npc.showChatWindow(player)
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    if id == CREATED :
      return htmltext
    if id == COMPLETED :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
    elif npcId == SYLVAIN :
      if cond == 0 :
        if player.getLevel() >= 36:
          htmltext = "30070-01.htm"
        else:
          htmltext = "30070-00.htm"
      elif cond == 1 :
        htmltext = "30070-02.htm"
      elif cond in [2,3,4,5,6] :
        htmltext = "30070-03.htm"
      elif cond == 7 :
        htmltext = "30070-04.htm"
    elif npcId == PUPINA :
      if cond == 2 :
        htmltext = "30118-01.htm"
      elif cond in [3,4] :
        htmltext = "30118-07.htm"
      elif cond == 5 :
        htmltext = "30118-08.htm"
        st.takeItems(ANGUS_REC, -1)
      elif cond == 6 :
        htmltext = "30118-10.htm"
    elif npcId == ANGUS :
      if cond == 3 :
        htmltext = "30474-01.htm"
      elif cond == 4 :
        if st.getQuestItemsCount(RELIC) >= 10:
          htmltext = "30474-04.htm"
          st.takeItems(RELIC, -1)
          st.giveItems(ANGUS_REC, 1)
          st.set("cond","5")
          st.playSound("ItemSound.quest_middle")
        else:
          htmltext = "30474-03.htm"
      elif cond == 5 :
        htmltext = "30474-05.htm"
    elif npcId == SLA :
      if cond == 6 :
        if st.getInt("talk") == 0:
          htmltext = "30666-01.htm"
        elif st.getInt("talk") == 1:
          htmltext = "30666-02.htm"
        elif st.getInt("talk") == 2:
          htmltext = "30666-03.htm"
      elif cond == 7 :
        htmltext = "30666-09.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED :
      return
    if st.getInt("cond")==4 :
      if st.getQuestItemsCount(RELIC) < 10:
        st.giveItems(RELIC,1)
        if st.getQuestItemsCount(RELIC) >= 10:
          st.playSound("ItemSound.quest_middle")
        else :
          st.playSound("ItemSound.quest_itemget")
    return

QUEST       = Quest(138,qn,"Temple Champion - 2")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addTalkId(SYLVAIN)
QUEST.addTalkId(PUPINA)
QUEST.addTalkId(ANGUS)
QUEST.addTalkId(SLA)
for mob in NPC :
   QUEST.addKillId(mob)
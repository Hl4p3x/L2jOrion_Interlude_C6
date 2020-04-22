# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "134_TempleMissionary"

# NPCs
GLYVKA = 30067
ROUKE = 31418

# ITEMs
FRAGMENT = 10335
TOOL = 10336
REPORT = 10337
REPORT2 = 10338
BADGE = 10339

# MONSTERs
NPC = [20157,20229,20230,20231,20232,20233,20234,27339]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [FRAGMENT,TOOL,REPORT,REPORT2]

 def onEvent (self,event,st) :
    htmltext = event
    id = st.getState()
    cond = st.getInt("cond")
    if event == "30067-02.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
    elif event == "30067-04.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
    elif event == "30067-08.htm" :
       st.playSound("ItemSound.quest_finish")
       st.unset("talk")
       st.exitQuest(False)
       st.giveItems(57, 15100)
       st.giveItems(BADGE, 1)
       if st.getPlayer().getLevel() >= 35 and st.getPlayer().getLevel() <= 40:
          st.addExpAndSp(30000,2000)
    elif event == "31418-02.htm" :
       st.set("cond","3")
       st.playSound("ItemSound.quest_middle")
    elif event == "31418-07.htm" :
       st.set("cond","5")
       st.playSound("ItemSound.quest_middle")
       st.giveItems(REPORT2, 1)
       st.set("talk","0")
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
    elif npcId == GLYVKA :
       if cond == 0 :
          if player.getLevel() >= 35 :
             htmltext = "30067-01.htm"
          else:
             htmltext = "30067-00.htm"
             st.exitQuest(1)
       elif cond == 1 :
          htmltext = "30067-02.htm"
       elif cond in [2,3,4] :
          htmltext = "30067-05.htm"
       elif cond == 5 :
          if st.getQuestItemsCount(REPORT2) >= 1:
             htmltext = "30067-06.htm"
             st.takeItems(REPORT2, -1)
             st.set("talk","1")
          elif st.getInt("talk"):
             htmltext = "30067-07.htm"
    elif npcId == ROUKE :
       if cond == 2 :
          htmltext = "31418-01.htm"
       elif cond == 3 :
          count = st.getQuestItemsCount(FRAGMENT)
          if count >= 10 :
             htmltext = "31418-04.htm"
             calc = int(count/10)
             st.takeItems(FRAGMENT, calc*10)
             st.giveItems(TOOL, calc)
          else:
             htmltext = "31418-03.htm"
       elif cond == 4 :
          if st.getQuestItemsCount(REPORT) >= 3:
             htmltext = "31418-05.htm"
             st.takeItems(FRAGMENT, -1)
             st.takeItems(TOOL, -1)
             st.takeItems(REPORT, -1)
             st.set("talk","1")
          elif st.getInt("talk"):
             htmltext = "31418-06.htm"
       elif cond == 5 :
          htmltext = "31418-08.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED : return
    npcId = npc.getNpcId()
    if st.getInt("cond")==3 :
       if npcId == 27339 :
          st.giveItems(REPORT,1)
          if st.getQuestItemsCount(REPORT) >= 3:
             st.set("cond","4")
             st.playSound("ItemSound.quest_middle")
          else :
             st.playSound("ItemSound.quest_itemget")
       else:
          if st.getQuestItemsCount(TOOL) >= 1:
             st.takeItems(TOOL, 1)
             if st.getRandom(100) <= 45 :
                st.addSpawn(27339,npc,True,900000)
          elif st.getRandom(100) <= 70 :
             st.playSound("ItemSound.quest_itemget")
             st.giveItems(FRAGMENT,1)
    return

QUEST       = Quest(134,qn,"Temple Missionary")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(GLYVKA)
QUEST.addTalkId(ROUKE)
QUEST.addTalkId(GLYVKA)
for mob in NPC :
   QUEST.addKillId(mob)

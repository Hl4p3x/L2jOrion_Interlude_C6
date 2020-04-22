# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "135_TempleExecutor"

# NPCs
SHEGFIELD = 30068
ALEX = 30291
SONIN = 31773
PANO = 30078

# ITEMs
CARGO = 10328
CRYSTAL = 10329
MAP = 10330
SONIN_CR = 10331
PANO_CR = 10332
ALEX_CR = 10333
BADGE = 10334

# MONSTERs
NPC = [20781,21104,21105,21106,21107]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [CARGO,CRYSTAL,MAP,SONIN_CR,ALEX_CR,PANO_CR]

 def onEvent (self,event,st) :
    htmltext = event
    id = st.getState()
    cond = st.getInt("cond")
    if event == "30068-02.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
    elif event == "30068-09.htm" :
       st.playSound("ItemSound.quest_finish")
       st.unset("talk")
       st.exitQuest(False)
       st.giveItems(57, 16924)
       st.giveItems(BADGE, 1)
       if st.getPlayer().getLevel() >= 35 and st.getPlayer().getLevel() <= 40:
          st.addExpAndSp(30000,2000)
    elif event == "30068-03.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
    elif event == "30291-06.htm" :
       st.set("cond","3")
       st.playSound("ItemSound.quest_middle")
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
    elif npcId == SHEGFIELD :
       if cond == 0 :
          if player.getLevel() >= 35 :
             htmltext = "30068-01.htm"
          else:
             htmltext = "30068-00.htm"
             st.exitQuest(1)
       elif cond == 1 :
          htmltext = "30068-02.htm"
       elif cond in [2,3,4] :
          htmltext = "30068-04.htm"
       elif cond == 5 :
          if st.getQuestItemsCount(SONIN_CR) and st.getQuestItemsCount(PANO_CR) and st.getQuestItemsCount(ALEX_CR):
             htmltext = "30068-05.htm"
             st.takeItems(SONIN_CR, -1)
             st.takeItems(PANO_CR, -1)
             st.takeItems(ALEX_CR, -1)
             st.set("talk","1")
          elif st.getInt("talk"):
             htmltext = "30068-06.htm"
    elif npcId == ALEX :
       if cond == 2 :
          htmltext = "30291-01.htm"
       elif cond == 3 :
          htmltext = "30291-07.htm"
       elif cond == 4 :
          if st.getQuestItemsCount(SONIN_CR) and st.getQuestItemsCount(PANO_CR):
             st.takeItems(MAP, -1)
             st.giveItems(ALEX_CR,1)
             st.playSound("ItemSound.quest_middle")
             st.set("cond","5")
             htmltext = "30291-09.htm"
          else:
             htmltext = "30291-08.htm"
       elif cond == 5 :
          htmltext = "30291-10.htm"
    elif npcId == SONIN :
       if cond == 4 :
          if st.getQuestItemsCount(CARGO) >= 10:
             htmltext = "31773-01.htm"
             st.playSound("ItemSound.quest_middle")
             st.takeItems(CARGO, -1)
             st.giveItems(SONIN_CR,1)
          else:
             htmltext = "31773-02.htm"
    elif npcId == PANO :
       if cond == 4 :
          if st.getQuestItemsCount(CRYSTAL) >= 10:
             htmltext = "30078-01.htm"
             st.playSound("ItemSound.quest_middle")
             st.takeItems(CRYSTAL, -1)
             st.giveItems(PANO_CR,1)
          else:
             htmltext = "30078-02.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED : return
    if st.getInt("cond")==3 :
       if st.getQuestItemsCount(CARGO) < 10:
          st.giveItems(CARGO,1)
          st.playSound("ItemSound.quest_itemget")
       elif st.getQuestItemsCount(CRYSTAL) < 10:
          st.giveItems(CRYSTAL,1)
          st.playSound("ItemSound.quest_itemget")
       elif st.getQuestItemsCount(MAP) < 10:
          st.giveItems(MAP,1)
          if st.getQuestItemsCount(MAP) >= 10 and st.getQuestItemsCount(CARGO) >= 10 and st.getQuestItemsCount(CRYSTAL) >= 10:
             st.set("cond","4")
             st.playSound("ItemSound.quest_middle")
          else :
             st.playSound("ItemSound.quest_itemget")
    return

QUEST       = Quest(135,qn,"Temple Executor")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(SHEGFIELD)
QUEST.addTalkId(SHEGFIELD)
QUEST.addTalkId(ALEX)
QUEST.addTalkId(SONIN)
QUEST.addTalkId(PANO)
for mob in NPC :
   QUEST.addKillId(mob)

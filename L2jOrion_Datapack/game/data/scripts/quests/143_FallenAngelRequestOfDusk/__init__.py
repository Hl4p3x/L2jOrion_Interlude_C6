# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "143_FallenAngelRequestOfDusk"

# NPCs
NATOOLS = 30894
TOBIAS = 30297
CASIAN = 30612
ROCK = 32368
ANGEL = 32369

# ITEMs
SEALED_PATH = 10354
PATH = 10355
EMPTY_CRYSTAL = 10356
MEDICINE = 10357
MESSAGE = 10358

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.isAngelSpawned = 0
    self.questItemIds = [SEALED_PATH,PATH,EMPTY_CRYSTAL,MEDICINE,MESSAGE]

 def onAdvEvent (self,event,npc,player) :
    st = player.getQuestState(qn)
    if not st: return
    htmltext = event
    if event == "30894-01.htm" :
       st.set("cond","1")
       st.playSound("ItemSound.quest_accept")
    elif event == "30894-03.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
       st.giveItems(SEALED_PATH, 1)
    elif event == "30297-04.htm" :
       st.set("cond","3")
       st.unset("talk")
       st.playSound("ItemSound.quest_middle")
       st.giveItems(PATH, 1)
       st.giveItems(EMPTY_CRYSTAL, 1)
    elif event == "30612-07.htm" :
       st.set("cond","4")
       st.unset("talk")
       st.giveItems(MEDICINE, 1)
       st.playSound("ItemSound.quest_middle")
    elif event == "32368-02.htm" :
       if self.isAngelSpawned == 0 :
          self.addSpawn(ANGEL,-21882,186730,-4320,0,False,900000)
          self.isAngelSpawned = 1
          self.startQuestTimer("angel_cleanup",900000,None,player)
    elif event == "32369-10.htm" :
       st.set("cond","5")
       st.unset("talk")
       st.takeItems(EMPTY_CRYSTAL, -1)
       st.giveItems(MESSAGE, 1)
       st.playSound("ItemSound.quest_middle")
    elif event == "angel_cleanup" :
       if self.isAngelSpawned == 1 :
          self.isAngelSpawned = 0
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
    elif npcId == NATOOLS :
       if cond == 1 :
          htmltext = "30894-01.htm"
       elif cond == 2 :
          htmltext = "30894-04.htm"
    elif npcId == TOBIAS :
       if cond == 2 :
          if st.getInt("talk"):
             htmltext = "30297-02.htm"
          else:
             htmltext = "30297-01.htm"
             st.takeItems(SEALED_PATH, -1)
             st.set("talk","1")
       elif cond == 3 :
          htmltext = "30297-05.htm"
       elif cond == 5 :
          htmltext = "30297-06.htm"
          st.playSound("ItemSound.quest_finish")
          st.exitQuest(False)
          st.giveItems(57, 89046)
          st.takeItems(MESSAGE, -1)
          if st.getPlayer().getLevel() >= 38 and st.getPlayer().getLevel() <= 43:
             st.addExpAndSp(223036,13901)
    elif npcId == CASIAN :
       if cond == 3 :
          if st.getInt("talk"):
             htmltext = "30612-02.htm"
          else:
             htmltext = "30612-01.htm"
             st.takeItems(PATH, -1)
             st.set("talk","1")
       elif cond == 4 :
          htmltext = "30612-07.htm"
    elif npcId == ROCK :
       if cond == 4 :
          htmltext = "32368-01.htm"
    elif npcId == ANGEL :
       if cond == 4 :
          if st.getInt("talk"):
             htmltext = "32369-02.htm"
          else:
             htmltext = "32369-01.htm"
             st.takeItems(MEDICINE, -1)
             st.set("talk","1")
       elif cond == 5 :
          htmltext = "32369-10.htm"
    return htmltext

QUEST       = Quest(143,qn,"Fallen Angel - Request of Dusk")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

#this quest doesnt have starter npc, quest will appear in list only when u select him from quest 998
QUEST.addTalkId(NATOOLS)
QUEST.addTalkId(TOBIAS)
QUEST.addTalkId(CASIAN)
QUEST.addTalkId(ROCK)
QUEST.addTalkId(ANGEL)

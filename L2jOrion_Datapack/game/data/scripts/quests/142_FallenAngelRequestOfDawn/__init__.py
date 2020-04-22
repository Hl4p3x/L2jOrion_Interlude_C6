# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "142_FallenAngelRequestOfDawn"

# NPCs
NATOOLS = 30894
RAYMOND = 30289
CASIAN = 30612
ROCK = 32368

# ITEMs
CRYPT = 10351
FRAGMENT = 10352
BLOOD = 10353

# MONSTERs
NPC=[20079,20080,20081,20082,20084,20086,20087,20088,20089,20090,27338]

class Quest (JQuest) :

 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.isAngelSpawned = 0
    self.questItemIds = [CRYPT,FRAGMENT,BLOOD]

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
       st.giveItems(CRYPT, 1)
    elif event == "30289-04.htm" :
       st.set("cond","3")
       st.playSound("ItemSound.quest_middle")
    elif event == "30612-07.htm" :
       st.set("cond","4")
       st.playSound("ItemSound.quest_middle")
    elif event == "32368-02.htm" :
       if self.isAngelSpawned == 0 :
          self.addSpawn(27338,-21882,186730,-4320,0,False,900000)
          self.isAngelSpawned = 1
          self.startQuestTimer("angel_cleanup",900000,None,player)
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
    elif npcId == RAYMOND :
       if cond == 2 :
          if st.getInt("talk"):
             htmltext = "30289-02.htm"
          else:
             htmltext = "30289-01.htm"
             st.takeItems(CRYPT, -1)
             st.set("talk","1")
       elif cond == 3 :
          htmltext = "30289-05.htm"
       elif cond == 6 :
          htmltext = "30289-06.htm"
          st.playSound("ItemSound.quest_finish")
          st.exitQuest(False)
          st.giveItems(57, 92676)
          st.takeItems(BLOOD, -1)
          if st.getPlayer().getLevel() >= 38 and st.getPlayer().getLevel() <= 43:
             st.addExpAndSp(223036,13091)
    elif npcId == CASIAN :
       if cond == 3 :
          htmltext = "30612-01.htm"
       elif cond == 4 :
          htmltext = "30612-07.htm"
    elif npcId == ROCK :
       if cond == 5 :
          htmltext = "32368-01.htm"
          if not st.getInt("talk"):
             st.takeItems(REPORT, -1)
             st.set("talk","1")
       elif cond == 6 :
          htmltext = "32368-03.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED : return
    npcId = npc.getNpcId()
    if npcId == 27338 and st.getInt("cond")==5:
       st.set("cond","6")
       st.playSound("ItemSound.quest_middle")
       st.giveItems(BLOOD, 1)
       self.isAngelSpawned = 0
    elif st.getInt("cond")==4 and st.getRandom(100) <= 20 and st.getQuestItemsCount(FRAGMENT)<30:
       st.giveItems(FRAGMENT,1)
       if st.getQuestItemsCount(FRAGMENT)>=30:
          st.set("cond","5")
          st.playSound("ItemSound.quest_middle")
       else:
          st.playSound("ItemSound.quest_itemget")
    return

QUEST       = Quest(142,qn,"Fallen Angel - Request of Dawn")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

#this quest doesnt have starter npc, quest will appear in list only when u select him from quest 998
QUEST.addTalkId(NATOOLS)
QUEST.addTalkId(RAYMOND)
QUEST.addTalkId(CASIAN)
QUEST.addTalkId(ROCK)
for mob in NPC :
   QUEST.addKillId(mob)

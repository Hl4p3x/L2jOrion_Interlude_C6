# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

#Complete - 95%. Need add other reward's from retail
qn = "280_TheFoodChain"

#NPC'S
VIZON = 32175

#ITEM'S
KELTIR_TOOTH = 9809
WOLF_TOOTH = 9810
REWARD = [28,35,116] #Pants, Cloth Shoes, Magic Ring

#MOB'S
MOBS_KELTIR = range(22229,22232)
MOBS_WOLF = range(22232,22234)

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
  
 def onEvent (self,event,st) :
     htmltext = event
     keltir_tooth = st.getQuestItemsCount(KELTIR_TOOTH)
     wolf_tooth = st.getQuestItemsCount(WOLF_TOOTH)
     summ = keltir_tooth + wolf_tooth
     if event == "32175-03.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
     elif event == "ADENA" :
       htmltext = "32175-06.htm"
       st.giveItems(57,summ*2)
       st.takeItems(KELTIR_TOOTH,-1)
       st.takeItems(WOLF_TOOTH,-1)
     elif event == "ITEM" :
       if summ < 25 :
         htmltext = "32175-09.htm"
       else:
         htmltext = "32175-06.htm"
         if keltir_tooth > 25 :
           st.giveItems(REWARD[st.getRandom(len(REWARD))],1)
           st.takeItems(KELTIR_TOOTH,25)
         else :
           st.giveItems(REWARD[st.getRandom(len(REWARD))],1)
           st.takeItems(KELTIR_TOOTH,keltir_tooth)
           st.takeItems(WOLF_TOOTH,25 - keltir_tooth)
     elif event == "32175-08.htm" :
       st.takeItems(KELTIR_TOOTH,-1)
       st.takeItems(WOLF_TOOTH,-1)
       st.exitQuest(1)
     return htmltext

 def onTalk (self,npc,player):
     npcId = npc.getNpcId()
     htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
     st = player.getQuestState(qn)
     if not st : return htmltext
     id = st.getState()
     cond = st.getInt("cond")
     keltir_tooth = st.getQuestItemsCount(KELTIR_TOOTH)
     wolf_tooth = st.getQuestItemsCount(WOLF_TOOTH)
     summ = keltir_tooth + wolf_tooth
     if id == CREATED and npcId == VIZON :
       if player.getLevel() < 3 :
         htmltext = "32175-02.htm"
         st.exitQuest(1)
       else :
         htmltext = "32175-01.htm"
     elif id == STARTED and npcId == VIZON :
       if not summ :
         htmltext = "32175-04.htm"
       else :
         htmltext = "32175-05.htm"
     return htmltext
    
 def onKill(self,npc,player,isPet) :
     st = player.getQuestState(qn)
     if not st: return
     if st.getState() == STARTED :
       npcId = npc.getNpcId()
       chance = st.getRandom(100)
       if (npcId in MOBS_KELTIR) and (chance < 95) : #Retail statistic info. 36 mob's - 34 tooth
         st.giveItems(KELTIR_TOOTH,1)
         st.playSound("ItemSound.quest_itemget")
       elif (npcId in MOBS_WOLF) and (chance < 75) : #Retail statistic info. 30 mob's - 22*3 tooth
         st.giveItems(WOLF_TOOTH,3)
         st.playSound("ItemSound.quest_itemget")
     return

QUEST       = Quest(280, qn, "The Food Chain")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(VIZON)

QUEST.addTalkId(VIZON)

for mob in MOBS_KELTIR :
    QUEST.addKillId(mob)
for mob in MOBS_WOLF :
    QUEST.addKillId(mob)

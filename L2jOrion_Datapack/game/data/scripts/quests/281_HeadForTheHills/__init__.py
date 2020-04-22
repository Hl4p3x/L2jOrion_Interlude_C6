# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "281_HeadForTheHills"

#NPC'S
MARCELA = 32173

#ITEM'S
HILLS = 9796
SOULSHOT_FOR_BEGINNERS  = 5789
REWARDS = [736,876,115]
NEWBIE_REWARD = 32

#MOB'S
MOBS = range(22234,22240)

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [HILLS]
  
 def onEvent (self,event,st) :
     htmltext = event
     player = st.getPlayer()
     hills = st.getQuestItemsCount(HILLS)
     onlyone = st.getInt("onlyone")
     if event == "32173-03.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
     elif event == "32173-06.htm" :
        newbie = player.getNewbie()
        if newbie | NEWBIE_REWARD != newbie :
           player.setNewbie(newbie|NEWBIE_REWARD)
           st.giveItems(SOULSHOT_FOR_BEGINNERS,6000)
           st.playTutorialVoice("tutorial_voice_026")
        if hills > 20 : 
           st.giveItems(57,hills*23+400)
        else :
           st.giveItems(57,hills*23)
        st.takeItems(HILLS,-1)
     elif event == "32173-07.htm" :
       if hills < 50 :
         htmltext = "32173-07a.htm"
       else:
         rnd = st.getRandom(len(REWARDS))
         REWARD = REWARDS[rnd]
         newbie = player.getNewbie()
         if newbie | NEWBIE_REWARD != newbie :
           st.giveItems(SOULSHOT_FOR_BEGINNERS,6000)
           st.playTutorialVoice("tutorial_voice_026")
           st.giveItems(REWARD,1)
           st.takeItems(HILLS,50)
           st.set("onlyone","1")
         else :
           st.giveItems(REWARD,1)
           st.takeItems(HILLS,50)
     elif event == "32173-09.htm" :
       st.takeItems(HILLS,-1)
       st.exitQuest(1)
     return htmltext

 def onTalk (self,npc,player):
     npcId = npc.getNpcId()
     htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
     st = player.getQuestState(qn)
     if not st : return htmltext
     id = st.getState()
     cond = st.getInt("cond")
     hills = st.getQuestItemsCount(HILLS)
     if id == CREATED and npcId == MARCELA :
       if player.getLevel() < 6 :
         htmltext = "32173-02.htm"
         st.exitQuest(1)
       else :
         htmltext = "32173-01.htm"
     elif id == STARTED and npcId == MARCELA :
       if not hills :
         htmltext = "32173-04.htm"
       else :
         htmltext = "32173-05.htm"
     return htmltext
    
 def onKill(self,npc,player,isPet) :
     st = player.getQuestState(qn)
     if not st: return
     if st.getState() == STARTED :
       npcId = npc.getNpcId()
       chance = st.getRandom(100)
       if (npcId in MOBS) and (chance < 50) : #Retail statistic info. 53 mob's - 28 hills
         st.giveItems(HILLS,1)
         st.playSound("ItemSound.quest_itemget")
     return

QUEST       = Quest(281, qn, "Head for the Hills!")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(MARCELA)

QUEST.addTalkId(MARCELA)

for mob in MOBS :
    QUEST.addKillId(mob)

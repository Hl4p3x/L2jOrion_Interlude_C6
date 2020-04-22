# Made by Polo - Have fun! - Fixed by BiTi
# v0.3.1 by DrLecter
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "636_TruthBeyond"

#Npc
ELIYAH = 31329
FLAURON = 32010

#Items
MARK = 8064

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if htmltext == "31329-04.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
    elif htmltext == "32010-02.htm" :
       st.playSound("ItemSound.quest_finish")
       st.giveItems(MARK,1)
       st.unset("cond")
       st.setState(CREATED)
    return htmltext

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   if st :
     npcId = npc.getNpcId()
     id = st.getState()
     cond = st.getInt("cond")
     if cond == 0 and id == CREATED:
       if npcId == ELIYAH :
         if player.getLevel()>72 :
           htmltext = "31329-02.htm"
       else:
         htmltext = "31329-01.htm"
         st.exitQuest(1)
     elif id == STARTED :
       if npcId == ELIYAH :
         htmltext = "31329-05.htm"
       elif npcId == FLAURON :
         if cond == 1 :
           htmltext = "32010-01.htm"
           st.set("cond","2")
         else :
           htmltext = "32010-03.htm"
   return htmltext


QUEST       = Quest(636,qn,"The Truth Beyond the Gate")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
#COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(ELIYAH)

QUEST.addTalkId(ELIYAH)
QUEST.addTalkId(FLAURON)
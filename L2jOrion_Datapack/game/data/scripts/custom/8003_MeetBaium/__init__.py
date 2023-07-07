import sys
from l2jorion.game.managers           import GrandBossManager
from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "8003_MeetBaium"

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onFirstTalk (self,npc,player):
   st = player.getQuestState(qn)
   if not st : st = self.newQuestState(player)

   baiumStatus = GrandBossManager.getInstance().getBossStatus(29020)
   if baiumStatus != 2 and st.getQuestItemsCount(4295) :
     st.exitQuest(1)
     return "31862.htm"
   else :
     npc.showChatWindow(player)
     st.exitQuest(1)
     return
   return

QUEST = Quest(-1,qn,"custom")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(31862)

QUEST.addFirstTalkId(31862)

QUEST.addTalkId(31862)
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "4_LongLiveLordOfFlame"

HONEY_KHANDAR,BEAR_FUR_CLOAK,BLOODY_AXE,ANCESTOR_SKULL,SPIDER_DUST,DEEP_SEA_ORB = range(1541,1547)
NPC_GIFTS = {30585:BEAR_FUR_CLOAK,30566:HONEY_KHANDAR,30562:BLOODY_AXE,30560:ANCESTOR_SKULL,30559:SPIDER_DUST,30587:DEEP_SEA_ORB}

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "30578-03.htm" :
      st.set("cond","1")
      st.set("id","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    return htmltext

 def onTalk (self,npc,player):
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   st = player.getQuestState(qn)
   if not st : return htmltext

   npcId = npc.getNpcId()
   id = st.getState()
   cond = st.getInt("cond")
   if id == COMPLETED :
     htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == 30578 :
     if cond == 0 :
       if player.getRace().ordinal() != 3 :
         htmltext = "30578-00.htm"
         st.exitQuest(1)
       elif player.getLevel() >= 2 :
         htmltext = "30578-02.htm"
       else:
         htmltext = "30578-01.htm"
         st.exitQuest(1)
     elif cond == 1 :
       htmltext = "30578-04.htm"
     elif cond == 2 :
       htmltext = "30578-06.htm"
       st.giveItems(4,1)
       for item in NPC_GIFTS.values():
           st.takeItems(item,-1)
       st.unset("cond")
       st.setState(COMPLETED)
       st.playSound("ItemSound.quest_finish")
   elif npcId in NPC_GIFTS.keys() and cond == 1 and id == STARTED:
     item=NPC_GIFTS[npcId]
     npc=str(npcId)
     if st.getQuestItemsCount(item) :
       htmltext = npc+"-02.htm"
     else :
       st.giveItems(item,1)
       htmltext = npc+"-01.htm"
       count = 0
       for item in NPC_GIFTS.values():
         count += st.getQuestItemsCount(item)
       if count == 6 :
         st.set("cond","2")
         st.set("id","2")
         st.playSound("ItemSound.quest_middle")
       else :
         st.playSound("ItemSound.quest_itemget")
   return htmltext

QUEST     = Quest(4,qn,"Long Live the Paagrio Lord")
CREATED   = State('Start',     QUEST)
STARTING  = State('Starting',  QUEST)
STARTED   = State('Started',   QUEST)
COMPLETED = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(30578)

QUEST.addTalkId(30578)

QUEST.addTalkId(30559)
QUEST.addTalkId(30560)
QUEST.addTalkId(30562)
QUEST.addTalkId(30566)
QUEST.addTalkId(30578)
QUEST.addTalkId(30585)
QUEST.addTalkId(30587)

for i in range(1541,1547) :
   STARTED.addQuestDrop(30578,i,1)
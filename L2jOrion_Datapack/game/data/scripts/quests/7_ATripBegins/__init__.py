import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "7_ATripBegins"

#NPCs 
MIRABEL  = 30146 
ARIEL    = 30148 
ASTERIOS = 30154 

#ITEM 
ARIELS_RECOMMENDATION = 7572 
 
#REWARDS 
ADENA                  = 57 
SCROLL_OF_ESCAPE_GIRAN = 7559 
MARK_OF_TRAVELER       = 7570 

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr) 

 def onEvent (self,event,st) : 
   htmltext = event 
   if event == "30146-03.htm" : 
     st.set("cond","1") 
     st.setState(STARTED) 
     st.playSound("ItemSound.quest_accept") 
   elif event == "30148-02.htm" : 
     st.giveItems(ARIELS_RECOMMENDATION,1) 
     st.set("cond","2") 
     st.set("id","2") 
     st.playSound("ItemSound.quest_middle") 
   elif event == "30154-02.htm" : 
     st.takeItems(ARIELS_RECOMMENDATION,-1) 
     st.set("cond","3") 
     st.set("id","3") 
     st.playSound("ItemSound.quest_middle") 
   elif event == "30146-06.htm" : 
     st.giveItems(SCROLL_OF_ESCAPE_GIRAN,1) 
     st.giveItems(MARK_OF_TRAVELER, 1) 
     st.set("cond","0") 
     st.setState(COMPLETED) 
     st.playSound("ItemSound.quest_finish") 
   return htmltext 

 def onTalk (self,npc,player): 
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>" 
   st = player.getQuestState(qn)
   if not st : return htmltext

   npcId = npc.getNpcId() 
   cond  = st.getInt("cond") 
   id    = st.getState() 
   if id == CREATED : 
     st.set("cond","0") 
     if player.getRace().ordinal() == 1 : 
       if player.getLevel() >= 3 : 
         htmltext = "30146-02.htm" 
       else : 
         htmltext = "<html><body>Quest for characters level 3 above.</body></html>" 
         st.exitQuest(1) 
     else : 
       htmltext = "30146-01.htm" 
       st.exitQuest(1) 
   elif npcId == MIRABEL and id == COMPLETED : 
     htmltext = "<html><body>I can't supply you with another Giran Scroll of Escape. Sorry traveller.</body></html>" 
   elif npcId == MIRABEL and cond == 1 : 
     htmltext = "30146-04.htm"
   elif id == STARTED :  
       if npcId == ARIEL and cond : 
         if st.getQuestItemsCount(ARIELS_RECOMMENDATION) == 0 : 
           htmltext = "30148-01.htm" 
         else : 
           htmltext = "30148-03.htm" 
       elif npcId == ASTERIOS and cond == 2 and st.getQuestItemsCount(ARIELS_RECOMMENDATION) > 0 : 
         htmltext = "30154-01.htm" 
       elif npcId == MIRABEL and cond == 3 : 
         htmltext = "30146-05.htm" 
   return htmltext 

QUEST     = Quest(7,qn,"A Trip Begins") 
CREATED   = State('Start',     QUEST) 
STARTED   = State('Started',   QUEST) 
COMPLETED = State('Completed', QUEST) 
 
QUEST.setInitialState(CREATED)
QUEST.addStartNpc(MIRABEL) 

QUEST.addTalkId(MIRABEL) 

QUEST.addTalkId(ARIEL) 
QUEST.addTalkId(ASTERIOS) 

STARTED.addQuestDrop(MIRABEL,ARIELS_RECOMMENDATION,1)
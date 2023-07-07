import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "101_SwordOfSolidarity"

ROIENS_LETTER_ID = 796
HOWTOGO_RUINS_ID = 937
BROKEN_SWORD_HANDLE_ID = 739
BROKEN_BLADE_BOTTOM_ID = 740
BROKEN_BLADE_TOP_ID = 741
ALLTRANS_NOTE_ID = 742
SWORD_OF_SOLIDARITY_ID = 738

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "30008-04.htm" :
        st.set("cond","1")
        st.setState(STARTED)
        st.playSound("ItemSound.quest_accept")
        st.giveItems(ROIENS_LETTER_ID,1)
    elif event == "30283-02.htm" :
        st.set("cond","2")
        st.takeItems(ROIENS_LETTER_ID,st.getQuestItemsCount(ROIENS_LETTER_ID))
        st.giveItems(HOWTOGO_RUINS_ID,1)
    elif event == "30283-07.htm" :
        st.takeItems(BROKEN_SWORD_HANDLE_ID,-1)
        st.giveItems(SWORD_OF_SOLIDARITY_ID,1)
        st.set("cond","0")
        st.setState(COMPLETED)
        st.playSound("ItemSound.quest_finish")
        st.set("onlyone","1")        
    return htmltext


 def onTalk (self,npc,player) :
   npcId = npc.getNpcId()
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>" 
   st = player.getQuestState(qn)
   if not st: return htmltext
   id = st.getState()
   if id == CREATED :
     st.set("cond","0")
     st.set("onlyone","0")
   if npcId == 30008 and st.getInt("cond")==0 and st.getInt("onlyone")==0 :
      if player.getRace().ordinal() != 0 :
        htmltext = "30008-00.htm"
      elif player.getLevel() >= 9 :
        htmltext = "30008-02.htm"
        return htmltext
      else:
        htmltext = "30008-08.htm"
        st.exitQuest(1)
   elif npcId == 30008 and st.getInt("cond")==0 and st.getInt("onlyone")==1 :
        htmltext = "<html><body>This quest has already been completed.</body></html>"
   if id == STARTED: 
       if npcId == 30008 and st.getInt("cond")==1 and (st.getQuestItemsCount(ROIENS_LETTER_ID)==1) :
            htmltext = "30008-05.htm"
       elif npcId == 30008 and st.getInt("cond")>=2 and st.getQuestItemsCount(ROIENS_LETTER_ID)==0 and st.getQuestItemsCount(ALLTRANS_NOTE_ID)==0 :
            if st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) and st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) :
              htmltext = "30008-12.htm"
            if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) + st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID)) <= 1 :
              htmltext = "30008-11.htm"
            if st.getQuestItemsCount(BROKEN_SWORD_HANDLE_ID) > 0 :
              htmltext = "30008-07.htm"
            if st.getQuestItemsCount(HOWTOGO_RUINS_ID) == 1 :
              htmltext = "30008-10.htm"
       elif npcId == 30008 and st.getInt("cond")==4 and st.getQuestItemsCount(ROIENS_LETTER_ID)==0 and st.getQuestItemsCount(ALLTRANS_NOTE_ID) :
            htmltext = "30008-06.htm"
            st.set("cond","5")
            st.takeItems(ALLTRANS_NOTE_ID,st.getQuestItemsCount(ALLTRANS_NOTE_ID))
            st.giveItems(BROKEN_SWORD_HANDLE_ID,1)
       elif npcId == 30283 and st.getInt("cond")==1 and st.getQuestItemsCount(ROIENS_LETTER_ID)>0 :
            htmltext = "30283-01.htm"
       elif npcId == 30283 and st.getInt("cond")>=2 and st.getQuestItemsCount(ROIENS_LETTER_ID)==0 and st.getQuestItemsCount(HOWTOGO_RUINS_ID)>0 :
            if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) + st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID)) == 1 :
              htmltext = "30283-08.htm"
            if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) + st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID)) == 0 :
              htmltext = "30283-03.htm"
            if st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) and st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) :
              htmltext = "30283-04.htm"
              st.set("cond","4")
              st.takeItems(HOWTOGO_RUINS_ID,st.getQuestItemsCount(HOWTOGO_RUINS_ID))
              st.takeItems(BROKEN_BLADE_TOP_ID,st.getQuestItemsCount(BROKEN_BLADE_TOP_ID))
              st.takeItems(BROKEN_BLADE_BOTTOM_ID,st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID))
              st.giveItems(ALLTRANS_NOTE_ID,1)
       elif npcId == 30283 and st.getInt("cond")==4 and st.getQuestItemsCount(ALLTRANS_NOTE_ID) :
            htmltext = "30283-05.htm"
       elif npcId == 30283 and st.getInt("cond")==5 and st.getQuestItemsCount(BROKEN_SWORD_HANDLE_ID) :
            htmltext = "30283-06.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st: return   
   if st.getState() == STARTED :
       npcId = npc.getNpcId()
       if npcId in [20361,20362] :
          if st.getQuestItemsCount(HOWTOGO_RUINS_ID) :
             if st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) == 0 :
                if st.getRandom(5) == 0 :
                   st.giveItems(BROKEN_BLADE_TOP_ID,1)
                   st.playSound("ItemSound.quest_middle")
             elif st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) == 0 :
                if st.getRandom(5) == 0 :
                   st.giveItems(BROKEN_BLADE_BOTTOM_ID,1)
                   st.playSound("ItemSound.quest_middle")
          if st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) and st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) :
             st.set("cond","3")
   return

QUEST       = Quest(101,qn,"Sword Of Solidarity Quest")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(30008)

QUEST.addTalkId(30008)

QUEST.addTalkId(30283)

QUEST.addKillId(20361)
QUEST.addKillId(20362)

STARTED.addQuestDrop(30283,ALLTRANS_NOTE_ID,1)
STARTED.addQuestDrop(30283,HOWTOGO_RUINS_ID,1)
STARTED.addQuestDrop(20362,BROKEN_BLADE_TOP_ID,1)
STARTED.addQuestDrop(20361,BROKEN_BLADE_TOP_ID,1)
STARTED.addQuestDrop(20362,BROKEN_BLADE_BOTTOM_ID,1)
STARTED.addQuestDrop(20361,BROKEN_BLADE_BOTTOM_ID,1)
STARTED.addQuestDrop(30008,ROIENS_LETTER_ID,1)
STARTED.addQuestDrop(30008,BROKEN_SWORD_HANDLE_ID,1)
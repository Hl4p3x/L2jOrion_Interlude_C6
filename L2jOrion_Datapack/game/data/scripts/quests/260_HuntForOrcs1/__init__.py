import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "260_HuntForOrcs1"

ORC_AMULET = 1114
ORC_NECKLACE = 1115
ADENA = 57
NEWBIE_REWARD = 4
SPIRITSHOT_FOR_BEGINNERS = 5790
SOULSHOT_FOR_BEGINNERS = 5789

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [ORC_AMULET, ORC_NECKLACE]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "30221-03.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    elif event == "30221-06.htm" :
      st.exitQuest(1)
      st.playSound("ItemSound.quest_finish")
    return htmltext

 def onTalk (self,npc,player):
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   st = player.getQuestState(qn)
   if not st : return htmltext

   npcId = npc.getNpcId()
   id = st.getState()

   if id == CREATED :
     st.set("cond","0")
   if st.getInt("cond")==0 :
     if player.getRace().ordinal() != 1 :
       htmltext = "30221-00.htm"
       st.exitQuest(1)
     elif player.getLevel()<6 :
       htmltext = "30221-01.htm"
       st.exitQuest(1)
     else :
       htmltext = "30221-02.htm"
   else :
     amulet = st.getQuestItemsCount(ORC_AMULET)
     necklace = st.getQuestItemsCount(ORC_NECKLACE)
     if amulet == necklace == 0 :
       htmltext = "30221-04.htm"
     else :
       htmltext = "30221-05.htm"
       st.giveItems(ADENA,(amulet*12)+(necklace*30))
       st.takeItems(ORC_AMULET,-1)
       st.takeItems(ORC_NECKLACE,-1)
       # check the player state against this quest newbie rewarding mark.
       newbie = player.isNewbie()
       if newbie | NEWBIE_REWARD != newbie :
          st.checkNewbieQuests()
          player.setNewbie(newbie|NEWBIE_REWARD)
          st.showQuestionMark(26)
          if player.getClassId().isMage() :
             st.playTutorialVoice("tutorial_voice_027")
             st.giveItems(SPIRITSHOT_FOR_BEGINNERS,3000)
          else :
             st.playTutorialVoice("tutorial_voice_026")
             st.giveItems(SOULSHOT_FOR_BEGINNERS,6000)
   return htmltext

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st : return 
   if st.getState() != STARTED : return 
   
   item=ORC_AMULET
   if npc.getNpcId() in range(20471,20474) :
     item = ORC_NECKLACE
   if st.getRandom(10)>4 :
     st.giveItems(item,1)
     st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(260,qn,"Hunt the Orcs")
CREATED     = State('Start', QUEST)
STARTING    = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(30221)

QUEST.addTalkId(30221)

QUEST.addKillId(20468)
QUEST.addKillId(20469)
QUEST.addKillId(20470)
QUEST.addKillId(20471)
QUEST.addKillId(20472)
QUEST.addKillId(20473)
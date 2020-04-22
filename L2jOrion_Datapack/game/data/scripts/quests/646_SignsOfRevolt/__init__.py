# Made by Edge
import sys
from l2jorion import Config
from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
qn = "646_SignsOfRevolt"

#NPC
TORRANT = 32016
#Drop rate
DROP_CHANCE = 75
#Mobs
MOBS = range(22029,22046) + [22047,22049]
#Item
CURSED_DOLL = 8087
#REWARDS
REWARDS = {
    "1" : [1880 , 9 ], #Steel
    "2" : [1881 , 12 ], #CBP
    "3" : [1882 , 20], #Leather
    "4" : [57 , 21600], #Adena
    }

class Quest (JQuest) :
 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [CURSED_DOLL]

 def onEvent (self,event,st) :
   htmltext = event
   if event == "32016-03.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
   elif event in REWARDS.keys() :
       item, amount = REWARDS[event]
       st.takeItems(CURSED_DOLL,-1)
       st.giveItems(item, amount)
       st.playSound("ItemSound.quest_finish")
       st.exitQuest(1)
       htmltext = "32016-07.htm"
   return htmltext

 def onTalk (self,npc,player):
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   st = player.getQuestState(qn)
   if st :
     id = st.getState()
     cond = st.getInt("cond")
     if id == CREATED :
      if player.getLevel() < 40 :
         htmltext = "32017-02.htm"
         st.exitQuest(1)
      else :
         htmltext = "32016-01.htm"
     elif cond == 1 :
         htmltext = "32016-04.htm"
     elif cond == 2 :
         if st.getQuestItemsCount(CURSED_DOLL) >= 180 :
             htmltext = "32016-05.htm"
         else :
             htmltext = "32016-04.htm"
   return htmltext

 def onKill (self,npc,player,isPet):
   partyMember = self.getRandomPartyMemberState(player,STARTED)
   if not partyMember: return
   st = partyMember.getQuestState(qn)
   if st :
      if st.getState() == STARTED :
         count = st.getQuestItemsCount(CURSED_DOLL)
         if st.getInt("cond") == 1 and count < 180 :
            chance = DROP_CHANCE * Config.RATE_DROP_QUEST
            numItems, chance = divmod(chance,100)
            if st.getRandom(100) < chance :
               numItems += 1
            if numItems :
               if count + numItems >= 180 :
                  numItems = 180 - count
                  st.playSound("ItemSound.quest_middle")
                  st.set("cond","2")
               else:
                  st.playSound("ItemSound.quest_itemget")
               st.giveItems(CURSED_DOLL,int(numItems))
   return

QUEST       = Quest(646, qn, "Signs of Revolt")
CREATED = State('Start', QUEST)
STARTED = State('Started', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(TORRANT)
QUEST.addTalkId(TORRANT)

for i in MOBS :
  QUEST.addKillId(i)
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "5000_MissQueen"

COUPON_ONE = 7832
COUPON_TWO = 7833

NPCs = range(31760,31767)

#enable/disable coupon give
QUEEN_ENABLED = 1

#Newbie/one time rewards section
#Any quest should rely on a unique bit, but
#it could be shared among quest that were mutually
#exclusive or race restricted.
#Bit #1 isn't used for backwards compatibility.
#This script uses 2 bits, one for newbie coupons and another for travelers
NEWBIE_REWARD = 16
TRAVELER_REWARD = 32

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player) :
    if not QUEEN_ENABLED : return
    st = player.getQuestState(qn)
    newbie = player.isNewbie()
    level = player.getLevel()
    occupation_level = player.getClassId().level()
    pkkills = player.getPkKills()
    cond = st.getInt("cond")
    cond2 = st.getInt("cond2")
    if event == "newbie_give_coupon" :
       #@TODO: check if this is the very first character for this account
       #would need a bit of SQL, or a core method to determine it.
       #This condition should be stored by the core in the account_data table
       #upon character creation.
       if 6 <= level <= 25 and not pkkills and occupation_level == 0 :
          # check the player state against this quest newbie rewarding mark.
          if cond == 0 and newbie | NEWBIE_REWARD != newbie :
             st.set("cond","1")
             player.setNewbie(newbie|NEWBIE_REWARD)
             st.giveItems(COUPON_ONE,1)
             return "31760-2.htm" #here's the coupon you requested
          else :
             return "31760-1.htm" #you got a coupon already!
       else :
          return "31760-3.htm" #you're not eligible to get a coupon (level caps, pkkills or already changed class)
    elif event == "traveller_give_coupon" :
       if 6 <= level <= 25 and not pkkills and occupation_level == 1 :
          # check the player state against this quest newbie rewarding mark.
          if cond2 == 0 and newbie | TRAVELER_REWARD != newbie :
             st.set("cond2","1")
             player.setNewbie(newbie|TRAVELER_REWARD)
             st.giveItems(COUPON_TWO,1)
             return "31760-5.htm" #here's the coupon you requested
          else :
             return "31760-4.htm" #you got a coupon already!
       else :
          return "31760-6.htm" #you're not eligible to get a coupon (level caps, pkkills or already changed class)

 def onFirstTalk (self,npc,player):
   st = player.getQuestState(qn)
   if not st : st = self.newQuestState(player)
   return "31760.htm"

QUEST = Quest(-1,qn,"custom")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)

for i in NPCs :
    QUEST.addStartNpc(i)
    QUEST.addFirstTalkId(i)
    QUEST.addTalkId(i)
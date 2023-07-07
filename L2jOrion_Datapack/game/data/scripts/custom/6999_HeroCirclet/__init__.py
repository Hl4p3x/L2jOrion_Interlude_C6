import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "6999_HeroCirclet"

MONUMENTS = [31690]+range(31769,31773)

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (Self,npc,player) :
   st = player.getQuestState(qn)
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   if player.isHero() :
     if st.getQuestItemsCount(6842) :
       htmltext = "You can't have more than one circlet."
     else:
       st.giveItems(6842,1)
       htmltext = "Enjoy your Wings of Destiny Circlet."
     st.exitQuest(1)
   else :
     html = "<html><body>Monument of Heroes:<br>You're not a Hero and aren't eligible to receive the Wings of Destiny Circlet. Better luck next time.<br><a action=\"bypass -h npc_%objectId%_Chat 0\">Return</a></body></html>"
     htmltext = html.replace("%objectId%",str(npc.getObjectId()))
     st.exitQuest(1)
   return htmltext

QUEST = Quest(-1,qn,"custom")
CREATED     = State('Start', QUEST)

QUEST.setInitialState(CREATED)

for i in MONUMENTS:
    QUEST.addStartNpc(i)
    QUEST.addTalkId(i)
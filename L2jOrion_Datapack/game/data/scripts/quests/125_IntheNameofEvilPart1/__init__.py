import sys
from l2jorion import Config
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "125_IntheNameofEvilPart1"

#NPCs
MUSHIKA = 32114
KARAKAWEI = 32117
STATUE = 32119
STATUEE = 32120
STATUEEE = 32121

#MOBs
ORNITHOMIMUS = range(22200,22202)+[22219,22224]
DEINONYCHUS = range(22203,22205)+[22220,22225]

#Items
DROP_CHANCE = 100
ORNITHOMIMUS_CLAW = 8779
DEINONYCHUS_BONE = 8780
EPITAPH = 8781

class Quest (JQuest) :
 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "problema.htm" :
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
       st.set("cond","1")
    elif event == "ok.htm" :
       st.set("cond","2")
    elif event == "materialy.htm" :
       st.set("cond","3")
    elif event == "materdal.htm" :
       st.set("cond","5")
       st.takeItems(ORNITHOMIMUS_CLAW,2)
       st.takeItems(DEINONYCHUS_BONE,2)
    elif event == "bolezn.htm" :
       st.set("cond","6")
    elif event == "boleznn.htm" :
       st.set("cond","7")
    elif event == "otstatuee.htm" :
       st.giveItems(EPITAPH,1)
       st.set("cond","8")    
    elif event == "proklatie.htm" :
       st.playSound("ItemSound.quest_finish")
       st.takeItems(EPITAPH,1)
       st.unset("cond")
       st.setState(COMPLETED)   
    return htmltext

 def onTalk (self,npc,player):
    st = player.getQuestState(qn)
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>" 
    if not st: return htmltext
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    if npcId == MUSHIKA :
       if id == CREATED :
          st2 = player.getQuestState("q124_MeetingTheElroki")
          if not st2 == None :
             if st2.getState().getName() == 'Completed' :
                if player.getLevel() >= 76 :
                   htmltext = "privetstvie.htm"
                else :
                   htmltext = "lvl.htm"
                   st.exitQuest(1)
          else :
             htmltext = "no.htm"
             st.exitQuest(1)
       elif cond >= 2 and cond < 8:
          htmltext = "idikkarakawei.htm"
       elif cond == 8:
          htmltext = "vernulsa.htm"
    elif npcId == KARAKAWEI :
       if cond == 2:
          htmltext = "privkarakawei.htm"
       elif cond == 3:
          htmltext = "materialy.htm"
       elif cond == 4:
          htmltext = "materprines.htm"
    elif npcId == STATUE :
       if cond == 5:
          htmltext = "statue.htm"
    elif npcId == STATUEE :
       if cond == 6:
          htmltext = "statuee.htm"
    elif npcId == STATUEEE :
       if cond == 7:
          htmltext = "statueee.htm"
    return htmltext

 def onKill (self, npc, player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    cond = st.getInt("cond")
    npcId = npc.getNpcId()
    if cond == 3:
       chance = DROP_CHANCE*Config.RATE_DROP_QUEST
       random = st.getRandom(100)
       ornyclaw = st.getQuestItemsCount(ORNITHOMIMUS_CLAW)
       deinobone = st.getQuestItemsCount(DEINONYCHUS_BONE)
       if ornyclaw == deinobone == 2:
          st.playSound("ItemSound.quest_middle")
          st.set("cond","4")
       elif npcId in ORNITHOMIMUS :
          if ornyclaw < 2:
             if random <= chance:
                st.giveItems(ORNITHOMIMUS_CLAW,1)
                st.playSound("ItemSound.quest_itemget")
       elif npcId in DEINONYCHUS :
          if deinobone < 2:
             if random <= chance:
                st.giveItems(DEINONYCHUS_BONE,1)
                st.playSound("ItemSound.quest_itemget")
    return

QUEST = Quest(125,qn,"The Name Of Evil - 1")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(MUSHIKA)
QUEST.addTalkId(MUSHIKA)
QUEST.addTalkId(KARAKAWEI)
QUEST.addTalkId(STATUE)
QUEST.addTalkId(STATUEE)
QUEST.addTalkId(STATUEEE)

   
for npcId in ORNITHOMIMUS:
   QUEST.addKillId(npcId)
   
for npcId in DEINONYCHUS:
   QUEST.addKillId(npcId)

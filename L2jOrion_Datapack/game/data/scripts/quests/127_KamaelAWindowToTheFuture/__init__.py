import sys
from l2jorion import Config
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "127_KamaelAWindowToTheFuture"

# NPCs
DOMINIC = 31350;
KLAUS = 30187;
ALDER = 32092;
AKLAN = 31288;
OLTLIN = 30862;
JURIS = 30113;
RODEMAI = 30756;
	
# Items
MARK_DOMINIC = 8939;
MARK_HUMAN = 8940;
MARK_DWARF = 8941;
MARK_ORC = 8944;
MARK_DELF = 8943;
MARK_ELF = 8942;

class Quest (JQuest) :

  def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)
    self.questItemIds = [MARK_DOMINIC, MARK_HUMAN, MARK_DWARF, MARK_ORC, MARK_DELF, MARK_ELF]

  def onAdvEvent (self,event,npc, player) :
    htmltext = event
    st = player.getQuestState(qn)
    if not st : return
    if event == "31350-04.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.giveItems(MARK_DOMINIC, 1)
      st.playSound("ItemSound.quest_accept")
    elif event == "31350-06.htm" :
      st.takeItems(MARK_HUMAN, -1)
      st.takeItems(MARK_DWARF, -1)
      st.takeItems(MARK_ELF, -1)
      st.takeItems(MARK_DELF, -1)
      st.takeItems(MARK_ORC, -1)
      st.takeItems(MARK_DOMINIC, -1)
      st.giveItems(57, 159100)
      st.playSound("ItemSound.quest_finish")
      st.exitQuest(False)
      st.setState(COMPLETED)
    elif event == "30187-06.htm" :
      st.set("cond", "2")
    elif event == "30187-08.htm" :
      st.set("cond", "3")
      st.playSound("ItemSound.quest_middle")
      st.giveItems(MARK_HUMAN, 1)
    elif event == "32092-05.htm" :
      st.set("cond", "4")
      st.playSound("ItemSound.quest_middle")
      st.giveItems(MARK_DWARF, 1)
    elif event == "31288-04.htm" :
      st.set("cond", "5")
      st.playSound("ItemSound.quest_middle")
      st.giveItems(MARK_ORC, 1)
    elif event == "30862-04.htm" :
      st.set("cond", "6")
      st.playSound("ItemSound.quest_middle")
      st.giveItems(MARK_DELF, 1)
    elif event == "30113-04.htm" :
      st.set("cond", "7")
      st.playSound("ItemSound.quest_middle")
      st.giveItems(MARK_ELF, 1)
    elif event == "30756-04.htm" :
      st.set("cond", "8")
      st.playSound("ItemSound.quest_middle")
    elif event == "30756-05.htm" :
      st.set("cond", "9");
      st.playSound("ItemSound.quest_middle")
    return htmltext

  def onTalk (self, npc, player) :
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
    st = player.getQuestState(qn)
    if not st : return htmltext
    npcId = npc.getNpcId()
    cond = st.getInt("cond")
    id = st.getState()
    if id == CREATED :
      htmltext = "31350-01.htm"
    elif id == STARTED :
      if npcId == KLAUS :
        if cond == 1 :
          htmltext = "30187-01.htm"
        elif cond == 2 :
          htmltext = "30187-06.htm"
      elif npcId == ALDER :
        if cond == 3 :
          htmltext = "32092-01.htm"
      elif npcId == AKLAN :
        if cond == 4 :
          htmltext = "31288-01.htm"
      elif npcId == OLTLIN :
        if cond == 5 :
          htmltext = "30862-01.htm"
      elif npcId == JURIS :
        if cond == 6 :
          htmltext = "30113-01.htm"
      elif npcId == RODEMAI :
        if cond == 7 :
          htmltext = "30756-01.htm"
        elif cond == 8 :
          htmltext = "30756-04.htm"
      elif npcId == DOMINIC :
        if cond == 9 :
          htmltext = "31350-05.htm"
    elif id == COMPLETED :
       htmltext = "<html><body>This quest has already been completed.</body></html>"
    return htmltext

QUEST       = Quest(127,qn,"Kamael A Window To The Future")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(DOMINIC)
QUEST.addTalkId(DOMINIC)
QUEST.addTalkId(KLAUS)
QUEST.addTalkId(ALDER)
QUEST.addTalkId(AKLAN)
QUEST.addTalkId(OLTLIN)
QUEST.addTalkId(JURIS)
QUEST.addTalkId(RODEMAI)
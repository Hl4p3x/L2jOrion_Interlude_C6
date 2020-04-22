import sys
from l2jorion import Config
from l2jorion.game.datatables import SkillTable
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "125_TheNameofEvilPart1"

# NPCs
MUSHIKA     = 32114
KARAKAWEI   = 32117
ULU_KAIMU   = 32119
BALU_KAIMU  = 32120
CHUTA_KAIMU = 32121

# ITEMS
GAZKH_FRAGMENT    = 8782
ORNITHOMIMUS_CLAW = 8779
DEINONYCHUS_BONE  = 8780
EPITAPH_OF_WISDOM = 8781

# MOBS
ORNITHOMIMUS = [ 22200,22201,22202,22219,22224,22742,22744 ]
DEINONYCHUS  = [ 16067,22203,22204,22205,22220,22225,22743,22745 ]

# DROP
DROP_CHANCE = 30

class Quest (JQuest) :
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.questItemIds = [GAZKH_FRAGMENT,ORNITHOMIMUS_CLAW,DEINONYCHUS_BONE,EPITAPH_OF_WISDOM]

	def onAdvEvent(self, event, npc, player) :
		htmltext = event
		st = player.getQuestState(qn)
		if not st : return

		cond = st.getInt("cond")

		if event == "32114-05.htm" :
			st.setState(STARTED)
			st.set("cond","1")
			st.playSound("ItemSound.quest_accept")
		elif event == "32114-09.htm" and cond == 1 :
			st.set("cond","2")
			st.giveItems(GAZKH_FRAGMENT,1)
			st.playSound("ItemSound.quest_middle")
		elif event == "32117-08.htm" and cond == 2 :
			st.set("cond","3")
			st.playSound("ItemSound.quest_middle")
		elif event == "32117-14.htm" and cond == 4 :
			st.set("cond","5")
			st.playSound("ItemSound.quest_middle")
		elif event == "32119-02.htm" :
			st.set("pilar1","0")
		elif cond == 5 and event.isdigit() :
			correct = st.getInt("pilar1")
			st.set("pilar1", str(correct+1))
			htmltext = "32119-0"+str(int(event)+2)+".htm"
		elif event == "32119-06.htm" and cond == 5 :
			if st.getInt("pilar1") < 4 :
				htmltext = "32119-00.htm"
			st.unset("pilar1")
		elif event == "32119-14.htm" and cond == 5 :
			st.set("cond","6")
			st.playSound("ItemSound.quest_middle")
		elif event == "32120-02.htm" :
			st.set("pilar2","0")
		elif cond == 6 and event.isdigit() :
			correct = st.getInt("pilar2")
			st.set("pilar2", str(correct+1))
			htmltext = "32120-0"+str(int(event)+2)+".htm"
		elif event == "32120-06.htm" and cond == 6 :
			if st.getInt("pilar2") < 4 :
				htmltext = "32120-00.htm"
			st.unset("pilar2")
		elif event == "32120-15.htm" and cond == 6 :
			st.set("cond","7")
			st.playSound("ItemSound.quest_middle")
		elif event == "32121-02.htm" :
			st.set("pilar3","0")
		elif cond == 7 and event.isdigit() :
			correct = st.getInt("pilar3")
			st.set("pilar3", str(correct+1))
			htmltext = "32121-0"+str(int(event)+2)+".htm"
		elif event == "32121-06.htm" and cond == 7 :
			if st.getInt("pilar3") < 4 :
				htmltext = "32121-00.htm"
			st.unset("pilar3")
		elif event == "32121-16.htm" and cond == 7 :
			st.set("cond","8")
			st.takeItems(GAZKH_FRAGMENT,-1)
			st.giveItems(EPITAPH_OF_WISDOM,1)
			st.playSound("ItemSound.quest_middle")
		return htmltext


	def onTalk (self, npc, player) :
		htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
		st = player.getQuestState(qn)
		if not st : return htmltext

		cond = st.getInt("cond")
		npcId = npc.getNpcId()

		if npcId == MUSHIKA :
			first = player.getQuestState("124_MeetingTheElroki")
			if st.getState() == COMPLETED :
				htmltext = "<html><body>This quest has already been completed.</body></html>"
			elif first and first.getState().getName() == 'Completed' and st.getState() == CREATED and player.getLevel() >= 76 :
				htmltext = "32114-01.htm"
			elif cond == 0 :
				htmltext = "32114-00.htm"
			elif cond == 1 :
				htmltext = "32114-07.htm"
			elif cond == 2 :
				htmltext = "32114-10.htm"
			elif cond >= 3 and cond < 8:
				htmltext = "32114-11.htm"
			elif cond == 8 :
				st.addExpAndSp(859195,86603)
				st.unset("cond")
				st.unset("pilar1")
				st.unset("pilar2")
				st.unset("pilar3")
				st.setState(COMPLETED)
				st.exitQuest(False)
				st.playSound("ItemSound.quest_finish")
				htmltext = "32114-12.htm"
		elif npcId == KARAKAWEI :
			if cond == 2 :
				htmltext = "32117-01.htm"
			elif cond == 3 :
				htmltext = "32117-09.htm"
			elif cond == 4 :
				st.takeItems(ORNITHOMIMUS_CLAW,-1)
				st.takeItems(DEINONYCHUS_BONE,-1)
				st.playSound("ItemSound.quest_middle")
				htmltext = "32117-10.htm"
			elif cond == 5 :
				htmltext = "32117-15.htm"
			elif cond == 6 or cond == 7 :
				htmltext = "32117-16.htm"
			elif cond == 8 :
				htmltext = "32117-17.htm"
		elif npcId == ULU_KAIMU :
			if cond == 5 :
				npc.doCast(SkillTable.getInstance().getInfo(5089,1))
				htmltext = "32119-01.htm"
			elif cond == 6 :
				htmltext = "32119-14.htm"
		elif npcId == BALU_KAIMU :
			if cond == 6 :
				npc.doCast(SkillTable.getInstance().getInfo(5089,1))
				htmltext = "32120-01.htm"
			elif cond == 7 :
				htmltext = "32120-16.htm"
		elif npcId == CHUTA_KAIMU :
			if cond == 7 :
				npc.doCast(SkillTable.getInstance().getInfo(5089,1))
				htmltext = "32121-01.htm"
			elif cond == 8 :
				htmltext = "32121-17.htm"
		return htmltext

	def onKill(self, npc, player, isPet) :
		st = player.getQuestState(qn)
		if not st : return

		if st.getInt("cond") == 3 :
			if npc.getNpcId() in ORNITHOMIMUS :
				if st.getQuestItemsCount(ORNITHOMIMUS_CLAW) < 2 :
					if st.getRandom(100) < DROP_CHANCE :
						st.giveItems(ORNITHOMIMUS_CLAW,1)
						st.playSound("ItemSound.quest_itemget")
			elif npc.getNpcId() in DEINONYCHUS :
				if st.getQuestItemsCount(DEINONYCHUS_BONE) < 2 :
					if st.getRandom(100) < DROP_CHANCE :
						st.giveItems(DEINONYCHUS_BONE,1)
						st.playSound("ItemSound.quest_itemget")
			if st.getQuestItemsCount(ORNITHOMIMUS_CLAW) == 2 and st.getQuestItemsCount(DEINONYCHUS_BONE) == 2 :
				st.set("cond","4")
				st.playSound("ItemSound.quest_middle")
		return

QUEST	= Quest(125,qn,"The Name of Evil - 1")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(MUSHIKA)
QUEST.addTalkId(MUSHIKA)
QUEST.addTalkId(KARAKAWEI)
QUEST.addTalkId(ULU_KAIMU)
QUEST.addTalkId(BALU_KAIMU)
QUEST.addTalkId(CHUTA_KAIMU)

for i in ORNITHOMIMUS :
	QUEST.addKillId(i)
for i in DEINONYCHUS :
	QUEST.addKillId(i)
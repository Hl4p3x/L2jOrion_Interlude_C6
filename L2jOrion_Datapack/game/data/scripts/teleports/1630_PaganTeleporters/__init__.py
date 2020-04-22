import sys
from l2jorion.game.datatables.csv import DoorTable
from l2jorion.game.model.actor.instance import L2PcInstance
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "1630_PaganTeleporters"
NPCS = [32034, 32035, 32036, 32037]

# Main Quest Code
class Quest(JQuest):
    def __init__(self, id, name, descr):
        JQuest.__init__(self, id, name, descr)

    def onAdvEvent(self, event, npc, pc):
        if event == "Close_Door1":
            DoorTable.getInstance().getDoor(19160001).closeMe()
        elif event == "Close_Door2":
            DoorTable.getInstance().getDoor(19160010).closeMe()
            DoorTable.getInstance().getDoor(19160011).closeMe()
        return

    def onTalk(self, npc, player):
        st = player.getQuestState(qn)
        npcId = npc.getNpcId()
        htmltext = None
        if npcId == 32034:
            if st.getQuestItemsCount(8064) == 0 and st.getQuestItemsCount(8065) == 0 and st.getQuestItemsCount(
                8067) == 0:
                return "<html><body>The Temple Gatekeeper:<br>You have nothing that would cover the holes.<br>(You must have a Visitor's Mark, a Faded Visitor's Mark, or a Pagan's Mark in order to open this door.)</body></html>"
            if st.getQuestItemsCount(8064):
                st.takeItems(8064, 1) # TODO: this part must happen when u walk through doors >.<
                st.giveItems(8065, 1)
            htmltext = "FadedMark.htm"
            DoorTable.getInstance().getDoor(19160001).openMe()
            self.startQuestTimer("Close_Door1", 10000, None, None)
        elif npcId == 32035:
            DoorTable.getInstance().getDoor(19160001).openMe()
            self.startQuestTimer("Close_Door1", 10000, None, None)
            htmltext = "FadedMark.htm"
        elif npcId == 32036:
            if not st.getQuestItemsCount(8067):
                htmltext = "<html><body>The Temple Gatekeeper:<br>Show your Mark or be gone from my sight!<br>Only those who possess the Pagan's Mark may pass through this gate!</body></html>"
            else:
                htmltext = "<html><body>The Temple Gatekeeper:<br>On seeing the Pagan's Mark, the statue's probing eyes go blank.<br>With the quiet whir of an engine, the gate swings open...</body></html>"
                self.startQuestTimer("Close_Door2", 10000, None, None)
                DoorTable.getInstance().getDoor(19160010).openMe()
                DoorTable.getInstance().getDoor(19160011).openMe()
        elif npcId == 32037:
            DoorTable.getInstance().getDoor(19160010).openMe()
            DoorTable.getInstance().getDoor(19160011).openMe()
            self.startQuestTimer("Close_Door2", 10000, None, None)
            htmltext = "FadedMark.htm"
        st.exitQuest(1)
        return htmltext

# Quest class and state definition
QUEST = Quest(-1, qn, "Teleports")

# Quest NPC starter initialization
for npc in NPCS:
    QUEST.addStartNpc(npc)
    QUEST.addTalkId(npc)
QUEST.addFirstTalkId(32039)
QUEST.addFirstTalkId(32040)
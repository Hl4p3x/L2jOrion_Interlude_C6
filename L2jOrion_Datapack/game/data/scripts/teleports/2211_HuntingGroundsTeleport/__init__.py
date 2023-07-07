import sys
from l2jorion.game.model.actor.instance import L2PcInstance
from l2jorion.game.model.quest          import State
from l2jorion.game.model.quest          import QuestState
from l2jorion.game.model.quest.jython   import QuestJython as JQuest

qn = "2211_HuntingGroundsTeleport"

GLUDIN_DAWN,GLUDIO_DAWN,DION_DAWN,GIRAN_DAWN,HEINE_DAWN,OREN_DAWN,ADEN_DAWN,\
GLUDIN_DUSK,GLUDIO_DUSK,DION_DUSK,GIRAN_DUSK,HEINE_DUSK,OREN_DUSK,ADEN_DUSK = range(31078,31092)
HW_DAWN,HW_DUSK = range(31168,31170)
GODDARD_DAWN,GODDARD_DUSK,RUNE_DAWN,RUNE_DUSK = range(31692,31696)
SCHUTTGART_DAWN,SCHUTTGART_DUSK = range(31997,31999)

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (self, npc, player):
    npcId = npc.getNpcId()
    if npcId in [GLUDIN_DAWN,GLUDIN_DUSK] :
          htmltext = "hg_gludin.htm"
    elif npcId in [GLUDIO_DAWN,GLUDIO_DUSK] :
          htmltext = "hg_gludio.htm"
    elif npcId in [DION_DAWN,DION_DUSK] :
          htmltext = "hg_dion.htm"
    elif npcId in [GIRAN_DAWN,GIRAN_DUSK] :
          htmltext = "hg_giran.htm"
    elif npcId in [OREN_DAWN,OREN_DUSK] :
          htmltext = "hg_oren.htm"
    elif npcId in [ADEN_DAWN,ADEN_DUSK] :
          htmltext = "hg_aden.htm"
    elif npcId in [HEINE_DAWN,HEINE_DUSK] :
          htmltext = "hg_heine.htm"
    elif npcId in [HW_DAWN,HW_DUSK] :
          htmltext = "hg_hw.htm"
    elif npcId in [GODDARD_DAWN,GODDARD_DUSK] :
          htmltext = "hg_goddard.htm"
    elif npcId in [RUNE_DAWN,RUNE_DUSK] :
          htmltext = "hg_rune.htm"
    elif npcId in [SCHUTTGART_DAWN,SCHUTTGART_DUSK] :
          htmltext = "hg_schuttgart.htm"
    else:
          htmltext = "hg_wrong.htm"
    return htmltext

QUEST    = Quest(2211, qn, "Teleports")
CREATED    = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)

for i in range(31078,31092)+range(31168,31170)+range(31692,31696)+range(31997,31999) :
    QUEST.addStartNpc(i)
    QUEST.addTalkId(i)
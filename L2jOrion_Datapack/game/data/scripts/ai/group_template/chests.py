import sys
from l2jorion.game.ai import CtrlIntention
from l2jorion.game.enums import AchType
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.util.random import Rnd

SKILL_DELUXE_KEY = 2229

#Base chance for BOX to be opened
BASE_CHANCE = 100

# Percent to decrease base chance when grade of DELUXE key not match
LEVEL_DECREASE = 40

# Chance for a chest to actually be a BOX (as opposed to being a mimic).
IS_BOX = 40

class chests(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self,id,name,descr):
        # firstly, don't forget to call the parent constructor to prepare the event triggering
        # mechanisms etc.
        JQuest.__init__(self,id,name,descr)

        self.chests = [18265,18266,18267,18268,18269,18270,18271,18272,18273,18274, \
                       18275,18276,18277,18278,18279,18280,18281,18282,18283,18284, \
                       18285,18286,18287,18288,18289,18290,18291,18292,18293,18294, \
                       18295,18296,18297,18298,21671,21694,21717,21740,21763,21786, \
                       21801,21802,21803,21804,21805,21806,21807,21808,21809,21810, \
                       21811,21812,21813,21814,21815,21816,21817,21818,21819,21820, \
                       21821,21822]

        for i in self.chests :
            self.addSkillUseId(i)
            self.addAttackId(i)

    def onSkillUse (self,npc,player,skill):
        npcId = npc.getNpcId()
        skillId = skill.getId()
        skillLevel= skill.getLevel()

        # check if the npc and skills used are valid for this script.  Exit if invalid.
        if npcId not in self.chests : return

        # if this has already been interacted, no further ai decisions are needed
        # if it's the first interaction, check if this is a box or mimic
        if not npc.isInteracted() :
            npc.setInteracted()
            if Rnd.get(100) < IS_BOX :
                # if it's a box, either it will be successfully openned by a proper key, or instantly disappear
                if skillId == SKILL_DELUXE_KEY :
                    # check the chance to open the box
                    keyLevelNeeded = int(npc.getLevel()/10)
                    levelDiff = keyLevelNeeded - skillLevel
                    if levelDiff < 0 :
                        levelDiff = levelDiff * (-1)
                    chance = BASE_CHANCE - levelDiff * LEVEL_DECREASE

                    # success, pretend-death with rewards:  npc.reduceCurrentHp(99999999, player)
                    if Rnd.get(100) < chance :
                        npc.setMustRewardExpSp(False)
                        npc.setSpecialDrop();
                        npc.reduceCurrentHp(99999999, player)
                        player.getAchievement().increase(AchType.OPEN_CHEST);
                        return
                # used a skill other than chest-key, or used a chest-key but failed to open: disappear with no rewards    
                npc.onDecay()
            else :
                attacker = player
                if npc.getAttackByList().contains(player.getPet()):
                    attacker = player.getPet()
                npc.setRunning()
                npc.addDamageHate(attacker,0,999)
                npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker)
        return

    def onAttack(self,npc,player,damage,isPet) :
        npcId = npc.getNpcId()
        # check if the npc and skills used are valid for this script.  Exit if invalid.
        if npcId not in self.chests : return

        # if this was a mimic, set the target, start the skills and become agro
        if not npc.isInteracted() :
            npc.setInteracted()
            if Rnd.get(100) < IS_BOX :
                npc.onDecay()
            else :  # if this weren't a box, upon interaction start the mimic behaviors...
                # todo: perhaps a self-buff (skill id 4245) with random chance goes here?
                attacker = player
                if isPet:
                    attacker = player.getPet()
                npc.setRunning()
                npc.addDamageHate(attacker,0,(damage*100)/(npc.getLevel()+7))
                npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker)
        return

# now call the constructor (starts up the ai)
QUEST           = chests(-1,"chests","ai")
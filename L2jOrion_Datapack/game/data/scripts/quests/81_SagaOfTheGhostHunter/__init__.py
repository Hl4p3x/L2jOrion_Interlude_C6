import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from quests.SagasSuperclass import Quest as JQuest

qn = "81_SagaOfTheGhostHunter"
qnu = 81
qna = "Saga of the Ghost Hunter"

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     # first initialize the quest.  The superclass defines variables, instantiates States, etc
     JQuest.__init__(self,id,name,descr)
     # Next, override necessary variables:
     self.NPC = [31603,31624,31286,31615,31617,31646,31649,31653,31654,31655,31656,31616]
     self.Items = [7080,7518,7081,7496,7279,7310,7341,7372,7403,7434,7104,0]
     self.Mob = [27301,27230,27304]
     self.qn = qn
     self.classid = 108
     self.prevclass = 0x24
     self.X = [164650,47391,47429]
     self.Y = [-74121,-56929,-56923]
     self.Z = [-2871,-2370,-2383]
     self.Text = ["PLAYERNAME! Pursued to here! However, I jumped out of the Banshouren boundaries! You look at the giant as the sign of power!",
                  "... Oh ... good! So it was ... let's begin!","I do not have the patience ..! I have been a giant force ...! Cough chatter ah ah ah!",
                  "Paying homage to those who disrupt the orderly will be PLAYERNAME's death!","Now, my soul freed from the shackles of the millennium, Halixia, to the back side I come ...",
                  "Why do you interfere others' battles?","This is a waste of time.. Say goodbye...!","...That is the enemy",
                  "...Goodness! PLAYERNAME you are still looking?","PLAYERNAME ... Not just to whom the victory. Only personnel involved in the fighting are eligible to share in the victory.",
                  "Your sword is not an ornament. Don't you think, PLAYERNAME?","Goodness! I no longer sense a battle there now.","let...","Only engaged in the battle to bar their choice. Perhaps you should regret.",
                  "The human nation was foolish to try and fight a giant's strength.","Must...Retreat... Too...Strong.","PLAYERNAME. Defeat...by...retaining...and...Mo...Hacker","....! Fight...Defeat...It...Fight...Defeat...It..."]
     # finally, register all events to be triggered appropriately, using the overriden values.
     JQuest.registerNPCs(self)

QUEST       = Quest(qnu,qn,qna)

QUEST.setInitialState(QUEST.CREATED)
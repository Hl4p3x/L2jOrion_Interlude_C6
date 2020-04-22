import sys
from l2jorion.game.datatables         import SkillTable
from l2jorion.game.network.serverpackets      import CreatureSay 
from l2jorion.game.network.serverpackets      import MagicSkillUser
from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn="501_ProofOfClanAlliance"
qd="Proof of Clan Alliance"

# debug facility, turn this to 0 to disable
DEBUG = 1

# Quest Npcs
SIR_KRISTOF_RODEMAI  = 30756
STATUE_OF_OFFERING   = 30757
WITCH_ATHREA         = 30758
WITCH_KALIS          = 30759

# Quest Items
HERB_OF_HARIT      = 3832
HERB_OF_VANOR      = 3833
HERB_OF_OEL_MAHUM  = 3834
BLOOD_OF_EVA       = 3835
SYMBOL_OF_LOYALTY  = 3837
PROOF_OF_ALLIANCE  = 3874
VOUCHER_OF_FAITH   = 3873
ANTIDOTE_RECIPE    = 3872
POTION_OF_RECOVERY = 3889

#Quest mobs, locs and drop
CHESTS = range(27173,27178)
CHEST_LOCS = [
   [102273,103433,-3512],
   [102190,103379,-3524],
   [102107,103325,-3533],
   [102024,103271,-3500],
   [102327,103350,-3511],
   [102244,103296,-3518],
   [102161,103242,-3529],
   [102078,103188,-3500],
   [102381,103267,-3538],
   [102298,103213,-3532],
   [102215,103159,-3520],
   [102132,103105,-3513],
   [102435,103184,-3515],
   [102352,103130,-3522],
   [102269,103076,-3533],
   [102186,103022,-3541]
   ]

MOBS={
    20685 : HERB_OF_VANOR,
    20644 : HERB_OF_HARIT,
    20576 : HERB_OF_OEL_MAHUM
    }

def leader(player) :
    leaderst = None
    clan = player.getClan()
    if clan :
        leader=clan.getLeader().getPlayerInstance()
        if leader :
           leaderst = leader.getQuestState(qn)
    return leaderst

def isEffected(player,skillId) :
    bool = 0
    effect = player.getFirstEffect(skillId)
    if effect :
        bool = 1
    return bool
        
class Quest (JQuest) :

 def __init__(self,id,name,descr) :
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [HERB_OF_VANOR, HERB_OF_HARIT, HERB_OF_OEL_MAHUM, SYMBOL_OF_LOYALTY, ANTIDOTE_RECIPE, VOUCHER_OF_FAITH, POTION_OF_RECOVERY, ANTIDOTE_RECIPE]
     self.athrea = self.chests = 0

 def onAdvEvent (self,event,npc,player) :
   if event == "chest_timer" :
     self.athrea = 0
     return
   if player.isClanLeader() : leaderst = st = player.getQuestState(qn)
   else :
       # non-leaders doing this quest need both their own quest state and the leader's
       st = player.getQuestState(qn)
       if not st: return
       leaderst = leader(player)

   if not leaderst :
       if DEBUG :
           debug = "Event can't find leader"
           print debug
           return debug
       return
   debug = ""
   htmltext = event
   if player.isClanLeader() :
       if event == "30756-07.htm" :
           st.playSound("ItemSound.quest_accept")
           st.set("cond","1")
           st.setState(STARTED)
           st.set("part","1")
       elif event == "30759-03.htm" :
           st.set("part","2")
           st.set("cond","2")
           st.set("dead_list"," ")
       elif event == "30759-07.htm" :
           st.takeItems(SYMBOL_OF_LOYALTY,1) #Item is not stackable, thus need to do this for each item
           st.takeItems(SYMBOL_OF_LOYALTY,1)
           st.takeItems(SYMBOL_OF_LOYALTY,1)
           st.giveItems(ANTIDOTE_RECIPE,1)
           st.set("part","3")
           st.set("cond","3")
           st.startQuestTimer("poison_timer",3600000)
           st.addNotifyOfDeath(player)
           skill = SkillTable.getInstance().getInfo(4082,1)
           npc.setTarget(player)
           npc.broadcastPacket(MagicSkillUser(npc,player,skill.getDisplayId(),1,skill.getHitTime(),0))
           npc.callSkill(skill, [player])
       elif event == "poison_timer" :
           st.exitQuest(1)
           if DEBUG :
               debug = "Times Up! Quest failed!"
               print debug
               return debug
           return
   elif event == "30757-05.htm" :
       if player.isClanLeader() : return "Only Clan Members can sacrifice themselves!"
       if st.getRandom(10) > 5 :
           htmltext = "30757-06.htm"
           st.giveItems(SYMBOL_OF_LOYALTY,1)
           deadlist = leaderst.get("dead_list").split()
           deadlist.append(player.getName())
           leaderst.set("dead_list"," ".join(deadlist))
       else :
           skill = SkillTable.getInstance().getInfo(4083,1)
           npc.setTarget(player)
           npc.broadcastPacket(MagicSkillUser(npc,player,skill.getDisplayId(),1,skill.getHitTime(),0))
           npc.callSkill(skill, [player])
           self.startQuestTimer(player.getName(),4000,npc,player)
   elif event == player.getName() :
       if player.isDead() :
           st.giveItems(SYMBOL_OF_LOYALTY,1)
           deadlist = leaderst.get("dead_list").split()
           deadlist.append(player.getName())
           leaderst.set("dead_list"," ".join(deadlist))
       elif DEBUG :
           debug = "player " + player.getName() + " didn't die!"
           print debug
           return debug
       return
   elif event == "30758-03.htm" :
       if not self.athrea :
           self.athrea = 1
           self.chests = 0
           leaderst.set("part","4")
           leaderst.set("chest_wins","0")
           for x,y,z in CHEST_LOCS :
               rand = st.getRandom(5)
               self.addSpawn(27173+rand,x,y,z,0,0,300000)
               self.startQuestTimer("chest_timer",300000,npc,player)
       else :
           htmltext = "30758-04.htm"
   elif event == "30758-07.htm" :
       if st.getQuestItemsCount(57) >= 10000 and not self.athrea :
           htmltext = "30758-08.htm"
           st.takeItems(57,10000)
   if DEBUG and debug :
       print debug
       return debug
   return htmltext

 def onTalk (self,npc,player) :
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   st = player.getQuestState(qn)
   if not st : return htmltext
   npcId = npc.getNpcId()
   id = st.getState()
   clan = player.getClan()
   part = st.getInt("part")
   debug = ""
   if npcId == SIR_KRISTOF_RODEMAI :
       if id == CREATED :
           if player.isClanLeader() and clan :
               level = clan.getLevel()
               if level <= 2 :
                   htmltext = "01"
               elif level >= 4 :
                   htmltext = "02"
               elif level == 3 :
                   if st.getQuestItemsCount(PROOF_OF_ALLIANCE) : #you already have the item, no need to complete the quest!
                       htmltext = "03"
                   else :
                       htmltext = "04"
           else :
               htmltext = "05"
       elif id == STARTED :
           htmltext = "10"
           if st.getQuestItemsCount(VOUCHER_OF_FAITH) and part == 6 :
               st.playSound("ItemSound.quest_finish")
               st.takeItems(VOUCHER_OF_FAITH,1)
               st.giveItems(PROOF_OF_ALLIANCE,1)
               st.addExpAndSp(0,120000)
               st.exitQuest(False)
               htmltext = "09"
   elif npcId == WITCH_KALIS :
       if id == CREATED : #not a valid leader (has not started the quest yet) or a clan member
           leaderst = leader(player)
           if leaderst :
               if player.isClanLeader() or leaderst == st : return "You must see Rodemai to start the quest! I cannot help you!"
               elif leaderst.getState() == STARTED :
                   htmltext = "12"
               elif DEBUG: debug = "Leader needs to start the quest!"
           elif DEBUG : debug = "Kalis cannot find leader"
       elif id == STARTED :
           symbol = st.getQuestItemsCount(SYMBOL_OF_LOYALTY)
           if part == 1 :#and not symbol :
               htmltext = "01"
           elif part == 2 and symbol < 3 :
               htmltext = "05"
           elif symbol >= 3 and not isEffected(player,4082) :
               htmltext = "06"
           elif part == 5 and st.getQuestItemsCount(HERB_OF_HARIT) and st.getQuestItemsCount(HERB_OF_VANOR) and \
                st.getQuestItemsCount(HERB_OF_OEL_MAHUM) and st.getQuestItemsCount(BLOOD_OF_EVA) and isEffected(player,4082):
               htmltext = "08"
               st.giveItems(VOUCHER_OF_FAITH,1)
               st.giveItems(POTION_OF_RECOVERY,1)
               for item in range(3832,3836) + [ANTIDOTE_RECIPE] :
                   st.takeItems(item,-1)
               st.set("part","6")
               st.set("cond","4")
               timer = st.getQuestTimer("poison_timer")
               if timer != None : timer.cancel()
           elif part == 3 or part == 4 or part == 5 :
               if not isEffected(player,4082) :
                   htmltext = "09"
                   st.set("part","1")
                   st.takeItems(ANTIDOTE_RECIPE,-1)
               else :
                   htmltext = "10"
           elif part == 6 :
               htmltext = "11"
           elif DEBUG : debug = "Uhhh....Kalis is confused by player: " + str(player)
       elif DEBUG : debug = "Leader has already finished the quest!"
   elif npcId == STATUE_OF_OFFERING :
       leaderst = leader(player)
       if leaderst :
           id = leaderst.getState()
           if id == STARTED :
               if leaderst.getInt("part") == 2 :
                   if player.isClanLeader() or leaderst == st :
                       htmltext = "02"
                   else :
                       if player.getLevel() >= 40 :
                           dlist = leaderst.get("dead_list").split()
                           if player.getName() not in dlist and len(dlist) < 3:
                               htmltext = "01"
                           else :
                               htmltext = "03"
                       else :
                           htmltext = "04"
               elif DEBUG : debug = "wrong state for sacrifice"
           else :
               htmltext = "08"
               if DEBUG : debug = "Leader must start the quest or has already finished the quest!"
       elif DEBUG : debug = "Statue can't find leader"
   elif npcId == WITCH_ATHREA :
       leaderst = leader(player)
       if leaderst :
           id = leaderst.getState()
           if id == STARTED :
               part = leaderst.getInt("part")
               if part == 3 and leaderst.getQuestItemsCount(ANTIDOTE_RECIPE) and not leaderst.getQuestItemsCount(BLOOD_OF_EVA) :
                   htmltext = "01"
               elif part == 5 :
                   htmltext = "10"
               elif part == 4 :
                   htmltext = "06"
                   if leaderst.getInt("chest_wins") >= 4 :
                       htmltext = "09"
                       st.giveItems(BLOOD_OF_EVA,1)
                       leaderst.set("part","5")
               elif DEBUG : debug = "You should go seek help elsewhere! I cannot help you in your current state!"
           elif DEBUG : debug = "You must have the quest started!"
       elif DEBUG : debug = "Athrea can't find your leader!"
   if DEBUG and debug :
       print debug
       return debug
   if htmltext.isdigit() :
       htmltext = str(npcId) + "-" + htmltext + ".htm"
   return htmltext

 def onKill(self,npc,player,isPet) :
     leaderst = leader(player)
     if not leaderst : return
     if not leaderst.getState() == STARTED :
         if DEBUG :
             print "onKill says leader needs to start quest"
             return "Need to start quest!"
         return
     part = leaderst.getInt("part")
     npcId = npc.getNpcId()
     if npcId in CHESTS and part == 4 :
         wins = leaderst.getInt("chest_wins")
         if (self.chests - wins) == 12 or (wins < 4 and not leaderst.getRandom(4)) :
             wins += 1
             leaderst.set("chest_wins",str(wins))
             npc.broadcastPacket(CreatureSay(npc.getObjectId(),0,npc.getName(),"###### BINGO! ######"))
         self.chests += 1
     elif npcId in MOBS.keys() :
         st = player.getQuestState(qn)
         if not st : st = self.newQuestState(player)
         if st == leaderst : return
         if part >=3 and part < 6 :
             if not st.getRandom(10) :
                 st.giveItems(MOBS[npcId],1)
                 st.playSound("ItemSound.quest_itemget")
         elif DEBUG :
             print "onKill says leader is not correct state"+str(part)
             return "leader is not correct state"
     return

 # only leaders are registered for onDeath.  Therefore, st should always match that of the leader
 def onDeath(self, npc, pc, st) :
     if st.getPlayer() == pc :
         timer1 = st.getQuestTimer("poison_timer")
         if timer1 != None : timer1.cancel()
         st.exitQuest(1)
         if DEBUG :
             print "leader died, quest failed"
             return "Leader died, quest failed"
     return

QUEST     = Quest(501,qn,qd)
CREATED   = State('Start', QUEST)
STARTED   = State('Started', QUEST)
COMPLETED = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(SIR_KRISTOF_RODEMAI)
QUEST.addStartNpc(STATUE_OF_OFFERING)

for i in [SIR_KRISTOF_RODEMAI,STATUE_OF_OFFERING,WITCH_KALIS,WITCH_ATHREA] :
    QUEST.addTalkId(i)

for i in MOBS.keys() + CHESTS :
    QUEST.addKillId(i)

import sys

from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "4000_ShadowWeapons"

#NPCs that would need to figure out what to show when asked about coupons exchange
NPC = [ 30026,30037,30066,30070,30109,30115,30120,30174,30175,30176,30187,30191, \
       30195,30288,30289,30290,30297,30373,30462,30474,30498,30499,30500,30503, \
       30504,30505,30511,30512,30513,30676,30677,30681,30685,30687,30689,30694, \
       30699,30704,30845,30847,30849,30854,30857,30862,30865,30894,30897,30900, \
       30905,30910,30913,31269,31272,31288,31314,31317,31321,31324,31326,31328, \
       31331,31334,31336,31965,31974,31276,31285,31996,32094,32096,32098 ]
#itemId for shadow weapon coupons, it's not used more than once but increases readability
D_COUPON = 8869
C_COUPON = 8870

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (Self,npc,player):
    st = player.getQuestState(qn)
    if not st: return
    has_d=st.getQuestItemsCount(D_COUPON)
    has_c=st.getQuestItemsCount(C_COUPON)
    if  has_d or has_c :
      #let's assume character had both c & d-grade coupons, we'll confirm later
      multisell=306893003
      if not has_d :
         #if s/he had c-grade only...
         multisell=306893002
      elif not has_c :
         #or d-grade only.
         multisell=306893001
      #finally, return htm with proper multisell value in it.
      htmltext=st.showHtmlFile("exchange.htm").replace("%msid%",str(multisell))
    else :
      htmltext="exchange-no.htm"
    st.exitQuest(1)
    return htmltext

QUEST       = Quest(4000,qn,"Custom")
CREATED     = State('Start', QUEST)

QUEST.setInitialState(CREATED)

for item in NPC:
   QUEST.addStartNpc(item)
   QUEST.addTalkId(item)
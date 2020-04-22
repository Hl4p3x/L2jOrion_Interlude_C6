import sys
from l2jorion.game.ai import CtrlIntention
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import CreatureSay
from l2jorion.util.random import Rnd

POLLEN = 6391
SKILL_NECTAR = 9998

# Поливаемые
WATERED_SQUASH = [12774,12775,12776,12777,12778,12779]

class squash(JQuest) :

    def __init__(self,id,name,descr):
        JQuest.__init__(self,id,name,descr)
        # Выросшие
        self.adultSmallSquash = [12775,12776]
        self.adultLargeSquash = [12778,12779]

    def onAdvEvent(self,event,npc,player) :
        objId = npc.getObjectId()
        if event == "Good By" and npc and player :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Good By!!  LOL."))
            npc.onDecay()
        elif event == "Good By1" and npc and player :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Всем, до свидания... Большая тыква сказала до свидания ..."))
            npc.onDecay()
        elif event == "Good By2" and npc and player :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Вы можете быстрее? Через 30 секунд я сбегу ..."))
        elif event == "Good By3" and npc and player :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Я прерву отношения с Вами через 20 секунд!"))
        elif event == "Good By4" and npc and player :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"У меня осталось всего 10 секунд! 9. 8. 7 ..!"))
        elif event == "Good By5" and npc and player :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Эй! Счастливо оставаться! Идиот, забудь обо мне!"))
        return

    def onSkillUse(self,npc,player,skill):
        npcId = npc.getNpcId()
        skillId = skill.getId()
        if skillId != SKILL_NECTAR : return
        if npcId not in WATERED_SQUASH : return
        objectId = npc.getObjectId()
        if skillId == SKILL_NECTAR :
            # Первый полив
            if npc.getNectar() == 0 :
                if Rnd.get(2) == 1 :
                    mytext = ["Чтобы быть способной расти, я должна пить только нектар ... причем чаще",
                              "Если ты будеш быстрее выливать мне нектар - я быстрее выросту!",
                              "Ну, верьте мне, прыскайте нектар! Я могу конечно превратиться в большую тыкву!!!",
                              "Принеси нектар, чтобы вырастить тыкву!",
                              "Плод прекрасной молодой тыквы начинает блестеть, когда семя предано земле! С этого времени будет способен стать здоровым и сильным!",
                              "О, давно не виделись?",
                              "Неожидал увидеть мое красивое появление?",
                              "Отлично! Это - нечто! Нектар?",
                              "Дозаправка! Заправь 5 бутылок, чтобы я смогла превратиться в большую тыкву! О!"]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
                    npc.addGood()
                else :
                    mytext = ["Не спеши! Слишком часто, я не успеваю!",
                              "Я же не автомат, меня скорострельностью не напоиш",
                              "Да куда же ты торопишься! Слишком часто, я не успеваю!",
                              "Упс, опять слишком быстро",
                              "Давай чуток помедленней, не спеши, медленно достань бутылку и медленно ее вылей!",
                              "У тебя нет чувства скорости? Медленнее давай"]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
            # Второй полив
            elif npc.getNectar() == 1 :
                if Rnd.get(2) == 1 :
                    mytext = ["Желаю стать большой тыквой!",
                              "Ням, ням, ням! Вышло! Заботится - хорошо!",
                              "Как думаеш, я зрелая или гнилая?",
                              "Нектар - только лучшее! Ха! Ха! Ха!"]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
                    npc.addGood()
                else :
                    mytext = ["О! Опять мимо! Может слишком быстро расходуеш нектар?",
                              "Если я умру такой как сейчас, Вы получите только молодую тыкву ...",
                              "Выращивают немного быстрее! Неплохо было бы стать большой тыквой, молодая тыква не хороша!",
                              "Tакую маленькую тыкву вы все должны есть? Принесите нектар, я могу быть больше!"]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
            # Третий полив
            elif npc.getNectar() == 2 :
                if Rnd.get(2) == 1 :
                    mytext = ["Tыква, изголодалась! Просит утолить жажду!",
                              "Ну наконец-то ..., это действительно вкусно! Есть еще?",
                              "Ухаживаешь за мной только для того, чтобы есть? Отлично, является случайным ваш ..., чтобы не дать манну на самоубийство"]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
                    npc.addGood()
                else :
                    mytext = ["Не воду ли Вы добавляете? Какой вкус?",
                              "Хозяин, спасите меня... Я не имею аромата нектара, я должна умереть ..."]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
            # Четвертый полив
            elif npc.getNectar() == 3 :
                if Rnd.get(2) == 1 :
                    mytext = ["Очень хорошо, делаешь чрезвычайно хорошо! Знаешь что следующим шагом должен делать?",
                              "Если Вы поймаете меня, я даю Вам 10 миллионов adena!!! Согласны?"]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
                    npc.addGood()
                else :
                    mytext = ["Я голодна, Tы хочеш чтоб я засохла?",
                              "Tребую нектар, чтобы расти немного быстрее."]
                    npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                    npc.addNectar()
            # Пятый полив
            elif npc.getNectar() == 4 :
                if Rnd.get(2) == 1 :
                    npc.addGood()
                if npc.getGood() >= 3 :
                    if npcId == 12774 :
                        newGourd = self.addSpawn(12775,npc)
                        newGourd.setOwner(player.getName())
                        self.startQuestTimer("Good By", 120000, newGourd, player)   # Через 2 минуты исчезновение
                        self.startQuestTimer("Good By2", 90000, newGourd, player)   # 30 секунд до исчезновения
                        self.startQuestTimer("Good By3", 100000, newGourd, player)  # 20 секунд до исчезновения
                        self.startQuestTimer("Good By4", 110000, newGourd, player)  # 10 секунд до исчезновения
                        mytext = ["Молодая тыква, жаждящая! Как, уже выросла?",
                                  "Я убегу через 2 минуты"]
                        npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                        npc.onDecay()
                    else :
                        newGourd = self.addSpawn(12778,npc)
                        newGourd.setOwner(player.getName())
                        self.startQuestTimer("Good By1", 120000, newGourd, player)  # Через 2 минуты исчезновение
                        self.startQuestTimer("Good By2", 90000, newGourd, player)   # 30 секунд до исчезновения
                        self.startQuestTimer("Good By3", 100000, newGourd, player)  # 20 секунд до исчезновения
                        self.startQuestTimer("Good By4", 110000, newGourd, player)  # 10 секунд до исчезновения
                        mytext = ["Милосердность является очень хорошей чертой. Tеперь посмотрите, я чувствую себя все более хорошо",
                                  "Я убегу через 2 минуты"]
                        npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                        npc.onDecay()
                else :
                    if npcId == 12774 :
                        newGourd = self.addSpawn(12776,npc)
                        newGourd.setOwner(player.getName())
                        mytext = ["Эй! Была - не была! Есть! Сейчас же! Tы не можешь должным образом заботиться? Я же так сгнию!",
                                  "Ничего себе, остановки? За что тебя благодарить",
                                  "Жажду нектара о ...",
                                  "Вы хотите большую тыкву? Но я хочу остаться маленькой тыковкой ..."]
                        npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                        npc.onDecay()
                    if npcId == 12777 :
                        newGourd = self.addSpawn(12779,npc)
                        newGourd.setOwner(player.getName())
                        mytext = ["Эй! Была - не была! Есть! Сейчас же! Tы не можешь должным образом заботиться? Я так сгнию!",
                                  "Ничего себе, остановки? За что тебя благодарить",
                                  "Жажду нектара о ...",
                                  "Вы хотите большую тыкву? Но я хочу остаться маленькой тыковкой ..."]
                        npc.broadcastPacket(CreatureSay(objectId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
                        npc.onDecay()
        return

    def onAttack(self,npc,player,damage,isPet) :
        npcId = npc.getNpcId()
        objId = npc.getObjectId()
        if npcId not in WATERED_SQUASH : return
        if npcId  in self.adultLargeSquash :
            if Rnd.get(30) < 2 :
                mytext = ["Укусы плетут кружево крысой ..., чтобы заменить ... тело ...!",
                          "Ха ха, росла! Полностью на всех!",
                          "Не можете чтоли все прицелиться? Смотрите все, чтобы не сбежала ...",
                          "Я считаю ваши удары! О, напоминает удар снова!",
                          "Не тратьте впустую ваше время!",
                          "Ха, этот звук действительно приятно слышать?",
                          "Я потребляю ваши атаки, чтобы расти!",
                          "Время, чтобы ударить снова! Ударь еще разок!",
                          "Tолько полезная музыка может открыть большую тыкву... Меня нельзя открыть с оружием!"]
                npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),mytext[Rnd.get(len(mytext))]))
        return

    def onKill(self,npc,player,isPet) :
        npcId = npc.getNpcId()
        objId = npc.getObjectId()
        if npcId not in WATERED_SQUASH : return
        if npcId in self.adultSmallSquash :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Tыква открывается!!"))
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Ееее! Открывается! Много хороших вещей  ..."))
        elif npcId in self.adultLargeSquash :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Tыква открывается!!"))
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Ееее! Открывается! Много хороших вещей  ..."))
        else :
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"За что, хозяин?!"))
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Ой, кишки вывалились!!"))
        return

QUEST = squash(-1,"group_template","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

for i in WATERED_SQUASH:
    QUEST.addSkillUseId(i)
    QUEST.addAttackId(i)
    QUEST.addKillId(i)
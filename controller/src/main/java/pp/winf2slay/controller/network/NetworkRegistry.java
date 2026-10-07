package pp.winf2slay.controller.network;

import com.jme3.network.serializing.Serializer;
import pp.winf2slay.controller.message.LobbyEntry;
import pp.winf2slay.controller.message.client.CMActivateHeroEffect;
import pp.winf2slay.controller.message.client.CMAddBot;
import pp.winf2slay.controller.message.client.CMAnimationsDone;
import pp.winf2slay.controller.message.client.CMAttackMonster;
import pp.winf2slay.controller.message.client.CMChallengeResponse;
import pp.winf2slay.controller.message.client.CMDrawCard;
import pp.winf2slay.controller.message.client.CMEndTurn;
import pp.winf2slay.controller.message.client.CMHeroSelectionResponse;
import pp.winf2slay.controller.message.client.CMJoinLobby;
import pp.winf2slay.controller.message.client.CMLeaveLobby;
import pp.winf2slay.controller.message.client.CMModifyDoubleResponse;
import pp.winf2slay.controller.message.client.CMModifySingleResponse;
import pp.winf2slay.controller.message.client.CMMulligan;
import pp.winf2slay.controller.message.client.CMPlayCard;
import pp.winf2slay.controller.message.client.CMPlayerSelectionResponse;
import pp.winf2slay.controller.message.client.CMRemoveBot;
import pp.winf2slay.controller.message.client.CMStartGame;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCCardsMoved;
import pp.winf2slay.controller.message.server.BCChallengeModified;
import pp.winf2slay.controller.message.server.BCChallengeResolved;
import pp.winf2slay.controller.message.server.BCChallengeRolled;
import pp.winf2slay.controller.message.server.BCDiceModified;
import pp.winf2slay.controller.message.server.BCDiceRolled;
import pp.winf2slay.controller.message.server.BCEffectActivated;
import pp.winf2slay.controller.message.server.BCGameStarted;
import pp.winf2slay.controller.message.server.BCMonsterAttacked;
import pp.winf2slay.controller.message.server.BCMulligan;
import pp.winf2slay.controller.message.server.BCRollResolved;
import pp.winf2slay.controller.message.server.BCTurnEnded;
import pp.winf2slay.controller.message.server.BroadcastMessage;
import pp.winf2slay.controller.message.server.ClientSync;
import pp.winf2slay.controller.message.server.PlayerSnapshot;
import pp.winf2slay.controller.message.server.SMActionRejected;
import pp.winf2slay.controller.message.server.SMChallengeRequest;
import pp.winf2slay.controller.message.server.SMContinueTurn;
import pp.winf2slay.controller.message.server.SMGameOver;
import pp.winf2slay.controller.message.server.SMHeroSelectionRequest;
import pp.winf2slay.controller.message.server.SMLobbyUpdate;
import pp.winf2slay.controller.message.server.SMModifyDoubleRequest;
import pp.winf2slay.controller.message.server.SMModifySingleRequest;
import pp.winf2slay.controller.message.server.SMNotice;
import pp.winf2slay.controller.message.server.SMPlayerSelectionRequest;
import pp.winf2slay.controller.message.server.SMTurnSwitch;
import pp.winf2slay.controller.message.server.SMWelcome;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.die.DiceResult;
import pp.winf2slay.model.effect.AttackBonus;
import pp.winf2slay.model.effect.ChallengeBonus;
import pp.winf2slay.model.effect.DestroyAllHeroes;
import pp.winf2slay.model.effect.DestroyHero;
import pp.winf2slay.model.effect.DestroyParty;
import pp.winf2slay.model.effect.DestroyRandomHeroes;
import pp.winf2slay.model.effect.DrawCards;
import pp.winf2slay.model.effect.DrawFromDiscard;
import pp.winf2slay.model.effect.EveryoneDiscards;
import pp.winf2slay.model.effect.HeroBonus;
import pp.winf2slay.model.effect.SacrificeHero;

import java.util.List;

/**
 * Registriert alle Klassen, die über das jME-Netzwerk übertragen werden.
 *
 * <p>Die Registrierung erfolgt einmalig auf dem Server; jME überträgt die
 * Zuordnung beim Verbindungsaufbau automatisch an die Clients.</p>
 */
public final class NetworkRegistry {

    /**
     * Spielname beim Verbindungsaufbau. Clients anderer jME-Spiele (z. B. Battleship)
     * werden damit sauber abgewiesen.
     */
    public static final String GAME_NAME = "WinfToSlay";

    /**
     * Protokollversion; Client und Server müssen übereinstimmen.
     */
    public static final int PROTOCOL_VERSION = 2;

    /**
     * Alle Nachrichtenklassen, für die der Server Listener registriert.
     */
    public static final List<Class<? extends ClientMessage>> CLIENT_MESSAGES = List.of(
            CMJoinLobby.class, CMLeaveLobby.class, CMAddBot.class, CMRemoveBot.class, CMStartGame.class,
            CMDrawCard.class, CMPlayCard.class, CMActivateHeroEffect.class, CMAttackMonster.class,
            CMMulligan.class, CMEndTurn.class, CMChallengeResponse.class, CMModifySingleResponse.class,
            CMModifyDoubleResponse.class, CMHeroSelectionResponse.class, CMPlayerSelectionResponse.class,
            CMAnimationsDone.class);

    private static final List<Class<? extends ServerMessage>> SERVER_MESSAGES = List.of(
            SMWelcome.class, SMLobbyUpdate.class, SMTurnSwitch.class, SMContinueTurn.class,
            SMActionRejected.class, SMChallengeRequest.class, SMModifySingleRequest.class,
            SMModifyDoubleRequest.class, SMHeroSelectionRequest.class, SMPlayerSelectionRequest.class,
            SMNotice.class, SMGameOver.class,
            BroadcastMessage.class, BCGameStarted.class, BCCardMoved.class, BCCardsMoved.class, BCMulligan.class,
            BCEffectActivated.class, BCMonsterAttacked.class, BCDiceRolled.class, BCDiceModified.class,
            BCRollResolved.class, BCChallengeRolled.class, BCChallengeModified.class, BCChallengeResolved.class,
            BCTurnEnded.class);

    private static final List<Class<?>> DATA_CLASSES = List.of(
            ClientSync.class, PlayerSnapshot.class, LobbyEntry.class, DiceResult.class,
            Card.class, Hero.class, Spell.class, Challenge.class, Modification.class, Monster.class, Leader.class,
            AttackBonus.class, ChallengeBonus.class, HeroBonus.class, DestroyHero.class, DestroyAllHeroes.class,
            DestroyParty.class, DestroyRandomHeroes.class, SacrificeHero.class, EveryoneDiscards.class,
            DrawCards.class, DrawFromDiscard.class);

    private static boolean registered;

    private NetworkRegistry() {
        // Utility-Klasse
    }

    /**
     * Registriert alle Klassen genau einmal. Muss vor dem Start des Servers
     * aufgerufen werden.
     */
    public static synchronized void registerAll() {
        if (registered) return;
        CLIENT_MESSAGES.forEach(Serializer::registerClass);
        SERVER_MESSAGES.forEach(Serializer::registerClass);
        DATA_CLASSES.forEach(Serializer::registerClass);
        registered = true;
    }
}

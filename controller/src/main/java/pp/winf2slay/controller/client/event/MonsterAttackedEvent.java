package pp.winf2slay.controller.client.event;

import pp.winf2slay.model.card.Monster;

/**
 * Ein Spieler greift ein Monster an.
 *
 * @param player Angreifer
 * @param monster angegriffenes Monster
 */
public record MonsterAttackedEvent(String player, Monster monster) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onMonsterAttacked(this);
    }
}

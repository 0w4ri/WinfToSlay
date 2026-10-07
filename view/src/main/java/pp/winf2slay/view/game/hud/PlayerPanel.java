package pp.winf2slay.view.game.hud;

import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import pp.winf2slay.controller.message.server.PlayerSnapshot;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.field.Group;
import pp.winf2slay.model.field.MonsterField;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;

import java.util.EnumSet;
import java.util.Set;

/**
 * Übersicht eines Spielers am linken Bildschirmrand: Name, Handkarten, Helden,
 * besiegte Monster, Klassen und Boni.
 */
public class PlayerPanel extends Container {

    private final Ui ui;
    private final ColorRGBA color;
    private final boolean me;
    private final Label avatar;
    private final Label name;
    private final Label hand;
    private final Label heroes;
    private final Label monsters;
    private final Label classes;
    private final Label bonus;
    private boolean active;
    private boolean bot;

    /**
     * @param ui     Oberflächenfabrik
     * @param player Spielername
     * @param color  Spielerfarbe
     * @param me     {@code true} für den eigenen Spieler
     */
    public PlayerPanel(Ui ui, String player, ColorRGBA color, boolean me) {
        super(Theme.STYLE);
        this.ui = ui;
        this.color = color;
        this.me = me;
        setBackground(ui.theme().glass());
        addChild(ui.spacer(308, 1));

        Container top = ui.row();
        avatar = ui.icon("person", 34, color);
        top.addChild(avatar);
        top.addChild(ui.spacer(8, 4));
        name = ui.outlined(me ? player + " (Du)" : player, 26, me ? Theme.GOLD_LIGHT : Theme.CREAM);
        top.addChild(name);
        addChild(top);

        Container stats = ui.row();
        hand = stat(stats, "style");
        heroes = stat(stats, "shield");
        monsters = stat(stats, "pets");
        classes = stat(stats, "school");
        addChild(stats);
        bonus = ui.text("", 18, Theme.GOLD_LIGHT);
        addChild(bonus);
    }

    private Label stat(Container row, String icon) {
        row.addChild(ui.icon(icon, 22, Theme.MUTED));
        Label value = ui.text("0", 21, Theme.CREAM);
        value.setInsets(new Insets3f(0, 3, 0, 12));
        row.addChild(value);
        return value;
    }

    /**
     * Übernimmt die Werte eines Spielers.
     *
     * @param p Spielerdaten
     */
    public void update(PlayerSnapshot p) {
        hand.setText(String.valueOf(p.getHandCount()));
        heroes.setText(p.getHeroCount() + "/" + Group.SIZE);
        monsters.setText(p.getMonsterCount() + "/" + MonsterField.SIZE);
        Set<ClassType> types = EnumSet.noneOf(ClassType.class);
        if (p.getLeader() != null) types.add(p.getLeader().getClassType());
        for (Hero h : p.getGroup()) if (h != null) types.add(h.getClassType());
        classes.setText(types.size() + "/" + Game.CLASSES_TO_WIN);
        classes.setColor(types.size() >= Game.CLASSES_TO_WIN - 1 ? Theme.GOLD_LIGHT.clone() : Theme.CREAM.clone());
        monsters.setColor(p.getMonsterCount() >= MonsterField.SIZE - 1 ? Theme.GOLD_LIGHT.clone()
                                                                       : Theme.CREAM.clone());
        StringBuilder b = new StringBuilder();
        if (p.getAttackBonus() != 0) b.append("Angriff +").append(p.getAttackBonus()).append("   ");
        if (p.getHeroBonus() != 0) b.append("Helden +").append(p.getHeroBonus()).append("   ");
        if (p.getChallengeBonus() != 0) b.append("Duell +").append(p.getChallengeBonus());
        bonus.setText(b.toString().trim());
        if (p.isBot() != bot) {
            bot = p.isBot();
            avatar.setIcon(ui.iconComponent(bot ? "smart_toy" : "person", 34, color));
        }
    }

    /**
     * @param active {@code true}, wenn der Spieler am Zug ist
     */
    public void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        setBackground(active ? ui.theme().darkPanel() : ui.theme().glass());
        name.setColor(active ? Theme.GOLD.clone() : (me ? Theme.GOLD_LIGHT.clone() : Theme.CREAM.clone()));
    }
}

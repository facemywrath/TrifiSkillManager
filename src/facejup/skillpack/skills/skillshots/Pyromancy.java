package facejup.skillpack.skills.skillshots;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftFallingBlock;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.sucy.skill.api.skills.Skill;
import com.sucy.skill.api.skills.SkillAttribute;
import com.sucy.skill.api.skills.SkillShot;

import facejup.skillpack.main.Main;
import facejup.skillpack.users.User;
import facejup.skillpack.util.Chat;
import facejup.skillpack.util.Lang;

public class Pyromancy extends Skill implements SkillShot{

	public HashMap<LivingEntity, Long> cooldown = new HashMap<>();
	public HashMap<Player, FallingBlock> fires = new HashMap<>();

	private final double COOLDOWN = 5;
	private final double MANACOST = 25;
	private final int COST = 7;
	private final int COST_SCALE = 2;
	private final int RANGE = 5;
	private final int RANGE_SCALE = 2;


	public Pyromancy(String name, String type, Material indicator, int maxLevel) {
		super(name, type, indicator, maxLevel);
		getDescription().add("&7Pick up fire and shoot it with leftclick.");
		settings.set(SkillAttribute.MANA, MANACOST);
	}

	@Override
	public boolean cast(LivingEntity player, int level) {
		if(!(player instanceof Player))
			return false;
		DecimalFormat format = new DecimalFormat("##.00");
		if(this.isOnCooldown(player, level))
		{
			player.sendMessage("Can't cast for " + format.format(((cooldown.get(player)+COOLDOWN*1000-System.currentTimeMillis())/1000.0)) + " seconds.");
			return false;
		}
		if(fires.containsKey(player) && ((CraftFallingBlock) fires.get(player)).getHandle().isAlive())
			return false;
		User user = Main.getPlugin(Main.class).getUserManager().getUser((OfflinePlayer)player);
		Block block = null;
		Location loc = player.getEyeLocation();
		for(int i = 0; i < RANGE+RANGE_SCALE * level; i++)
		{
			loc.add(player.getLocation().getDirection());
			if(loc.getBlock().getType() == Material.FIRE)
			{
				block = loc.getBlock();
				break;
			}
		}
		if(block == null)
		{
			player.sendMessage(Chat.translate(Lang.tag + "Must use on an existing flame"));
			return false;
		}
		if(!user.decMana((int) MANACOST))
		{
			player.sendMessage(Lang.tag + Chat.translate("&5Not enough mana! &c(" + user.getMana() + "/" + MANACOST + ")"));
			return false;
		}
		cooldown.put(player, System.currentTimeMillis());
		block.setType(Material.AIR);
		loc = block.getLocation().add(new Vector(0,1,0));
		fires.put((Player) player, loc.getWorld().spawnFallingBlock(loc, Material.FIRE, (byte) 0));
		moveFire((Player) player);
		return true;
	}

	public void moveFire(Player player)
	{
		if(fires.containsKey(player))
		{
			FallingBlock block = fires.get(player);
			block.setVelocity(player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5)).subtract(block.getLocation()).toVector().normalize().multiply(0.25));
			Main main = Main.getPlugin(Main.class);
			main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable()
			{
				public void run()
				{
					moveFire(player);
				}
			}, 2L);
		}
	}
	

	public boolean isOnCooldown(LivingEntity shooter, int level)
	{
		if(cooldown.containsKey(shooter))
			return (cooldown.get(shooter)+COOLDOWN*1000 > System.currentTimeMillis());
		return false;

	}

	@Override
	public int getCost(int level)
	{
		return COST + level*COST_SCALE;
	}

}

package solo.transferpro;

import java.util.ArrayList;
import java.util.HashMap;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.QueryRegenerateEvent;
import cn.nukkit.plugin.PluginBase;
import solo.solobasepackage.util.Message;

public class Main extends PluginBase implements Listener{
	
	public static Main instance;
	
	public static Main getInstance(){
		return instance;
	}
	
	public ServerManager serverManager;
	public HashMap<String, Boolean> mode = new HashMap<String, Boolean>();
	public HashMap<String, ServerInfo> queue = new HashMap<String, ServerInfo>();
	
	@Override
	public void onLoad(){
		instance = this;
	}
	
	@Override
	public void onEnable(){
		this.getDataFolder().mkdirs();

		this.serverManager = new ServerManager();
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable(){
		this.serverManager.save();
	}
	
	@EventHandler
	public void onLogin(PlayerLoginEvent event){
		if(ServerManager.useLoadBalancer){
			ServerInfo info = this.serverManager.selectServer();
			if(info != null){
				this.getServer().getLogger().info("§a" + event.getPlayer().getName() + "님이 " + info.getMotd() + "§r§a서버로 이동하셨습니다.");
				info.transfer(event.getPlayer());
				info.onlinePlayersCount++;
			}
		}
	}
	
	@EventHandler
	public void onQuery(QueryRegenerateEvent event){
		if(ServerManager.calculatePlayersCountHollServer){
			event.setMaxPlayerCount(this.serverManager.getAllMaxPlayersCount());
			event.setPlayerCount(this.serverManager.getAllOnlinePlayersCount());
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event){
		String name = event.getPlayer().getName().toLowerCase();
		TransferButton button = this.serverManager.getButton(event.getBlock());
		if(this.mode.containsKey(name)){
			if(this.mode.get(name)){
				if(button == null){
					this.serverManager.addButton(event.getBlock(), this.queue.get(name));
					Message.normal(event.getPlayer(), "성공적으로 서버이통 버튼을 생성하였습니다.");
					
					this.mode.remove(name);
					this.queue.remove(name);
						
					event.setCancelled();
				}
			}else{
				if(button != null){
					button.despawnFromAll();
					this.serverManager.removeButton(button);
					Message.normal(event.getPlayer(), "성공적으로 서버이동 버튼을 제거하였습니다.");
					
					this.mode.remove(name);
					this.queue.remove(name);
					
					event.setCancelled();
					return;
				}
			}
		}else if(button != null){
			button.onTouch(event.getPlayer());
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		this.serverManager.buttons.values().forEach((b) -> b.spawnTo(event.getPlayer()));
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event){
		String name = event.getPlayer().getName().toLowerCase();
		this.mode.remove(name);
		this.queue.remove(name);
	}
	
	@EventHandler
	public void onLevelChange(EntityLevelChangeEvent event){
		if(event.getEntity() instanceof Player){
			Player player = (Player) event.getEntity();
			for(TransferButton button : this.serverManager.buttons.values()){
				button.spawnTo(player, event.getTarget());
			}
		}
	}
	
	@EventHandler
	public void onLevelLoad(LevelLoadEvent event){
		this.serverManager.loadFromLevel(event.getLevel());
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("서버이동버튼")){
			if(args.length == 0){
				args = new String[]{"x"};
			}
			String name = sender.getName().toLowerCase();
			switch(args[0]){
				case "생성":
					String ip;
					short port;
					try{
						ip = args[1];
						port = Short.parseShort(args[2]);
					}catch(Exception e){
						Message.usage(sender, "/서버이동버튼 생성 [ip] [port]");
						return true;
					}
					ServerInfo info = this.serverManager.getServer(ip, port);
					if(info == null){
						Message.alert(sender, "해당 서버는 아직 추가되어 있지 않습니다. /서버 추가 [ip] [port] 명령어로 서버를 추가한 후 이동을 시도해주세요.");
						return true;
					}
					this.mode.put(name, true);
					this.queue.put(name, info);
					Message.normal(sender, "서버이동 버튼을 생성할 곳에 터치하세요.");
					return true;
					
				case "제거":
					this.mode.put(name, false);
					Message.normal(sender, "제거할 서버이동 버튼을 터치하세요.");
					return true;
					
				case "취소":
					if(this.queue.containsKey(name)){
						this.mode.remove(name);
						this.queue.remove(name);
						Message.normal(sender, "진행중이던 작업을 취소하였습니다.");
					}else{
						Message.alert(sender, "진행중인 작업이 없습니다.");
					}
					return true;
					
				default:
					ArrayList<String> information = new ArrayList<String>();
					information.add("§2§l/서버이동버튼 생성 [ip] [port] §r§7- 서버이동 버튼을 생성합니다.");
					information.add("§2§l/서버이동버튼 제거 §r§7- 서버이동 버튼을 제거합니다.");
					information.add("§2§l/서버이동버튼 취소 §r§7- 진행중이던 작업을 취소합니다.");
					int page = 1;
					try{
						page = Integer.parseInt(args[1]);
					}catch(Exception e){
						
					}
					Message.page(sender, "서버이동버튼 명령어 목록", information, page);
			}
		}else if(command.getName().equals("서버")){
			if(args.length == 0){
				args = new String[]{"x"};
			}
			
			String ip;
			short port;
			
			ArrayList<String> information;
			int page = 1;
			
			switch(args[0]){
				case "이동":
					if(!(sender instanceof Player)){
						Message.alert(sender, "인게임내에서만 사용가능합니다.");
						return true;
					}
					try{
						ip = args[1];
						port = Short.parseShort(args[2]);
					}catch(Exception e){
						Message.usage(sender, "/서버 이동 [ip] [port]");
						return true;
					}
					ServerInfo info = this.serverManager.getServer(ip, port);
					if(info == null){
						Message.alert(sender, "해당 서버는 아직 추가되어 있지 않습니다. /서버 추가 [ip] [port] 명령어로 서버를 추가한 후 이동을 시도해주세요.");
						return true;
					}
					info.transfer((Player) sender);
					return true;
					
				case "추가":
					try{
						ip = args[1];
						port = Short.parseShort(args[2]);
					}catch(Exception e){
						Message.usage(sender, "/서버 추가 [ip] [port]");
						return true;
					}
					this.serverManager.addServer(ip, port);
					Message.normal(sender, "서버를 추가하였습니다. ( " + args[1] + ":" + args[2] + " )");
					return true;
					
				case "목록":
					try{
						page = Integer.parseInt(args[1]);
					}catch(Exception e){
						
					}
					information = new ArrayList<String>();
					this.serverManager.getServers().forEach((i) -> information.add(i.getMotd() + "  (" + i.getIp() + ":" + Short.toString(i.getPort()) + ") " + ((i.isOnline()) ? "§a(온라인)" : "§7(오프라인)") + "§r"));
					Message.page(sender, "추가된 서버 목록", information, page);
					return true;
					
				case "삭제":
					try{
						ip = args[1];
						port = Short.parseShort(args[2]);
					}catch(Exception e){
						Message.usage(sender, "/서버 삭제 [ip] [port]");
						return true;
					}
					if(this.serverManager.removeServer(ip, port) != null){
						Message.normal(sender, "성공적으로 삭제하였습니다.");
						return true;
					}
					Message.alert(sender, "해당 서버는 존재하지 않습니다.");
					return true;
					
				default:
					try{
						page = Integer.parseInt(args[1]);
					}catch(Exception e){
						
					}
					information = new ArrayList<String>();
					information.add("§2§l/서버 추가 [ip] [port] §r§7- 서버를 추가합니다.");
					information.add("§2§l/서버 목록 §r§7- 서버 목록을 봅니다.");
					information.add("§2§l/서버 이동 [ip] [port] §r§7- 해당 서버로 이동합니다.");
					information.add("§2§l/서버 삭제 [ip] [port] §r§7- 서버를 삭제합니다.");
					Message.page(sender, "서버 명령어 목록", information, page);
			}
		}
		return true;
	}
}
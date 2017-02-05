package solo.transferpro;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;

public class ServerManager{

	public static boolean useLoadBalancer;
	public static int checkQueryInterval;
	public static boolean calculatePlayersCountHollServer;
	public static String serverNameTagFormat;
	public static String offlineServerNameTagFormat;
	
	public Config config;
	
	public HashSet<ServerInfo> servers = new HashSet<ServerInfo>();
	public HashMap<String, TransferButton> buttons = new HashMap<String, TransferButton>();
	
	public ServerManager(){
		this.init();
	}
	
	@SuppressWarnings({ "deprecation", "serial", "unchecked" })
	public void init(){
		this.config = new Config(new File(Main.getInstance().getDataFolder(), "setting.yml"), Config.YAML, new LinkedHashMap<String, Object>(){{
			put("useLoadBalancer", false);
			put("checkQueryInterval", 80);
			put("calculatePlayersCountHollServer", false);
			put("serverNameTagFormat", "§b§l터치시 서버이동§r\n{MOTD}§r\n§o{ONLINEPLAYERS}/{MAXPLAYERS}");
			put("offlineServerNameTagFormat", "§7§l현재 오프라인입니다§r\n§oIP : {IP}§r\n§oPORT : {PORT}");
			put("servers", new ArrayList<String>(){{
				add("example.kro.kr:19132");
			}});
			put("buttons", new LinkedHashMap<String, Object>(){{
				
			}});
		}});
		
		useLoadBalancer = config.getBoolean("useLoadBalancer");
		checkQueryInterval = config.getInt("checkQueryInterval");
		calculatePlayersCountHollServer = config.getBoolean("calculatePlayersCountHollServer");
		serverNameTagFormat = config.getString("serverNameTagFormat");
		offlineServerNameTagFormat = config.getString("offlineServerNameTagFormat");
		
		ArrayList<String> list = (ArrayList<String>) config.get("servers");
		list.forEach((address) -> {
			try{
				String ip = address.split(":")[0];
				short port = Short.parseShort(address.split(":")[1]);
				this.addServer(ip, port);
			}catch(Exception e){
				e.printStackTrace();
			}
		});
		
		Server.getInstance().getLevels().values().forEach((l) -> this.loadFromLevel(l));
	}
	
	public void save(){
		ArrayList<String> list = new ArrayList<String>();
		this.servers.forEach((s) -> {
			if(! s.isClosed()){
				list.add(s.getIp() + ":" + Short.toString(s.getPort()));
			}
		});
		this.config.set("servers", list);
		
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		this.buttons.forEach((k, v) -> {
			if(! v.isClosed()){
				data.put(k, v.serverInfo.getIp() + ":" + Short.toString(v.serverInfo.getPort()));
			}
		});
		this.config.set("buttons", data);
		
		this.config.save();
	}
	
	public Collection<ServerInfo> getServers(){
		return this.servers;
	}
	
	public ServerInfo getServer(String ip, short port){
		for(ServerInfo s : this.servers){
			if(s.getIp().equals(ip) && s.getPort() == port){
				return s;
			}
		}
		return null;
	}
	
	public boolean addServer(String ip, short port){
		if(this.isExistServer(ip, port)){
			return false;
		}
		this.servers.add(new ServerInfo(ip, port));
		return true;
	}
	
	public boolean isExistServer(String ip, short port){
		for(ServerInfo s : this.servers){
			if(s.getIp().equals(ip) && s.getPort() == port){
				return true;
			}
		}
		return false;
	}
	
	public ServerInfo removeServer(String ip, short port){
		ServerInfo target = null;
		for(ServerInfo s : this.servers){
			if(s.getIp().equals(ip) && s.getPort() == port){
				target = s;
				break;
			}
		}
		if(target != null){
			target.close();
			this.servers.remove(target);
		}
		return target;
	}
	
	public ServerInfo selectServer(){
		int remainSlot = Server.getInstance().getMaxPlayers() - Server.getInstance().getOnlinePlayers().size();
		ServerInfo ret = null;
		for(ServerInfo s : this.servers){
			int check = s.getMaxPlayersCount() - s.getOnlinePlayersCount();
			if(check > remainSlot){
				remainSlot = check;
				ret = s;
			}
		}
		return ret;
	}
	
	public int getAllMaxPlayersCount(){
		int count = Server.getInstance().getMaxPlayers();
		for(ServerInfo s : this.servers){
			if(s.isOnline()){
				count += s.getMaxPlayersCount();
			}
		}
		return count;
	}
	
	public int getAllOnlinePlayersCount(){
		int count = Server.getInstance().getOnlinePlayers().size();
		for(ServerInfo s : this.servers){
			if(s.isOnline()){
				count += s.getOnlinePlayersCount();
			}
		}
		return count;
	}
	
	
	
	
	
	
	//TRANSFER BUTTONS
	
	@SuppressWarnings("unchecked")
	public void loadFromLevel(Level level){
		LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
		((LinkedHashMap<String, Object>) this.config.get("buttons")).forEach((k, v) -> data.put(k, (String) v));
		data.forEach((k, v) -> {
			String levelName = k.split(":")[0];
			if(levelName.equals(level.getFolderName())){
				try{
					Position pos = new Position(
							Integer.parseInt(k.split(":")[1]),
							Integer.parseInt(k.split(":")[2]),
							Integer.parseInt(k.split(":")[3]),
							level
						);
					String ip = v.split(":")[0];
					Short port = Short.parseShort(v.split(":")[1]);
					ServerInfo info = this.getServer(ip, port);
					if(info != null){
						System.out.println("pass");
						this.addButton(pos, info);
					}
				}catch(Exception e){
					
				}
			}
		});
	}
	
	public String getHash(Position pos){
		return pos.getLevel().getFolderName() + ":" + Integer.toString(pos.getFloorX()) + ":" + Integer.toString(pos.getFloorY()) + ":" + Integer.toString(pos.getFloorZ());
	}
	
	public TransferButton getButton(Position pos){
		String hash = this.getHash(pos);
		if(this.buttons.containsKey(hash)){
			return this.buttons.get(hash);
		}
		return null;
	}
	
	public boolean addButton(Position pos, ServerInfo info){
		String hash = this.getHash(pos);
		if(this.getButton(pos) == null){
			this.buttons.put(hash, new TransferButton(pos, info));
			return true;
		}
		return false;
	}
	
	public TransferButton removeButton(Position pos){
		String hash = this.getHash(pos);
		if(this.buttons.containsKey(hash)){
			return this.buttons.remove(hash);
		}
		return null;
	}
}
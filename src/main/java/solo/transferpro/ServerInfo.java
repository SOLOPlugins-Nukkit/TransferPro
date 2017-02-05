package solo.transferpro;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.scheduler.Task;
import cn.nukkit.scheduler.AsyncTask;
import solo.transferpro.query.MCQuery;
import solo.transferpro.query.QueryResponse;

public class ServerInfo{
	
	private String ip;
	private short port;

	private boolean online = false;
	private String motd = "Offline";
	//private String mcpeVersion = "null";
	private int maxPlayersCount = 0;
	public int onlinePlayersCount = 0;
	//private ArrayList<String> players = new ArrayList<String>();
	//private String plugins = "";
	
	private int failed = 0;
	private boolean closed = false;
	
	public ServerInfo(String ip, short port){
		this.ip = ip;
		this.port = port;
		
		Server.getInstance().getScheduler().scheduleAsyncTask(new QueryTask());
	}
	
	public String getIp(){
		return this.ip;
	}
	
	public short getPort(){
		return this.port;
	}
	
	public boolean isOnline(){
		return ! this.closed && this.online;
	}
	
	public String getMotd(){
		return this.motd;
	}
	
	//public String getVersion(){
	//	return this.mcpeVersion;
	//}
	
	public int getMaxPlayersCount(){
		return this.maxPlayersCount;
	}
	
	public int getOnlinePlayersCount(){
		return this.onlinePlayersCount;
	}
	
	//public ArrayList<String> getPlayers(){
	//	return this.players;
	//}
	
	//public String getPlugins(){
	//	return this.plugins;
	//}
	
	public void transfer(Player player){
		DataPacket pk = new DataPacket(){
			public static final byte NETWORK_ID = 0x52; // TransferPacket
				 
			public String address = ServerInfo.this.ip; // Server address
			public short port = ServerInfo.this.port; // Server port
				 
			@Override
			public void decode(){
				this.address = this.getString();
				this.port = (short) this.getLShort();
			}
			 
			@Override
			public void encode(){
				this.reset();
				this.putString(address);
				this.putLShort(port);
			}
			 
			@Override
			public byte pid(){
				return NETWORK_ID;
			}
		};
		player.dataPacket(pk);
	}
	
	public void close(){
		this.closed = true;
	}
	
	public boolean isClosed(){
		return this.closed;
	}

	public class QueryTask extends AsyncTask{
		@Override
		public void onRun(){
			try{
				MCQuery mcQuery = new MCQuery(ServerInfo.this.ip, ServerInfo.this.port);
				QueryResponse response = mcQuery.basicStat();
				this.setResult(response);
			}catch(Exception e){
			}
		}
		
		@Override
		public void onCompletion(Server server){
			try{
				QueryResponse response = (QueryResponse) this.getResult();
				
				ServerInfo.this.motd = response.getMOTD();
				ServerInfo.this.maxPlayersCount = response.getMaxPlayers();
				ServerInfo.this.onlinePlayersCount = response.getOnlinePlayers();
				//try{
				//	ServerInfo.this.mcpeVersion = response.getVersion();
				//	ServerInfo.this.players = response.getPlayerList();
				//	ServerInfo.this.plugins = response.getPlugins();
				//}catch(Exception e){
				//	
				//}
				
				ServerInfo.this.failed = 0;
				ServerInfo.this.online = true;
			}catch(Exception e){
				if(ServerInfo.this.failed > 2){
					ServerInfo.this.online = false;
				}else{
					ServerInfo.this.failed++;
				}
			}
			if(! ServerInfo.this.isClosed()){
				server.getScheduler().scheduleDelayedTask(new Task(){
					@Override
					public void onRun(int currentTick){
						server.getScheduler().scheduleAsyncTask(new QueryTask());
					}
				}, ServerManager.checkQueryInterval);
			}
		}
	}
}
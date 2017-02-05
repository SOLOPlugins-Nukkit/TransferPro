package solo.transferpro;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.MoveEntityPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.network.protocol.SetEntityDataPacket;
import cn.nukkit.scheduler.Task;
import solo.solobasepackage.util.Message;

public class TransferButton extends Position{
	
	public ServerInfo serverInfo;
	
	public AddEntityPacket addPk;
	public MoveEntityPacket movePk;
	public RemoveEntityPacket removePk;
	public SetEntityDataPacket setPk;
	
	public boolean closed = false;
	
	public String lastUpdate;
	
	public TransferButton(Position pos, ServerInfo info){
		super(pos.x, pos.y, pos.z, pos.level);
		this.serverInfo = info;
		
		this.init();
		this.spawnToAll();
		
		Server.getInstance().getScheduler().scheduleDelayedTask(new UpdateTask(), 20);
	}
	
	public String getTag(){
		String tag = this.serverInfo.isOnline() ? ServerManager.serverNameTagFormat : ServerManager.offlineServerNameTagFormat;
		tag = tag.replace("{MOTD}", this.serverInfo.getMotd());
		//tag = tag.replace("{VERSION}", this.serverInfo.getVersion());
		tag = tag.replace("{MAXPLAYERS}", Integer.toString(this.serverInfo.getMaxPlayersCount()));
		tag = tag.replace("{ONLINEPLAYERS}",Integer.toString(this.serverInfo.getOnlinePlayersCount()));
		tag = tag.replace("{IP}", this.serverInfo.getIp());
		tag = tag.replace("{PORT}", Short.toString(this.serverInfo.getPort()));
		return tag;
	}
	
	public void init(){
		long eid = Entity.entityCount++;

		this.addPk = new AddEntityPacket();
		this.addPk.entityUniqueId = eid;
		this.addPk.entityRuntimeId = eid;
		this.addPk.type = 15;
		this.addPk.x = (float) (this.getFloorX() + 0.5);
		this.addPk.y = (float) (this.getFloorY() + 0.2);
		this.addPk.z = (float) (this.getFloorZ() + 0.5);
		this.addPk.speedX = 0;
		this.addPk.speedY = 0;
		this.addPk.speedZ = 0;
		this.addPk.yaw = 0;
		this.addPk.pitch = 0;
		
		String tag = this.getTag();
		this.lastUpdate = tag;
		
		long flags = 0;
		flags |= 1 << Entity.DATA_FLAG_INVISIBLE;
		flags |= 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG;
		flags |= 1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG;
		flags |= 1 << Entity.DATA_FLAG_NO_AI;
		EntityMetadata metadata = new EntityMetadata()
				.putLong(Entity.DATA_FLAGS, flags)
				.putShort(Entity.DATA_AIR, 400)
				.putShort(Entity.DATA_MAX_AIR, 400)
				.putString(Entity.DATA_NAMETAG, tag)
				.putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
				.putFloat(Entity.DATA_SCALE, 0.0001f);
		
		this.addPk.metadata = metadata;
		
		this.movePk = new MoveEntityPacket();
		this.movePk.eid = eid;
		this.movePk.x = (float) (this.getFloorX() + 0.5);
		this.movePk.y = (float) (this.getFloorY() + 0.2);
		this.movePk.z = (float) (this.getFloorZ() + 0.5);
		
		this.removePk = new RemoveEntityPacket();
		this.removePk.eid = eid;
		
		this.setPk = new SetEntityDataPacket();
		this.setPk.eid = eid;
	}
	
	public ServerInfo getServerInfo(){
		return this.serverInfo;
	}
	
	public void onTouch(Player player){
		Message.normal(player, "곧 다른 서버로 이동됩니다...");
		Server.getInstance().getScheduler().scheduleDelayedTask(new Task(){
			@Override
			public void onRun(int currentTick) {
				TransferButton.this.serverInfo.transfer(player);
			}
		}, 30);
	}
	
	public void despawnFrom(Player player){
		player.dataPacket(this.removePk);
	}
	
	public void despawnFromAll(){
		Server.getInstance().getOnlinePlayers().values().forEach((p) -> this.despawnFrom(p));
	}
	
	public void spawnTo(Player player){
		this.spawnTo(player, player.getLevel());
	}
	
	public void spawnTo(Player player, Level level){
		if(level == null || ! this.level.getFolderName().equals(level.getFolderName())){
			this.despawnFrom(player);
			return;
		}
		player.dataPacket(this.addPk);
		player.dataPacket(this.movePk);
	}
	
	public void spawnToAll(){
		Server.getInstance().getOnlinePlayers().values().forEach((p) -> this.spawnTo(p));
	}
	
	public void close(){
		this.closed = true;
	}
	
	public boolean isClosed(){
		return this.closed;
	}
	
	public class UpdateTask extends Task{
		@Override
		public void onRun(int currentTick){
			if(TransferButton.this.serverInfo.isClosed() || TransferButton.this.isClosed()){
				TransferButton.this.despawnFromAll();
				TransferButton.this.close();
			}else{
				String tag = TransferButton.this.getTag();
				if(! TransferButton.this.lastUpdate.equals(tag)){
					TransferButton.this.lastUpdate = tag;
					
					long flags = 0;
					flags |= 1 << Entity.DATA_FLAG_INVISIBLE;
					flags |= 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG;
					flags |= 1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG;
					flags |= 1 << Entity.DATA_FLAG_NO_AI;
					EntityMetadata metadata = new EntityMetadata()
							.putLong(Entity.DATA_FLAGS, flags)
							.putShort(Entity.DATA_AIR, 400)
							.putShort(Entity.DATA_MAX_AIR, 400)
							.putString(Entity.DATA_NAMETAG, tag)
							.putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
							.putFloat(Entity.DATA_SCALE, 0.0001f);
					
					TransferButton.this.setPk.metadata = metadata;
					TransferButton.this.level.getPlayers().values().forEach((p) -> p.dataPacket(TransferButton.this.setPk));
				}

				Server.getInstance().getScheduler().scheduleDelayedTask(new UpdateTask(), 60);
			}
		}
	}
	
}
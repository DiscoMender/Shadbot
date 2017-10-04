package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File DATA_FILE = new File("data.json");
	private static final ConcurrentHashMap<String, JSONObject> GUILDS_MAP = new ConcurrentHashMap<>();

	private static final String SETTINGS = "settings";
	private static final String USERS = "users";

	static {
		if(!DATA_FILE.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(DATA_FILE);
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occured during data file initialization. Exiting.", err);
				System.exit(1);

			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));
			for(Object guildID : mainObj.keySet()) {
				GUILDS_MAP.put(guildID.toString(), mainObj.getJSONObject(guildID.toString()));
			}

		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("Error while reading data file. Exiting.", err);
			System.exit(1);
		}
	}

	private static JSONObject getGuild(IGuild guild) {
		return GUILDS_MAP.getOrDefault(guild.getStringID(), Storage.getDefaultGuildObject());
	}

	private static JSONObject getDefaultGuildObject() {
		JSONObject guildObj = new JSONObject();
		guildObj.put(SETTINGS, new JSONObject()
				.put(Setting.ALLOWED_CHANNELS.toString(), new JSONArray())
				.put(Setting.PREFIX.toString(), Config.DEFAULT_PREFIX)
				.put(Setting.DEFAULT_VOLUME.toString(), Config.DEFAULT_VOLUME));
		guildObj.put(USERS, new JSONArray());
		return guildObj;
	}

	public static Object getSetting(IGuild guild, Setting setting) {
		return Storage.getGuild(guild).getJSONObject(SETTINGS).opt(setting.toString());
	}

	public static Player getPlayer(IGuild guild, IUser user) {
		return new Player(guild, user, Storage.getUserObject(guild, user));
	}

	private static JSONObject getUserObject(IGuild guild, IUser user) {
		JSONArray array = Storage.getGuild(guild).getJSONArray(USERS);
		for(int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			if(obj.getLong("userID") == user.getLongID()) {
				return obj;
			}
		}
		return null;
	}

	public static void saveSetting(IGuild guild, Setting setting, Object value) {
		JSONObject settingsObj = Storage.getGuild(guild).getJSONObject(SETTINGS).put(setting.toString(), value);
		GUILDS_MAP.put(guild.getStringID(), Storage.getGuild(guild).put(SETTINGS, settingsObj));
	}

	public static void savePlayer(Player player) {
		JSONArray usersArray = Storage.getGuild(player.getGuild()).getJSONArray(USERS).put(player.toJSON());
		GUILDS_MAP.put(player.getGuild().getStringID(), Storage.getGuild(player.getGuild()).put(USERS, usersArray));
	}

	public static void removeSetting(IGuild guild, Setting setting) {
		Storage.getGuild(guild).getJSONObject(SETTINGS).remove(setting.toString());
	}

	public static void save() {
		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject();
			for(String guildId : GUILDS_MAP.keySet()) {
				mainObj.put(guildId, GUILDS_MAP.get(guildId));
			}

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving data !", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}
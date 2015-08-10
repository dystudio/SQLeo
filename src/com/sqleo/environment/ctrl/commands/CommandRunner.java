package com.sqleo.environment.ctrl.commands;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.sqleo.environment.Application;

public class CommandRunner {

	private TreeMap<String, Command> commandMap = new TreeMap<String, Command>();

	public CommandRunner() {
		registerCommand(ConnectCommand.NAME, new ConnectCommand());
		registerCommand(OutputCommand.NAME, new OutputCommand());
		registerCommand(FormatCommand.NAME, new FormatCommand());
	}

	public void registerCommand(final String cmd, final Command cmnd) {
		if (commandMap.containsKey(cmd)) {
			Application.println("Command:" + cmd + " already used, please use different name for your command");
			return;
		}
		commandMap.put(cmd, cmnd);
	}

	public Command getCommand(final String text) {
		Command found = null;
		for (Entry<String, Command> entry : commandMap.entrySet()) {
			if (text.startsWith(entry.getKey())) {
				found = entry.getValue();
			}
		}
		return found;
	}
}
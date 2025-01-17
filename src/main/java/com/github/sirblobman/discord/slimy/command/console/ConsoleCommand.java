package com.github.sirblobman.discord.slimy.command.console;

import com.github.sirblobman.discord.slimy.DiscordBot;
import com.github.sirblobman.discord.slimy.command.AbstractCommand;
import com.github.sirblobman.discord.slimy.manager.ConsoleCommandManager;

import org.apache.logging.log4j.Logger;

public abstract class ConsoleCommand extends AbstractCommand {
    public ConsoleCommand(DiscordBot discordBot) {
        super(discordBot);
    }

    public final void onCommand(String label, String[] args) {
        try {
            execute(args);
        } catch (Exception ex) {
            Logger logger = getLogger();
            logger.error("Failed to execute console command '" + label + "':", ex);
        }
    }

    protected final ConsoleCommandManager getConsoleCommandManager() {
        DiscordBot discordBot = getDiscordBot();
        return discordBot.getConsoleCommandManager();
    }

    protected abstract void execute(String[] args);
}

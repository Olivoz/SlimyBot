package com.github.sirblobman.discord.slimy.command.slash;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.github.sirblobman.discord.slimy.DiscordBot;
import com.github.sirblobman.discord.slimy.object.MainConfiguration;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;

public final class SlashCommandDevInfo extends SlashCommand {
    private final SystemInfo systemInfo;

    public SlashCommandDevInfo(DiscordBot discordBot) {
        super(discordBot, "devinfo");
        this.systemInfo = new SystemInfo();
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    @Override
    public CommandData getCommandData() {
        String commandName = getCommandName();
        return new CommandData(commandName, "View information for developers and bot owners.")
                .addOption(OptionType.STRING, "type", "What type of information do you need?");
    }

    @Override
    public Message execute(SlashCommandEvent e) {
        Member sender = e.getMember();
        if(sender == null) {
            EmbedBuilder errorEmbed = getErrorEmbed(null);
            errorEmbed.addField("Error", "This command can only be executed in a guild.", false);
            return getMessage(errorEmbed);
        }

        String senderId = sender.getId();
        DiscordBot discordBot = getDiscordBot();
        MainConfiguration mainConfiguration = discordBot.getMainConfiguration();
        String botOwnerId = mainConfiguration.getBotOwnerId();

        if(!senderId.equals(botOwnerId)) {
            EmbedBuilder errorEmbed = getErrorEmbed(null);
            errorEmbed.addField("Error", "This command can only be executed by the bot owner.", false);
            return getMessage(errorEmbed);
        }

        OptionMapping typeOptionMapping = e.getOption("type");
        if(typeOptionMapping == null) {
            EmbedBuilder errorEmbed = getErrorEmbed(sender);
            errorEmbed.addField("Error", "Missing Argument 'type'.", false);
            return getMessage(errorEmbed);
        }

        String typeOption = typeOptionMapping.getAsString().toLowerCase(Locale.US);
        if(typeOption.equals("all")) {
            MessageEmbed embed1 = getOperatingSystemEmbed(sender).build();
            MessageEmbed embed2 = getBotUserInformationEmbed(sender).build();
            MessageEmbed embed3 = getJavaInformationEmbed(sender).build();
            MessageEmbed embed4 = getUptimeEmbed(sender).build();
            MessageEmbed embed5 = getResourceUsageEmbed(sender).build();
            MessageEmbed embed6 = getTemperatureEmbed(sender).build();
            return new MessageBuilder().setEmbeds(embed1, embed2, embed3, embed4, embed5, embed6).build();
        }

        EmbedBuilder embedBuilder = switch(typeOption) {
            case "os" -> getOperatingSystemEmbed(sender);
            case "bot" -> getBotUserInformationEmbed(sender);
            case "java" -> getJavaInformationEmbed(sender);
            case "uptime" -> getUptimeEmbed(sender);
            case "resources" -> getResourceUsageEmbed(sender);
            case "temperature" -> getTemperatureEmbed(sender);
            default -> null;
        };

        if(embedBuilder == null) {
            EmbedBuilder errorEmbed = getErrorEmbed(sender);
            errorEmbed.addField("Error", "Unknown information type '" + typeOption + "'.", false);
            return getMessage(errorEmbed);
        }

        return getMessage(embedBuilder);
    }

    private EmbedBuilder getTemperatureEmbed(Member sender) {
        EmbedBuilder builder = getExecutedByEmbed(sender);
        builder.setTitle("Sensors");

        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat decimalFormat = new DecimalFormat("0.00", decimalFormatSymbols);
        DecimalFormat numberFormat = new DecimalFormat("#,##0", decimalFormatSymbols);

        HardwareAbstractionLayer hardware = this.systemInfo.getHardware();
        Sensors sensors = hardware.getSensors();
        double cpuTemperature = sensors.getCpuTemperature();
        double cpuVoltage = sensors.getCpuVoltage();
        builder.addField("CPU Temperature", decimalFormat.format(cpuTemperature) + "\u00B0C", false);
        builder.addField("CPU Voltage", decimalFormat.format(cpuVoltage) + "V", false);

        int[] fanSpeeds = sensors.getFanSpeeds();
        if(fanSpeeds.length < 1) builder.addField("Fan Speed", "N/A", false);
        else {
            int number = 1;
            for(int fanSpeed : fanSpeeds) {
                builder.addField("Fan Speed " + number, numberFormat.format(fanSpeed) + "rpm", false);
                number++;
            }
        }

        return builder;
    }

    private EmbedBuilder getOperatingSystemEmbed(Member sender) {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");

        String osNameLowercase = osName.toLowerCase();
        String osImageName = getOperatingSystemImageName(osNameLowercase);
        String osImageURL = ("http://resources.sirblobman.xyz/slimy_bot/images/" + osImageName);

        EmbedBuilder builder = getExecutedByEmbed(sender);
        builder.setTitle("Operating System");
        builder.setThumbnail(osImageURL);
        builder.addField("Name", osName, true);
        builder.addField("Version", osVersion, true);
        builder.addField("Arch", osArch, true);

        return builder;
    }

    private EmbedBuilder getJavaInformationEmbed(Member sender) {
        String javaVendor = System.getProperty("java.vendor");
        String javaURL = System.getProperty("java.vendor.url");
        String javaVersion = System.getProperty("java.version");
        String javaImageURL = ("http://resources.sirblobman.xyz/slimy_bot/images/java.png");

        EmbedBuilder builder = getExecutedByEmbed(sender);
        builder.setTitle("Java Information");
        builder.setThumbnail(javaImageURL);
        builder.addField("Vendor", javaVendor, true);
        builder.addField("Version", javaVersion, true);
        builder.addField("URL", javaURL, true);

        return builder;
    }

    private EmbedBuilder getResourceUsageEmbed(Member sender) {
        Runtime runtime = Runtime.getRuntime();
        String cpuCoreCount = Integer.toString(runtime.availableProcessors());
        String cpuImageURL = ("http://resources.sirblobman.xyz/slimy_bot/images/cpu.png");

        long maxMemory = runtime.maxMemory();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();

        long usedMemoryBytes = (totalMemory - freeMemory);
        long freeMemoryBytes = (maxMemory - usedMemoryBytes);

        String usedMemoryMebibytes = toMebibytes(usedMemoryBytes);
        String freeMemoryMebibytes = toMebibytes(freeMemoryBytes);
        String maxMemoryMebibytes = toMebibytes(maxMemory);

        EmbedBuilder builder = getExecutedByEmbed(sender);
        builder.setTitle("Resource Information");
        builder.setThumbnail(cpuImageURL);
        builder.addField("CPU Cores", cpuCoreCount, true);
        builder.addField("Free RAM", freeMemoryMebibytes, true);
        builder.addField("Used RAM", usedMemoryMebibytes, true);
        builder.addField("Max RAM", maxMemoryMebibytes, true);

        return builder;
    }

    private EmbedBuilder getBotUserInformationEmbed(Member sender) {
        DiscordBot discordBot = getDiscordBot();
        JDA discordAPI = discordBot.getDiscordAPI();
        SelfUser selfUser = discordAPI.getSelfUser();

        String avatarURL = selfUser.getEffectiveAvatarUrl();
        String name = selfUser.getName();
        String id = selfUser.getId();
        String tag = selfUser.getAsTag();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm:ss.SSSa",
                Locale.US);

        OffsetDateTime timeCreated = selfUser.getTimeCreated();
        String dateCreatedString = timeCreated.format(dateTimeFormatter) + " UTC";

        OffsetDateTime timeJoined = sender.getTimeJoined();
        String dateJoinedString = timeJoined.format(dateTimeFormatter) + " UTC";

        EmbedBuilder builder = getExecutedByEmbed(sender);
        builder.setTitle("Bot Information");
        builder.setThumbnail(avatarURL);
        builder.addField("Name", name, true);
        builder.addField("Tag", tag, true);
        builder.addField("ID", id, true);
        builder.addField("Date Created", dateCreatedString, false);
        builder.addField("Date Joined", dateJoinedString, false);

        return builder;
    }

    private EmbedBuilder getUptimeEmbed(Member sender) {
        EmbedBuilder builder = getExecutedByEmbed(sender);
        builder.setTitle("Uptime");

        String systemUptimeString = getSystemUptime();
        builder.addField("System Uptime", systemUptimeString, false);

        DiscordBot discordBot = getDiscordBot();
        long startupTimestamp = discordBot.getStartupTimestamp();
        long currentTimestamp = System.currentTimeMillis();
        long uptime = (currentTimestamp - startupTimestamp);
        String uptimeString = formatTime(uptime);
        builder.addField("Bot Uptime", uptimeString, false);

        return builder;
    }

    private String getSystemUptime() {
        String property = System.getProperty("os.name");
        if(!property.contains("nux")) return "N/A";

        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("uptime -p");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return bufferedReader.readLine();
        } catch(Exception ex) {
            return "N/A";
        }
    }

    private String toMebibytes(double bytes) {
        double kibibytes = (bytes / 1_024.0D);
        double mebibytes = (kibibytes / 1_024.0D);

        DecimalFormat format = new DecimalFormat("0.000");
        String mebibytesString = format.format(mebibytes);
        return (mebibytesString + " MiB");
    }

    private String formatTime(long milliseconds) {
        long weeks = (TimeUnit.MILLISECONDS.toDays(milliseconds) / 7L);
        milliseconds -= (TimeUnit.DAYS.toMillis(weeks * 7L));
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        milliseconds -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        milliseconds -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        milliseconds -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder builder = new StringBuilder();
        if(weeks > 0) builder.append(weeks).append("w ");
        if(days > 0) builder.append(days).append("d ");
        if(hours > 0) builder.append(hours).append("h ");
        if(minutes > 0) builder.append(minutes).append("m ");
        if(seconds > 0) builder.append(seconds).append("s ");
        if(milliseconds > 0) builder.append(milliseconds).append("ms");
        return builder.toString().trim();
    }

    private String getOperatingSystemImageName(String osName) {
        if(osName.contains("windows")) return "windows.png";
        if(osName.contains("mac os")) return "apple.png";
        return "linux.png";
    }
}
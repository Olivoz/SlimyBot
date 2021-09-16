package com.github.sirblobman.discord.slimy.command.slash;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.github.sirblobman.discord.slimy.DiscordBot;
import com.github.sirblobman.discord.slimy.object.FAQSolution;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.yaml.snakeyaml.Yaml;

public final class SlashCommandFAQ extends SlashCommand {
    public SlashCommandFAQ(DiscordBot discordBot) {
        super(discordBot, "faq");
    }
    
    @Override
    public CommandData getCommandData() {
        String commandName = getCommandName();
        return new CommandData(commandName, "Get some default answers to common questions.")
                .addOption(OptionType.STRING, "id", "The ID of the question.", true);
    }
    
    @Override
    public Message execute(SlashCommandEvent e) {
        Member sender = e.getMember();
        OptionMapping questionIdOption = e.getOption("id");
        if(questionIdOption == null) {
            EmbedBuilder errorEmbed = getErrorEmbed(sender);
            errorEmbed.addField("Error", "Missing Argument 'id'.", false);
            return getMessage(errorEmbed);
        }
        
        String questionId = questionIdOption.getAsString();
        try {
            FAQSolution solution = getSolution(questionId);
            EmbedBuilder builder = getEmbed(sender, solution, questionId);
            return getMessage(builder);
        } catch(Exception ex) {
            ex.printStackTrace();
            EmbedBuilder errorEmbed = getErrorEmbed(sender);
            errorEmbed.addField("Error", ex.getMessage(), false);
            return getMessage(errorEmbed);
        }
    }
    
    private FAQSolution getSolution(String questionId) {
        File file = new File("questions.yml");
        if(!file.exists()) {
            throw new IllegalStateException("'questions.yml' does not exist!");
        }
        
        try {
            FileInputStream inputStream = new FileInputStream(file);
            Yaml yaml = new Yaml();
            
            Map<String, FAQSolution> solutionMap = yaml.load(inputStream);
            if(solutionMap == null) {
                throw new IllegalStateException("Invalid YAML configuration in 'questions.yml'.");
            }
            
            if(solutionMap.containsKey(questionId)) {
                return solutionMap.get(questionId);
            }
            
            throw new IllegalStateException("Could not find a solution with id '" + questionId + "'.");
        } catch(IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private EmbedBuilder getEmbed(Member sender, FAQSolution solution, String questionId) {
        Objects.requireNonNull(solution, "sender must not be null!");
        Objects.requireNonNull(solution, "solution must not be null!");
    
        EmbedBuilder builder = getExecutedByEmbed(sender);
        builder.setColor(Color.GREEN);
        builder.setTitle("FAQ");
        builder.setDescription("Question ID: " + questionId);
        
        String pluginName = solution.getPluginName();
        if(pluginName != null) {
            builder.addField("Plugin", pluginName, false);
        }
        
        String question = solution.getQuestion();
        builder.addField("Question", question, false);
        
        String answer = solution.getAnswer();
        builder.addField("Answer", answer, false);
        
        return builder;
    }
}
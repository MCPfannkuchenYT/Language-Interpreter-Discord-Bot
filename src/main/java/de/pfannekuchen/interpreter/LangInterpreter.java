package de.pfannekuchen.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LangInterpreter extends ListenerAdapter {
	
	private static JDA bot;
	
	public static void main(String[] args) throws LoginException, IOException {
		bot = JDABuilder.createLight(Files.readAllLines(new File("token").toPath()).get(0)).build();
		bot.addEventListener(new LangInterpreter());
	}
	
	/**
	 * Listens for Chat Messages and executes their Brainfuck File/Message
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		new Thread(() -> {
			try {
				if (event.getMessage().getContentRaw().toLowerCase().startsWith("!brainfuck")) {
					String brainfuckString = event.getMessage().getContentRaw().replaceFirst("!brainfuck ", "");
					executeBrainfuckEvent(brainfuckString, brainfuckString.startsWith("fast "), event.getTextChannel());
				}
				if (event.getMessage().getContentRaw().toLowerCase().startsWith("!bf")) {
					String brainfuckString = event.getMessage().getContentRaw().replaceFirst("!bf ", "");
					executeBrainfuckEvent(brainfuckString, brainfuckString.startsWith("fast "), event.getTextChannel());
				}
				if (event.getMessage().getAttachments().size() != 0) if (event.getMessage().getAttachments().get(0).getFileExtension().equalsIgnoreCase("b") || event.getMessage().getAttachments().get(0).getFileExtension().equalsIgnoreCase("bf")) {
					System.out.println("Downloading Brainfuck Code...");
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					Utils.copyLarge(event.getMessage().getAttachments().get(0).retrieveInputStream().get(), baos, new byte[4096]);
					executeBrainfuckEvent(baos.toString(), true, event.getTextChannel());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	/**
	 * Executes a given brainfuck code
	 * @param brainfuckString The Code to execute
	 * @param isFast Whether it should use -O3
	 * @param respond Channel to respond in
	 * @throws IOException Throws randomly lol
	 */
	private static void executeBrainfuckEvent(String brainfuckString, boolean isFast, TextChannel respond) throws IOException {
		Message m = respond.sendMessage(new EmbedBuilder().setAuthor("Your Brainfuck Code is being executed.. please wait.").setTitle("Brainfuck Compilation Status").setDescription(":white_check_mark: Read Brainfuck Code \n" + 
				":pencil: Interpret Brainfuck Code as C\n" + 
				":x: Compile C Code" + (isFast ? " (with -O3)" : "") + "\n" + 
				":x: Execute the program").build()).complete();
		/* Interpreting the Brainfuck Code as C Code */
		String cString = "#include <stdio.h>\n#include <stdint.h>\n#include <stdlib.h>\n#undef putchar\n\nint main() {\n    char *p = malloc(30000);\n";
		System.out.println("Interpreting Brainfuck...");
		for (char c : brainfuckString.toCharArray()) {
			switch (c) {
		        case '>': cString += ("++p;"); break;
		        case '<': cString += ("--p;"); break;
		        case '+': cString += ("++*p;"); break;
		        case '-': cString += ("--*p;"); break;
		        case '.': cString += ("putchar(*p);"); break;
		        case '[': cString += ("while(*p){"); break;
		        case ']':cString += ("}"); break;
			}
		}
	    cString += "}\n";
		/* Compiling C Code */
	    m.editMessage(new EmbedBuilder().setAuthor("Your Brainfuck Code is being executed.. please wait.").setTitle("Brainfuck Compilation Status").setDescription(":white_check_mark: Read Brainfuck Code \n" + 
				":white_check_mark: Interpret Brainfuck Code as C\n" + 
				":pencil: Compile C Code" + (isFast ? " (with -O3)" : "") + "\n" + 
				":x: Execute the program").build()).complete();
	    System.out.println("Compiling C Code...");
	    File c_file = File.createTempFile("brainfuck", ".c");
		Files.write(c_file.toPath(), cString.getBytes());
		File out_file = File.createTempFile("brainfuck", "");
		Utils.run("/usr/bin/gcc " + c_file.getAbsolutePath().replaceFirst("C\\:", "/mnt/c").replaceFirst("\\\\", "/") + " -o " + out_file.getAbsolutePath().replaceFirst("C\\:", "/mnt/c").replaceFirst("\\\\", "/") + " " + (isFast ? "-O3" : ""), false, 60, (didFail) -> {
			if (didFail) {
				m.editMessage(new EmbedBuilder().setAuthor("Your Brainfuck Code is being executed.. please wait.").setTitle("Brainfuck Compilation Status").setDescription(":white_check_mark: Read Brainfuck Code \n" + 
						":white_check_mark: Interpret Brainfuck Code as C\n" + 
						":x: Compile C Code" + (isFast ? " (with -O3)" : "") + "\n" + 
						":x: Execute the program [CANCELLED]").build()).complete();
				return false; // return anything
			}
			
			/* Execute C Code */
			m.editMessage(new EmbedBuilder().setAuthor("Your Brainfuck Code is being executed.. please wait.").setTitle("Brainfuck Compilation Status").setDescription(":white_check_mark: Read Brainfuck Code \n" + 
					":white_check_mark: Interpret Brainfuck Code as C\n" + 
					":white_check_mark: Compile C Code" + (isFast ? " (with -O3)" : "") + "\n" + 
					":pencil: Execute the program").build()).complete();
			System.out.println("Running compiled Code");
			String brainfuckOutput = Utils.run("./latestBrainfuck", true, 60L, null);
			System.out.println("Printing Results");
			if (brainfuckOutput == null) {
				m.editMessage(new EmbedBuilder().setAuthor("Your Brainfuck Code is being executed.. please wait.").setTitle("Brainfuck Compilation Status").setDescription(":white_check_mark: Read Brainfuck Code \n" + 
						":white_check_mark: Interpret Brainfuck Code as C\n" + 
						":white_check_mark: Compile C Code" + (isFast ? " (with -O3)" : "") + "\n" + 
						":x: Execute the program").build()).complete();
				respond.sendMessage("Your Code ran exceeded the compile&execute time limit.. too bad. ").queue();
			} else {
				m.editMessage(new EmbedBuilder().setAuthor("Your Brainfuck Code is being executed.. please wait.").setTitle("Brainfuck Compilation Status").setDescription(":white_check_mark: Read Brainfuck Code \n" + 
						":white_check_mark: Interpret Brainfuck Code as C\n" + 
						":white_check_mark: Compile C Code" + (isFast ? " (with -O3)" : "") + "\n" + 
						":white_check_mark: Execute the program").build()).complete();
				if (!isFast) respond.sendMessage("Consider running `!brainfuck fast <code>` for optimized and faster Code.\n").queue();
				printLongMessage(brainfuckOutput, respond);
				respond.sendMessage("Your Code was successfully executed").queue();
			}
			
			return true; // return anything
		});
		System.out.println("Cleaning up..");
		out_file.delete();
		c_file.delete();
	}
	
	/**
	 * Prints a large message in multiple if needed
	 * @param msg Message
	 * @param respond Channel to respond to
	 */
	private static void printLongMessage(String msg, TextChannel respond) {
		if (msg.length() < 2000) {
			respond.sendMessage("```" + msg + "```").queue();
		} else {
			String[] lines = msg.split("\n");
			String temporaryString = "";
			for (int i = 0; i < lines.length; i++) {
				if ((temporaryString + (lines[i] + "\n")).length() < 2000) temporaryString = (temporaryString + (lines[i] + "\n"));
				else {
					respond.sendMessage("```" + temporaryString + "```").queue();
					temporaryString = lines[i] + "\n";
				}
			}
			if (!temporaryString.isEmpty()) respond.sendMessage("```" + temporaryString + "```").queue();
		}
	}
	
}
